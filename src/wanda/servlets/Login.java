/* ===========================================================================
 * Copyright (C) 2017 CapsicoHealth Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wanda.servlets;

import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.annotation.WebServlet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import wanda.data.TenantUser_Data;
import wanda.data.TenantUser_Factory;
import wanda.data.TenantView_Data;
import wanda.data.TenantView_Factory;
import wanda.data.User_Data;
import wanda.data.User_Factory;
import wanda.servlets.helpers.RoleHelper;
import wanda.servlets.helpers.UserTenantSync;
import wanda.web.config.Eula;
import wanda.web.config.WebBasics;

import tilda.db.Connection;
import tilda.db.ConnectionPool;
import tilda.db.ListResults;
import tilda.utils.DateTimeUtil;
import tilda.utils.EncryptionUtil;
import tilda.utils.SystemValues;
import tilda.utils.TextUtil;
import tilda.utils.json.JSONUtil;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SessionFilter;
import wanda.web.SessionUtil;
import wanda.web.SimpleServlet;
import wanda.web.exceptions.AccessForbiddenException;
import wanda.web.exceptions.ResourceNotAuthorizedException;

/**
 * Servlet implementation class Login
 */
@WebServlet("/svc/Login")
public class Login extends SimpleServlet
  {
    interface LoginCallback
      {
        void onLoginSuccess(User_Data u)
        throws Exception;

        void onLoginFailure(User_Data u)
        throws Exception;
      }

    protected static final Logger LOG              = LogManager.getLogger(Login.class.getName());
    private static final long     serialVersionUID = 7833614578489016882L;

    /**
     * Default constructor.
     */
    public Login()
      {
        super(false);
      }

    @Override
    public void init(ServletConfig Conf)
      {
        SessionFilter.addMaskedUrlNvp("pswd");
      }

    private static void login(Connection C, String username, String password, LoginCallback loginCallback)
    throws Exception
      {
        LOG.debug("Loging in user locally");
        // Authenticate using local Login
        username = username.toLowerCase();
        User_Data u = User_Factory.lookupByEmail(username);
        if (u.read(C) == false || u.getDeleted() != null || u.isLocked() == true || u.getInviteCancelled() == true)
          {
            LOG.error("User '" + username + "' not found in the local DB or not in a state where they can log in.");
            loginCallback.onLoginFailure(u);
          }

        if (EncryptionUtil.hash(password).equals(u.getPswd()) == false)
          {
            LOG.error("Invalid password for User '" + username + "' in the local DB");
            loginCallback.onLoginFailure(u);
          }

        if (WebBasics.validatePassword(password).size() > 0)
          {
            u.sendForgotPswdEmail(C);
            throw new AccessForbiddenException("User", "Password invalid as per Password Rules");
          }

        loginCallback.onLoginSuccess(u);
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        PrintWriter Out = Res.setContentType(ResponseUtil.ContentType.JSON);
        long TenantUserRefnum = Req.getParamLong("tenantUserRefnum", false);
        String eulaToken = Req.getParamString("eulaToken", false);
        int accept = Req.getParamInt("accept", false);

        if (TextUtil.isNullOrEmpty(eulaToken) == false && U != null)
          { // Eula step
            String EulaTokenSaved = Req.getSessionString(SessionUtil.Attributes.EULA_CODE.name());
            if (eulaToken.equals(EulaTokenSaved) == false)
              {
                Req.addError("eulaToken", "is Invalid");
                Req.throwIfErrors();
              }
            if (accept != 1)
              {
                Req.addError("accept", "You must accept the EULA before continuing.");
                Req.throwIfErrors();
              }
            Req.setSessionTenantUser(TenantUserRefnum);
            ClearUserForEula(C, Req, U, TenantUserRefnum);
            if (TenantUserRefnum != SystemValues.EVIL_VALUE)
              UserTenantSync.sync(C, U, TenantUserRefnum);
            Res.success();
          }
        else if (TenantUserRefnum != SystemValues.EVIL_VALUE && U != null)
          { // User Selected a Tenant
            TenantView_Data TV = TenantView_Factory.getTenantByTenantUserRefnum(C, U.getRefnum(), TenantUserRefnum);

            if (TV == null)
              {
                Req.addError("tenantUserRefnum", "is Invalid");
                Req.throwIfErrors();
              }

            Eula E = WebBasics.getEula(TV.getName());
            int days = DateTimeUtil.computeDaysToNow(TV.getTenantUserLastEula());
            if (E != null && TextUtil.isNullOrEmpty(E._eulaUrl) == false && (days < 0 || days > E._days))
              {
                doEula(Out, Req, C, TenantUserRefnum, E, U);
              }
            else
              {
                Req.setSessionTenantUser(TV.getTenantUserRefnum());
                ClearUserForEula(C, Req, U, TenantUserRefnum);
                UserTenantSync.sync(C, U, TenantUserRefnum);
                JSONUtil.response(Out, "tenantUserJson", TV);
              }
          }
        else
          { // User trying to login
            String Email = Req.getParamString("email", true);
            String Pswd = Req.getParamString("pswd", true);
            Req.throwIfErrors();
            // To override the DB rollback
            Req.setSessionInt(SessionUtil.Attributes.FORCE_COMMIT.name(), SessionUtil.FORCE_COMMIT);
            login(C, Email, Pswd, new LoginCallback()
              {
                @Override
                public void onLoginSuccess(User_Data U)
                throws Exception
                  {
                    // Generate App Data if empty
                    if (U.getAppData().size() < 1 && U.generateAppData(C) == false)
                      {
                        throw new Exception("Failed to generate AppData.");
                      }

                    if (User_Data.isUserLocked(U))
                      {
                        throw new ResourceNotAuthorizedException("User", Email);
                      }

                    if (hasPasswordExpired(U))
                      {
                        U.sendForgotPswdEmail(C);
                        throw new AccessForbiddenException("User", "Your password has expired, please reset your password");
                      }
                    U.setLastipaddress(Req.getRemoteAddr());
                    U.setLastLoginNow();
                    U.setLoginCount(U.getLoginCount() + 1);
                    U.setFailCount(0);
                    U.setFailCycleCount(0);
                    U.setNullFailFirst();
                    U.setNullLocked();
                    U.write(C);
                    Req.setSessionUser(U);
                    Req.setSessionInt(SessionUtil.Attributes.FORCE_RELOAD_USER.name(), SessionUtil.FORCE_RELOAD_USER);
                    // SuperAdmin Check
                    if (U.hasRoles(RoleHelper.SUPERADMIN))
                      {
                        ClearUserForEula(C, Req, U, SystemValues.EVIL_VALUE);
                        JSONUtil.startOK(Out, '{');
                        JSONUtil.print(Out, "appData", true, U.getAppDataJson(Email + "@@" + Pswd));
                        JSONUtil.end(Out, '}');
                        return;
                      }

                    if (ConnectionPool.isMultiTenant() == false)
                      {
                        Eula E = WebBasics.getEula("");
                        int days = DateTimeUtil.computeDaysToNow(U.getLastEula());
                        if (E != null && (days < 0 || days > E._days))
                          {
                            doEula(Out, Req, C, TenantUserRefnum, E, U);
                          }
                        else
                          {
                            Req.setSessionInt(SessionUtil.Attributes.EULA_CLEAR.toString(), 1);
                            // Generate Response
                            JSONUtil.startOK(Out, '{');
                            JSONUtil.print(Out, "appData", true, U.getAppDataJson(Email + "@@" + Pswd));
                            JSONUtil.end(Out, '}');
                          }
                      }
                    else
                      {
                        nextLoginStep(C, U, Req, Res, Out); // Also returns response
                      }
                  }

                @Override
                public void onLoginFailure(User_Data U)
                throws Exception
                  {
                    String ErrorMessage;
                    // Failure logging handled in session filter.
                    if (U==null || U.getDeleted() != null || U.isLocked() == true || U.getInviteCancelled() == true)
                      {
                        if (U == null)
                          LOG.error("Patient not found");
                        else
                          {
                            if (U.getDeleted() != null)
                              LOG.error("Patient is deleted");
                            if (U.isLocked() == true)
                              LOG.error("Patient is locked until "+DateTimeUtil.printDateTimeCompact(U.getLocked(), true, true));
                            if (U.getInviteCancelled() == true)
                              LOG.error("Patient has been uninvited");
                          }
                        ErrorMessage = "Invalid Login Id or Password, or this account is locked.";
                      }
                    else
                      {
                        User_Data.markUserLoginFailure(C, U);
                        int FailCount = WebBasics.getLoginAttempts() - U.getFailCount();
                        if (U.isLocked() == true)
                         {
                           if (U.getFailCycleCount() >= WebBasics.getLoginFailedCycle())
                            ErrorMessage = "Your account is locked!\nYou have exceeded  the maximum "+WebBasics.getLoginAttempts()+" reset or login attempts.\nPlease contact your Administrator.";
                           else
                            ErrorMessage = "You have exceeded the maximum "+WebBasics.getLoginAttempts()+" reset or login attempts.\nPlease try again in "+WebBasics.getLockFor()+" minutes.";
                         }
                        else
                         ErrorMessage = "Invalid Login Id or Password.\nYou have " + FailCount + " attempt(s) remaining";
                      }
                    throw new ResourceNotAuthorizedException("User", Email, ErrorMessage);
                  }
              }); // End of login() method

          }
      }

    private static boolean doEula(PrintWriter Out, RequestUtil Req, Connection C, long TenantUserRefnum, Eula E, User_Data U)
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

        String TokenNew = EncryptionUtil.getToken(20, true);
        Req.setSessionString(SessionUtil.Attributes.EULA_CODE.toString(), TokenNew);
        JSONUtil.startOK(Out, '{');
        JSONUtil.print(Out, "appData", true, U.getAppDataJson(Email + "@@" + Pswd));
        JSONUtil.print(Out, "tenantUserRefnum", false, TenantUserRefnum);
        JSONUtil.print(Out, "eulaUrl", false, E._eulaUrl);
        JSONUtil.print(Out, "eulaToken", false, TokenNew);
        JSONUtil.end(Out, '}');
        return false;
      }


    private static void ClearUserForEula(Connection C, RequestUtil Req, User_Data U, long tenantUserRefnum)
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
            Eula E = WebBasics.getEula(list.get(0).getName());
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
