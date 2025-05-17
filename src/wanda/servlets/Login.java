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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import tilda.utils.DateTimeUtil;
import tilda.utils.EncryptionUtil;
import tilda.utils.SystemValues;
import tilda.utils.TextUtil;
import tilda.utils.json.JSONUtil;
import wanda.data.TenantView_Data;
import wanda.data.TenantView_Factory;
import wanda.data.User_Data;
import wanda.data.User_Factory;
import wanda.saml.ConfigSAML;
import wanda.saml.SAMLUserProfile;
import wanda.servlets.helpers.LoginCallbackInterface;
import wanda.servlets.helpers.LoginHelper;
import wanda.servlets.helpers.UserTenantSync;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SessionFilter;
import wanda.web.SessionUtil;
import wanda.web.SimpleServlet;
import wanda.web.config.Eula;
import wanda.web.config.SSOConfig;
import wanda.web.config.Wanda;
import wanda.web.exceptions.AccessForbiddenException;

/**
 * Servlet implementation class Login
 */
@WebServlet("/svc/Login")
public class Login extends SimpleServlet
  {
    protected static final Logger LOG              = LogManager.getLogger(LoginHelper.class.getName());
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

    private static void login(Connection C, String username, String password, LoginCallbackInterface loginCallback)
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

        if (EncryptionUtil.hash(password, u.getPswdSalt()).equals(u.getPswd()) == false)
          {
            LOG.error("Invalid password for User '" + username + "' in the local DB");
            loginCallback.onLoginFailure(u);
          }

        if (Wanda.validatePassword(password).size() > 0)
          {
            u.sendForgotPswdEmail(C);
            throw new AccessForbiddenException("User", "Password invalid as per Password Rules");
          }

        // use the login process to migrate to using the salt if it hasn't been set before.
        if (TextUtil.isNullOrEmpty(u.getPswdSalt()) == true)
          {
            String salt = u.getOrCreatePswdSalt();
            String pswd = EncryptionUtil.hash(password, salt);
            u.setPswdSalt(salt);
            u.setPswd(pswd);
          }

        loginCallback.onLoginSuccess(u);
      }



    @Override
    protected void justDo(RequestUtil req, ResponseUtil res, Connection C, User_Data U)
    throws Exception
      {
        PrintWriter Out = res.setContentType(ResponseUtil.ContentType.JSON);
        long TenantUserRefnum = req.getParamLong("tenantUserRefnum", false);
        String eulaToken = req.getParamString("eulaToken", false);
        int accept = req.getParamInt("accept", false);
        String ssoId = req.getParamString("ssoId", false);

        if (TextUtil.isNullOrEmpty(eulaToken) == false && U != null)
          { // Eula step
            String EulaTokenSaved = req.getSessionString(SessionUtil.Attributes.EULA_CODE.name());
            if (eulaToken.equals(EulaTokenSaved) == false)
              {
                req.addError("eulaToken", "is Invalid");
                req.throwIfErrors();
              }
            if (accept != 1)
              {
                req.addError("accept", "You must accept the EULA before continuing.");
                req.throwIfErrors();
              }
            req.setSessionTenantUser(TenantUserRefnum);
            LoginHelper.ClearUserForEula(C, req, U, TenantUserRefnum);
            if (TenantUserRefnum != SystemValues.EVIL_VALUE)
              UserTenantSync.sync(C, U, TenantUserRefnum);
            res.success();
          }
        else if (TenantUserRefnum != SystemValues.EVIL_VALUE && U != null)
          { // User Selected a Tenant
            TenantView_Data TV = TenantView_Factory.getTenantByTenantUserRefnum(C, U.getRefnum(), TenantUserRefnum);

            if (TV == null)
              {
                req.addError("tenantUserRefnum", "is Invalid");
                req.throwIfErrors();
              }

            Eula E = Wanda.getEula(TV.getName());
            int days = DateTimeUtil.computeDaysToNow(TV.getTenantUserLastEula());
            if (E != null && TextUtil.isNullOrEmpty(E._eulaUrl) == false && (days < 0 || days > E._days))
              {
                LoginHelper.doEula(Out, req, C, TenantUserRefnum, E, U);
              }
            else
              {
                req.setSessionTenantUser(TV.getTenantUserRefnum());
                LoginHelper.ClearUserForEula(C, req, U, TenantUserRefnum);
                UserTenantSync.sync(C, U, TenantUserRefnum);
                JSONUtil.response(Out, "tenantUserJson", TV);
              }
          }
        else if (TextUtil.isNullOrEmpty(ssoId) == false) // SSO
          {
            SAMLUserProfile userProfile = ConfigSAML.processCallback(req.getHttpServletRequest(), res.getHttpServletResponse(), C, ssoId);
            if (userProfile == null)
              return;
            req.setSessionInt(SessionUtil.Attributes.FORCE_COMMIT.name(), SessionUtil.FORCE_COMMIT);
            U = User_Factory.lookupByEmail(userProfile._email);
            if (U.read(C) == false)
             throw new Exception("SSO user '" + userProfile._email + "' not found in the local DB.");
            SSOConfig ssoConfig = Wanda.getSsoConfig(ssoId);
            if (ssoConfig._eula == false)
             req.setSessionInt(SessionUtil.Attributes.EULA_CLEAR.toString(), 1);
            LoginHelper.onBasicLoginSuccess(req, C, U);
            U.syncUpApps(C, U, ssoConfig._defaultPromoCode, ssoId);            
            res.sendRedirect("/web/apps/learning-ai/main.jsp");
          }
        else // User trying to login
          {
            String Email = req.getParamString("email", true);
            String Pswd = req.getParamString("pswd", true);
            req.throwIfErrors();
            // To override the DB rollback
            req.setSessionInt(SessionUtil.Attributes.FORCE_COMMIT.name(), SessionUtil.FORCE_COMMIT);
            login(C, Email, Pswd, new LoginCallbackInterface()
              {
                @Override
                public void onLoginSuccess(User_Data U)
                throws Exception
                  {
                    LoginHelper.onLoginSuccess(req, res, C, Out, TenantUserRefnum, Email, Pswd, U);
                  }

                @Override
                public void onLoginFailure(User_Data U)
                throws Exception
                  {
                    LoginHelper.onLoginFailure(C, U);
                  }

              }); // End of login() method
          }
      }

  }
