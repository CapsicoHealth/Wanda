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
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import tilda.utils.EncryptionUtil;
import tilda.utils.json.JSONUtil;
import wanda.data.UserDetail_Data;
import wanda.data.UserDetail_Factory;
import wanda.data.User_Data;
import wanda.data.User_Factory;
import wanda.data.importers.promos.Plan;
import wanda.servlets.helpers.PlanHelper;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SessionFilter;
import wanda.web.SimpleServlet;
import wanda.web.config.Wanda;
import wanda.web.exceptions.NotFoundException;

@WebServlet("/svc/user/onboarding")
public class UserOnBoarding extends SimpleServlet
  {
    private static final long serialVersionUID = 2358369573367773870L;

    public UserOnBoarding()
      {
        super(false, true);
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
        String email = Req.getParamString("email", true);
        String token = Req.getParamString("token", true);
        String password = Req.getParamString("password", true);
        String company = Req.getParamString("company", false);
        String title = Req.getParamString("title", false);
        String phone = Req.getParamString("phone", false);
        String country = Req.getParamString("country", false);
        String stateProv = Req.getParamString("stateProv", false);

        List<String> passwordHistory = new ArrayList<String>();
        List<String> errors = Wanda.validatePassword(password);
        if (errors.isEmpty() == false)
          for (String error : errors)
            Req.addError("password", error);

        Req.throwIfErrors();

        U = User_Factory.lookupByEmail(email);
        if (U.read(C) == false)
          throw new NotFoundException("User email", email);

        if (U.checkTokenValidity(token) == false)
          throw new NotFoundException("User token", "Invalid or expired token");

        /*
         * find user with +token
         * validate +phone
         * validate +password && not user.pswdHist contains +password
         * find_or_create user.contact and set +phone to contact.telMobile
         * update user password with +password
         * set +password to user.pswd
         * set Time.now to user.pswdCreate
         * add +password to user.pswdHist
         * 
         */
        String salt = U.getOrCreatePswdSalt();
        String hashedPassword = EncryptionUtil.hash(password, salt);
        if (U.hasPswdHist(hashedPassword))
          {
            // Is this a user who has already logged in, or a user still in the process of registering?
            if (U.getLoginCount() > 0)                       // an existing established user changing their password and failing
             throw new Exception("Password Already used");   // the already-used-password check.
          }
        else
          {
            passwordHistory.add(hashedPassword);
          }

        UserDetail_Data contact = UserDetail_Factory.lookupByUserRefnum(U.getRefnum());
        contact.setCompany(company);
        contact.setProfTitle(title);
        contact.setTelMobile(phone);
        contact.setCountry(country);
        contact.setStateProv(stateProv);
        contact.write(C);

        U.setPswd(hashedPassword);
        U.setPswdSalt(salt);
        U.setPswdCreateNow();
        U.setInvitedUser(false);
        U.setPswdHist(passwordHistory);
        U.write(C);

        PrintWriter Out = Res.setContentType(ResponseUtil.ContentType.JSON);
        JSONUtil.startOK(Out, '{');
        List<Plan> plans = PlanHelper.getAvailablePlans(C, U);
        if (plans != null && plans.isEmpty() == false)
          {
            JSONUtil.print(Out, "mustPickPlan", true, true);
            JSONUtil.print(Out, "email", false, email);
            JSONUtil.print(Out, "token", false, token);
            JSONUtil.print(Out, "message", false, "Successfully registered. Please pick a plan now.");
          }
        else
          {
            JSONUtil.print(Out, "message", true, "Successfully registered. Please Login.");
          }
        JSONUtil.end(Out, '}');
      }

  }
