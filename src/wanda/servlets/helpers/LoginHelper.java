package wanda.servlets.helpers;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;
import tilda.db.ConnectionPool;
import tilda.db.ListResults;
import tilda.utils.DateTimeUtil;
import tilda.utils.SystemValues;
import tilda.utils.TextUtil;
import tilda.utils.json.JSONUtil;
import wanda.data.TenantView_Data;
import wanda.data.TenantView_Factory;
import wanda.data.User_Data;
import wanda.web.LoginSyncService;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SessionUtil;
import wanda.web.config.Wanda;
import wanda.web.exceptions.AccessForbiddenException;
import wanda.web.exceptions.ResourceNotAuthorizedException;

public class LoginHelper
  {
    protected static final Logger LOG = LogManager.getLogger(LoginHelper.class.getName());

    /**
     * Note: clientAppDataKeys was used to sign a client-side database on a mobile device. This is no longer in use.
     * We keep this for reference only.
     * 
     * @param req
     * @param res
     * @param C
     * @param U
     * @throws Exception
     */
    public static void loginSuccess(RequestUtil req, ResponseUtil res, Connection C, User_Data U, User_Data impersonatedU)
    throws Exception
      {
        basicLoginSuccess(req, C, U.isSuperAdmin() == true && impersonatedU != null ? impersonatedU : U);

        // SuperAdmin Check
        if (U.isSuperAdmin() == true)
          {
            LOG.debug("SuperAdmin let through!");
            if (impersonatedU != null)
              {
                LOG.debug("Impersonation in effect: " + U.getEmail()+" is impersonating "+ impersonatedU.getEmail()+".");
                EulaHelper.clearUserForEula(C, req, impersonatedU, null, false);
                PlanHelper.clearUserForPlan(C, req, impersonatedU, false);
                req.setSessionString(SessionUtil.Attributes.USER_IMPERSONATOR_EMAIL.toString(), U.getEmail());
              }
            else
              {
                EulaHelper.clearUserForEula(C, req, U, null, false);
                PlanHelper.clearUserForPlan(C, req, U, false);
              }

            res.success();
            return;
          }

        TenantView_Data TV = null;
        LOG.debug("Checking multi-tenancy");
        if (ConnectionPool.isMultiTenant() == true)
          {
            LOG.debug("Multi-tenancy is enabled");
            // Let's get the list of tenants the user has access to
            ListResults<TenantView_Data> list = TenantView_Factory.getAllActiveByUserRefnum(C, U.getRefnum(), 0, 1000);
            if (list.size() == 0)
              {
                req.removeSessionUser();
                throw new ResourceNotAuthorizedException("User", U.getEmail(), "You do not have access to any Tenant.\nPlease contact your Administrator.");
              }

            // If Tenant Login, need to check access
            long tenantUserRefnum = req.getParamLong("tenantUserRefnum", false);
            if (tenantUserRefnum != SystemValues.EVIL_VALUE)
              { // User Selected a Tenant
                LOG.debug("Tenant login");
                for (TenantView_Data tv : list)
                  if (tv.getTenantUserRefnum() == tenantUserRefnum)
                    {
                      TV = tv;
                      break;
                    }
                if (TV == null) // couldn't find a tenantUserRefnum match
                  {
                    req.removeSessionUser();
                    throw new ResourceNotAuthorizedException("User", U.getEmail(), "You do not have access to this Tenant.\nPlease contact your Administrator.");
                  }
              }
            else if (list.size() > 1) // this is not a tenant login and the user has access to multiple tenants, so we have to force a pick.
              {
                LOG.debug("Tenant selection needed");
                // multiple tenants, we need a tenant selection
                PrintWriter out = res.setContentType(ResponseUtil.ContentType.JSON);
                JSONUtil.startOK(out, '{');
                JSONUtil.print(out, "tenants", "tenantJson", true, list, " ");
                JSONUtil.print(out, "message", false, "Please select a tenant");
                // JSONUtil.print(out, "appData", true, clientAppDataKeys);
                JSONUtil.end(out, '}');
                return;
              }

            LOG.debug("Setting Tenant session");
            req.setSessionTenantUser(TV.getTenantUserRefnum());
            LOG.debug("Tenant user sync");
            UserTenantSync.sync(C, U, tenantUserRefnum);
          }

        LOG.debug("Checking Eula");
        if (EulaHelper.doEula(req, res, C, U, TV) == false)
          return;
        LOG.debug("Checking Plan");
        if (PlanHelper.doPlan(req, res, C, U) == false)
          return;

        if (U.write(C) == false)
          throw new Exception("Cannot update user " + U.getRefnum());

        // all good!
        req.setSessionUser(U);
        PrintWriter out = res.setContentType(ResponseUtil.ContentType.JSON);
        JSONUtil.startOK(out, '{');
        JSONUtil.print(out, "tenantUser", "tenantUserJson", true, TV, " ");
        // JSONUtil.print(out, "appData", false, clientAppDataKeys);
        JSONUtil.end(out, '}');
      }

    public static void basicLoginSuccess(RequestUtil req, Connection C, User_Data U)
    throws Exception, ResourceNotAuthorizedException, AccessForbiddenException, IOException
      {
        LOG.debug("Calling UserSync Services");
        doUserSyncServices(C, U);

        LOG.debug("Setting data masking if appropriate");
        boolean maskedMode = req.getParamBoolean("dataMasking", false);
        req.setSessionBool(SessionUtil.Attributes.MASKING_MODE.name(), maskedMode);

        // Generate App Data if empty
        LOG.debug("Initializing first-time AppData");
        if (U.getAppData().size() < 1 && U.generateAppData(C) == false)
          throw new Exception("Failed to generate AppData.");

        LOG.debug("Updating user info");
        U.setLastipaddress(req.getRemoteAddr());
        U.setLastLoginNow();
        U.setLoginCount(U.getLoginCount() + 1);
        U.setFailCount(0);
        U.setFailCycleCount(0);
        U.setNullFailFirst();
        U.setNullLocked();
        U.setNullPswdResetCode();
        U.setNullPswdResetCreate();

        // If the user has a promo code, we want to update their app mapping.
        LOG.debug("Updating user apps based on promo code if any");
        if (TextUtil.isNullOrEmpty(U.getPromoCode()) == false)
          U.syncUpApps(C, U, U.getPromoCode(), U.getLoginDomain());

        LOG.debug("Setting session user");
        req.setSessionUser(U);
        req.setSessionInt(SessionUtil.Attributes.FORCE_RELOAD_USER.name(), SessionUtil.FORCE_RELOAD_USER);
      }


    public static void loginFailure(Connection C, User_Data U)
    throws Exception
      {
        String ErrorMessage = null;
        // Failure logging handled in session filter.
        if (U == null || U.getDeleted() != null || U.isLocked() == true || U.getInviteCancelled() == true)
          {
            if (U == null)
              LOG.error("Patient not found");
            else
              {
                if (U.getDeleted() != null)
                  LOG.error("Patient is deleted");
                if (U.isLocked() == true)
                  LOG.error("Patient is locked until " + DateTimeUtil.printDateTimeCompact(U.getLocked(), true, true));
                if (U.getInviteCancelled() == true)
                  LOG.error("Patient has been uninvited");
              }
            ErrorMessage = "Invalid Login Id or Password, or this account is locked.";
          }
        else
          {
            User_Data.markUserLoginFailure(C, U);
            int FailCount = Wanda.getLoginAttempts() - U.getFailCount();
            if (U.isLocked() == true)
              {
                if (U.getFailCycleCount() >= Wanda.getLoginFailedCycle())
                  ErrorMessage = "Your account is locked!\nYou have exceeded  the maximum " + Wanda.getLoginAttempts() + " reset or login attempts.\nPlease contact your Administrator.";
                else
                  ErrorMessage = "You have exceeded the maximum " + Wanda.getLoginAttempts() + " reset or login attempts.\nPlease try again in " + Wanda.getLockFor() + " minutes.";
              }
            else
              ErrorMessage = "Invalid Login Id or Password.\nYou have " + FailCount + " attempt(s) remaining";
          }
        throw new ResourceNotAuthorizedException("User", U.getEmail(), ErrorMessage);
      }

    protected static void doUserSyncServices(Connection C, User_Data U)
      {
        if (Wanda.getLoginSystem() != null)
          {
            List<LoginSyncService> L = Wanda.getLoginSystem().getUserSyncServiceClasses();
            for (LoginSyncService lss : L)
              if (lss != null)
                try
                  {
                    lss.syncUser(C, U);
                  }
                catch (Throwable T)
                  {
                    LOG.warn("Cannot follow user sync process off of '" + lss.getClass().getCanonicalName() + "'.");
                  }
          }
      }
  }
