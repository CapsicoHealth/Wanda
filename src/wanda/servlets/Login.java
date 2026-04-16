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

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import tilda.utils.EncryptionUtil;
import tilda.utils.TextUtil;
import wanda.data.User_Data;
import wanda.data.User_Factory;
import wanda.saml.ConfigSAML;
import wanda.saml.SAMLUserProfile;
import wanda.servlets.helpers.LoginHelper;
import wanda.servlets.helpers.PlanHelper;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SessionFilter;
import wanda.web.SessionUtil;
import wanda.web.SimpleServlet;
import wanda.web.config.SSOConfig;
import wanda.web.config.Wanda;
import wanda.web.exceptions.AccessForbiddenException;
import wanda.web.exceptions.ResourceNotAuthorizedException;

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
        SessionFilter.addMaskedUrlNvp("SAMLResponse");
      }


    @Override
    protected void justDo(RequestUtil req, ResponseUtil res, Connection C, User_Data U)
    throws Exception
      {
        String ssoId = req.getParamString("ssoId", false);
        // SSO Login
        if (TextUtil.isNullOrEmpty(ssoId) == false)
          {
            LOG.debug("Loging in user by SSO");
            loginSSO(req, res, C, ssoId);
            return;
          }

        LOG.debug("Loging in user locally");
        loginRegular(req, res, C, U);

      }

    protected static void loginSSO(RequestUtil req, ResponseUtil res, Connection C, String ssoId)
    throws Exception, CloneNotSupportedException, ResourceNotAuthorizedException, AccessForbiddenException, IOException
      {
        SAMLUserProfile userProfile = ConfigSAML.processCallback(req.getHttpServletRequest(), res.getHttpServletResponse(), C, ssoId);
        if (userProfile == null)
          return;
        req.setSessionInt(SessionUtil.Attributes.FORCE_COMMIT.name(), SessionUtil.FORCE_COMMIT);
        User_Data U = User_Factory.lookupByEmail(userProfile._email);
        if (U.read(C) == false)
          throw new Exception("SSO user '" + userProfile._email + "' not found in the local DB.");
        SSOConfig ssoConfig = Wanda.getSsoConfig(ssoId);
        if (ssoConfig._eula == false)
          req.setSessionInt(SessionUtil.Attributes.EULA_CLEAR.toString(), 1);
        // Automatically clearing users for plan on SSO login.
        PlanHelper.clearUserForPlan(C, req, U, false);
        LoginHelper.basicLoginSuccess(req, C, U);
        U.write(C);
        U.syncUpApps(C, U, ssoConfig._defaultPromoCode, ssoId);
        String returnUrl = userProfile._returnUrl != null ? userProfile._returnUrl : Wanda.getUrlRedirectPostLogin();
        LOG.debug("SSO login successful. Redirecting to " + returnUrl);
        res.sendRedirect(returnUrl);
      }

    protected static void loginRegular(RequestUtil req, ResponseUtil res, Connection C, User_Data U)
    throws Exception
      {
        User_Data impersonatedU = null;
        
        if (U == null) // actual login operation
          {
            String email = req.getParamString("email", true);
            String pswd = req.getParamString("pswd", true);
            String emailImpersonation = req.getParamString("emailImpersonation", false);
            req.throwIfErrors();

            // To override the DB rollback
            req.setSessionInt(SessionUtil.Attributes.FORCE_COMMIT.name(), SessionUtil.FORCE_COMMIT);

            // Look up user by email
            LOG.debug("Getting user");
            email = email.toLowerCase();
            U = User_Factory.lookupByEmail(email);
            // If we cannot read the user, of the user has been soft-deleted, locked or the invite has been cancelled, we cannot proceed.
            String errMsg = null; 
            if (U.read(C) == false)
             errMsg = "User '" + email + "' not found in the local DB.";
            else if (U.isNullDeleted() == false)
             errMsg = "User '" + email + "' is soft-deleted.";
            else if (U.isLocked() == true)
             errMsg = "User '" + email + "' is locked.";
            else if (U.getInviteCancelled() == true)
             errMsg = "User '" + email + "' has a canceled invitation.";
            // Check password is correct
            else if (EncryptionUtil.hash(pswd, U.getPswdSalt()).equals(U.getPswd()) == false)
             errMsg = "Invalid password for User '" + email + "' in the local DB";

            if (errMsg != null)
              {
                LOG.error(errMsg);
                LoginHelper.loginFailure(C, U);
                return;
              }

            // Check the password is still valid (the rules for password validation may have changed)
            LOG.debug("Validating password");
            if (Wanda.validatePassword(pswd).size() > 0)
              {
                U.sendForgotPswdEmail(C);
                throw new AccessForbiddenException("User", "Password invalid as per Password Rules. A password reset email has been sent.");
              }

            // Has the password expired?
            LOG.debug("Checking if password expired");
            if (hasPasswordExpired(U) == true)
              {
                U.sendForgotPswdEmail(C);
                throw new AccessForbiddenException("User", "Your password has expired. A password reset email has been sent.");
              }

            // use the login process to migrate to using the salt if it hasn't been set before.
            if (TextUtil.isNullOrEmpty(U.getPswdSalt()) == true)
              {
                // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                // !!! NOTE: WE ARE RELYING ON THE "loginSuccess" TO COMMIT EVENTUALLY THE USER OBJECT. !!!
                // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                LOG.debug("Upgrading user's salt.");
                String salt = U.getOrCreatePswdSalt();
                pswd = EncryptionUtil.hash(pswd, salt);
                U.setPswdSalt(salt);
                U.setPswd(pswd);
              }
            
            // Check impersonation only if the user is a super admin
            if (U.isSuperAdmin() == true && TextUtil.isNullOrEmpty(emailImpersonation) == false)
              {
                impersonatedU = User_Factory.lookupByEmail(emailImpersonation);
                if (impersonatedU.read(C) == false)
                 throw new AccessForbiddenException("User", "Your password has expired. A password reset email has been sent.");
              }
          }
        
        try
          {
            LoginHelper.loginSuccess(req, res, C, U, impersonatedU);
          }
        finally
          {
            U.write(C);
            if (impersonatedU != null)
             impersonatedU.write(C);
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


  }
