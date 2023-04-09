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

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.annotation.WebServlet;

import wanda.data.User_Data;
import wanda.data.User_Factory;
import wanda.web.config.WebBasics;

import tilda.db.Connection;
import tilda.utils.CompareUtil;
import tilda.utils.EncryptionUtil;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SessionFilter;
import wanda.web.SessionUtil;
import wanda.web.SimpleServlet;
import wanda.web.exceptions.NotFoundException;
import wanda.web.exceptions.ResourceNotAuthorizedException;

@WebServlet("/svc/user/setPswd")
public class SetPassword extends SimpleServlet
  {
    private static final long serialVersionUID = 988554219257979935L;

    public SetPassword()
      {
        super(false, false);
      }

    @Override
    public void init(ServletConfig Conf)
      {
        SessionFilter.addMaskedUrlNvp("password");
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        String Email = Req.getParamString("email", true);
        String Token = Req.getParamString("token", true);
        String Password = Req.getParamString("password", true);
        List<String> Errors = WebBasics.validatePassword(Password);
        int FailCount = 0;
        String ErrorMessage = null;
        if (!Errors.isEmpty())
          {
            for (String Error : Errors)
              {
                Req.addError("password", Error);
              }
          }
        Req.throwIfErrors();
        User_Data user = User_Factory.lookupByEmail(Email.toLowerCase());
        if(user.read(C) == false)
          {
            throw new NotFoundException("User", Email, "Cannot find user with email "+Email);
          }
        if (CompareUtil.equals(Token, user.getPswdResetCode()) == false || (user.isNullLocked() == false && ChronoUnit.MILLIS.between(ZonedDateTime.now(), user.getLocked()) > 0))
          {
            Req.setSessionInt(SessionUtil.Attributes.FORCE_COMMIT.name(), SessionUtil.FORCE_COMMIT);
            User_Data.markUserLoginFailure(C, user);
            FailCount = WebBasics.getLoginAttempts() - user.getFailCount(); 
            ErrorMessage = CompareUtil.equals(Token, user.getPswdResetCode()) == false ? "The token supplied is no longer valid. Please request a new reset token."
                         : FailCount <= 0 ? "Your account is locked! You have exeeded maximum password reset or login attempts" 
                         : "Unable to reset your password, you have "+FailCount+" attempts remaining";
            throw new ResourceNotAuthorizedException("User", Email, ErrorMessage);
          }

        if (ChronoUnit.MINUTES.between(user.getPswdResetCreate(), ZonedDateTime.now()) > WebBasics.getResetCodeTTL())
          {
            Req.addError("token", "The token has expired (after "+WebBasics.getResetCodeTTL()+" mn). Please request a new reset token.");
          }

        String hashedPassword = EncryptionUtil.hash(Password);
        if (user.hasPswdHist(hashedPassword))
          {
            Req.addError("password", "Password Already used");
          }
        Req.throwIfErrors();
        user.setPswd(hashedPassword);
        user.setPswdCreateNow();
        user.setNullPswdResetCode();
        user.setNullPswdResetCreate();
        user.setNullLocked();
        user.pushToPswdHistory(hashedPassword);
        user.write(C);
        Res.success();
      }
  }
