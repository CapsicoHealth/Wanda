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
import tilda.utils.CollectionUtil;
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
        boolean previousUser = false;
        // If the user exists and, is not a guest or has already registered, then this is a collision.
        if (user.read(C) == true)
          {
            if (user.isGuest() == false || user.getLoginCount() > 0)
              Req.addError("email", "This email is invalid or has already been used");
            else
              previousUser = true;
          }

        Req.throwIfErrors();

        String[] guestRole = RoleHelper.GUEST;
        long[] appRefnums = WebBasics.getGuestRegistrationAppRefnums();
        long[] tenantRefnums = WebBasics.getGuestRegistrationTenantRefnums();

        if (previousUser == false)
          User_Data.inviteUser(C, email, fName, lName, guestRole, tenantRefnums, appRefnums);
        else
          User_Data.updateDetailsAndInvite(C, user, email, fName, lName, guestRole, appRefnums, CollectionUtil.toList(tenantRefnums), new long[] { });

        Res.success();
      }

  }
