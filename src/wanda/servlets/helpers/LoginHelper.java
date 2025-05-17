package wanda.servlets.helpers;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;
import tilda.db.ConnectionPool;
import tilda.db.ListResults;
import tilda.utils.DateTimeUtil;
import tilda.utils.EncryptionUtil;
import tilda.utils.SystemValues;
import tilda.utils.TextUtil;
import tilda.utils.json.JSONUtil;
import wanda.data.TenantUser_Data;
import wanda.data.TenantUser_Factory;
import wanda.data.TenantView_Data;
import wanda.data.TenantView_Factory;
import wanda.data.User_Data;
import wanda.web.LoginSyncService;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SessionUtil;
import wanda.web.config.Eula;
import wanda.web.config.Wanda;
import wanda.web.exceptions.AccessForbiddenException;
import wanda.web.exceptions.ResourceNotAuthorizedException;

public class LoginHelper
  {
    protected static final Logger LOG = LogManager.getLogger(LoginHelper.class.getName());

    public static void onBasicLoginSuccess(RequestUtil req, Connection C, User_Data U)
    throws Exception, ResourceNotAuthorizedException, AccessForbiddenException, IOException
      {
        doUserSyncServices(C, U);
        boolean maskedMode = req.getParamBoolean("dataMasking", false);
        req.setSessionBool(SessionUtil.Attributes.MASKING_MODE.name(), maskedMode);

        // Generate App Data if empty
        if (U.getAppData().size() < 1 && U.generateAppData(C) == false)
          {
            throw new Exception("Failed to generate AppData.");
          }

        U.setLastipaddress(req.getRemoteAddr());
        U.setLastLoginNow();
        U.setLoginCount(U.getLoginCount() + 1);
        U.setFailCount(0);
        U.setFailCycleCount(0);
        U.setNullFailFirst();
        U.setNullLocked();
        U.write(C);
        req.setSessionUser(U);
        req.setSessionInt(SessionUtil.Attributes.FORCE_RELOAD_USER.name(), SessionUtil.FORCE_RELOAD_USER);
      }

    public static void onLoginSuccess(RequestUtil req, ResponseUtil res, Connection C, PrintWriter Out, long TenantUserRefnum, String Email, String Pswd, User_Data U)
    throws Exception, ResourceNotAuthorizedException, AccessForbiddenException, IOException
      {
        if (User_Data.isUserLocked(U))
          {
            throw new ResourceNotAuthorizedException("User", Email);
          }

        if (hasPasswordExpired(U))
          {
            U.sendForgotPswdEmail(C);
            throw new AccessForbiddenException("User", "Your password has expired, please reset your password");
          }

        onBasicLoginSuccess(req, C, U);

        // SuperAdmin Check
        if (U.hasRoles(RoleHelper.SUPERADMIN))
          {
            ClearUserForEula(C, req, U, SystemValues.EVIL_VALUE);
            JSONUtil.startOK(Out, '{');
            JSONUtil.print(Out, "appData", true, U.getAppDataJson(Email + "@@" + Pswd));
            JSONUtil.end(Out, '}');
            return;
          }

        if (ConnectionPool.isMultiTenant() == false)
          {
            Eula E = Wanda.getEula("");
            int days = DateTimeUtil.computeDaysToNow(U.getLastEula());
            if (E != null && (days < 0 || days > E._days))
              {
                doEula(Out, req, C, TenantUserRefnum, E, U);
              }
            else
              {
                req.setSessionInt(SessionUtil.Attributes.EULA_CLEAR.toString(), 1);
                // Generate Response
                JSONUtil.startOK(Out, '{');
                JSONUtil.print(Out, "appData", true, U.getAppDataJson(Email + "@@" + Pswd));
                JSONUtil.end(Out, '}');
              }
          }
        else
          {
            nextLoginStep(C, U, req, res, Out); // Also returns response
          }
      }


    public static void onLoginFailure(Connection C, User_Data U)
    throws Exception
      {
        String ErrorMessage;
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

    /**
     * Checks if the user's password has expired. Mostly used for the login servlet. Wondering if it should be elsewhere.
     * 
     * @param U
     * @return
     */
    protected static boolean hasPasswordExpired(User_Data U)
      {
        return U.getPswdCreate() != null && ChronoUnit.DAYS.between(U.getPswdCreate(), ZonedDateTime.now()) > Wanda.getPasswordExpiry();
      }

    protected static boolean isUserLocked(User_Data U)
      {
        return User_Data.isUserLocked(U);
      }


    public static void ClearUserForEula(Connection C, RequestUtil Req, User_Data U, long tenantUserRefnum)
    throws Exception
      {
        U.setLastEulaNow();
        if (U.write(C) == false)
          throw new Exception("Cannot update user " + U.getRefnum());
        if (tenantUserRefnum != SystemValues.EVIL_VALUE)
          {
            TenantUser_Data TU = TenantUser_Factory.lookupByPrimaryKey(tenantUserRefnum);
            TU.setLastEulaNow();
            if (TU.write(C) == false)
              throw new Exception("Cannot update TenantUser refnum " + tenantUserRefnum);
            Req.setSessionTenantUser(TU.getRefnum());
          }
        Req.removeSessionAttribute(SessionUtil.Attributes.EULA_CODE.toString());
        Req.setSessionInt(SessionUtil.Attributes.EULA_CLEAR.toString(), 1);
      }

    public static boolean doEula(PrintWriter Out, RequestUtil Req, Connection C, long TenantUserRefnum, Eula E, User_Data U)
    throws Exception
      {
        // For encrypting AppData._dbKey
        String Email = Req.getParamString("email", true);
        String Pswd = Req.getParamString("pswd", true);
        // For Eula
        String TokenIn = Req.getParamString("eulaToken", false);
        String TokenSaved = Req.getSessionString(SessionUtil.Attributes.EULA_CODE.toString());
        if (TextUtil.isNullOrEmpty(TokenSaved) == false && TokenSaved.equals(TokenIn) == true)
          {
            ClearUserForEula(C, Req, U, TenantUserRefnum);
            return true;
          }

        String TokenNew = EncryptionUtil.getToken(18, true);
        Req.setSessionString(SessionUtil.Attributes.EULA_CODE.toString(), TokenNew);
        JSONUtil.startOK(Out, '{');
        JSONUtil.print(Out, "appData", true, U.getAppDataJson(Email + "@@" + Pswd));
        JSONUtil.print(Out, "tenantUserRefnum", false, TenantUserRefnum);
        JSONUtil.print(Out, "eulaUrl", false, E._eulaUrl);
        JSONUtil.print(Out, "eulaToken", false, TokenNew);
        JSONUtil.end(Out, '}');
        return false;
      }

    private static void nextLoginStep(Connection C, User_Data U, RequestUtil Req, ResponseUtil Res, PrintWriter Out)
    throws Exception
      {
        // For Encrypting AppData._dbKey
        String Email = Req.getParamString("email", true);
        String Pswd = Req.getParamString("pswd", true);

        ListResults<TenantView_Data> list = TenantView_Factory.getAllActiveByUserRefnum(C, U.getRefnum(), 0, 1000);
        if (list.size() == 0)
          {
            Req.removeSessionUser();
            throw new ResourceNotAuthorizedException("User", U.getEmail(), "You do not have access to any Tenants.\nPlease contact your Administrator.");
          }
        else if (list.size() == 1)
          {
            Eula E = Wanda.getEula(list.get(0).getName());
            int days = DateTimeUtil.computeDaysToNow(U.getLastEula());
            if (E != null && (days < 0 || days > E._days))
              {
                doEula(Out, Req, C, list.get(0).getTenantUserRefnum(), E, U);
              }
            else
              {
                Req.setSessionLong(SessionUtil.Attributes.USERREFNUM.toString(), list.get(0).getUserRefnum());
                Req.setSessionLong(SessionUtil.Attributes.TENANTUSERREFNUM.toString(), list.get(0).getTenantUserRefnum());
                ClearUserForEula(C, Req, U, list.get(0).getTenantUserRefnum());
                UserTenantSync.sync(C, U, list.get(0).getTenantUserRefnum());
                JSONUtil.startOK(Out, '{');
                list.get(0).toJSON(Out, "tenantUserJson", false);
                JSONUtil.print(Out, "appData", false, U.getAppDataJson(Email + "@@" + Pswd));
                JSONUtil.end(Out, '}');
              }
          }
        else
          {
            JSONUtil.startOK(Out, '{');
            JSONUtil.print(Out, "appData", true, U.getAppDataJson(Email + "@@" + Pswd));
            JSONUtil.print(Out, "tenants", "tenantUserJson", false, list, " ");
            JSONUtil.print(Out, "message", false, "Please select a tenant");
            JSONUtil.end(Out, '}');
          }
      }
  }
