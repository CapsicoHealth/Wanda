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

/**
 * 
 */
package wanda.servlets;

import javax.servlet.annotation.WebServlet;

import tilda.db.Connection;
import wanda.data.User_Data;
import wanda.data.User_Factory;
import wanda.servlets.helpers.RoleHelper;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.config.WebBasics;

@WebServlet("/svc/user/guest/registration")
public class GuestRegistration extends SimpleServlet
  {

    private static final long serialVersionUID = -4044066138357179403L;

    public GuestRegistration()
      {
        super(false);
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        String email = Req.getParamString("email", true);
        String fName = Req.getParamString("fName", true);
        String lName = Req.getParamString("lName", true);

        if (WebBasics.getGuestRegistrationAllowed() == false)
          Req.addError("email", "Guest registrations are not allowed");
        
        User_Data user = User_Factory.lookupByEmail(email);
        if (user.read(C) == true)
         Req.addError("email", "This email is invalid or has already been registered");
        
        Req.throwIfErrors();

        String[] guestRole = RoleHelper.GUEST;
        long[]  appRefnums = WebBasics.getGuestRegistrationAppRefnums();

        User_Data emailUser = User_Factory.lookupByEmail(email);
        if (emailUser.read(C))
          Req.addError("email", "User already exists with email '" + email + "'");
        Req.throwIfErrors();
        User_Data.inviteUser(C, email, fName, lName, guestRole, null, appRefnums);

        Req.throwIfErrors();
        Res.success();
      }

  }
