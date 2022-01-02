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

import javax.servlet.ServletConfig;
import javax.servlet.annotation.WebServlet;

import tilda.db.Connection;
import tilda.db.ListResults;
import tilda.utils.EncryptionUtil;
import tilda.utils.json.JSONUtil;
import wanda.data.UserDetail_Data;
import wanda.data.UserDetail_Factory;
import wanda.data.User_Data;
import wanda.data.User_Factory;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SessionFilter;
import wanda.web.SimpleServlet;
import wanda.web.config.WebBasics;
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
        String phone = Req.getParamString("phone", false);
        List<String> passwordHistory = new ArrayList<String>();
        List<String> errors = WebBasics.validatePassword(password);
        if (!errors.isEmpty())
          {
            for (String error : errors)
              {
                Req.addError("password", error);
              }
          }
        Req.throwIfErrors();

        User_Data user = User_Factory.lookupByPswdResetCode(token);
        if (user.read(C) == false)
          throw new NotFoundException("User Token", "" + token);
        
        if (user.getEmail().equalsIgnoreCase(email) == false)
          throw new NotFoundException("User email", "" + email);

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
        String hashedPassword = EncryptionUtil.hash(password);
        if (user.hasPswdHist(hashedPassword))
          {
            throw new Exception("Password Already used");
          }
        passwordHistory.add(hashedPassword);
        if (phone != null && phone.length() > 0)
          {
            UserDetail_Data contact = UserDetail_Factory.lookupByUserRefnum(user.getRefnum());
            contact.setTelMobile(phone);
            contact.write(C);
          }
        user.setPswd(hashedPassword);
        user.setPswdCreateNow();
        user.setPswdResetCodeNull();
        user.setPswdResetCreateNull();
        user.setInvitedUser(false);
        user.setPswdHist(passwordHistory);
        user.write(C);

        PrintWriter Out = Res.setContentType(ResponseUtil.ContentType.JSON);
        JSONUtil.startOK(Out, '{');
        JSONUtil.print(Out, "message", true, "Successfully registered. Please Login.");
        JSONUtil.end(Out, '}');

      }



  }
