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

package wanda.servlets.admin;

import jakarta.servlet.annotation.WebServlet;

import wanda.data.AdminUsersView_Data;
import wanda.data.AdminUsersView_Factory;
import wanda.data.User_Data;
import wanda.servlets.helpers.RoleHelper;

import tilda.db.Connection;
import tilda.db.ListResults;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.exceptions.BadRequestException;

@WebServlet("/svc/admin/user/validation")
public class UserValidationServlet extends SimpleServlet
  {

    private static final long serialVersionUID = -7239859216559266751L;

    public UserValidationServlet()
    {
      super(true);
    }

    @Override
    protected void justDo(RequestUtil req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        throwIfUserInvalidRole(U, RoleHelper.ADMINROLES);
        String email = req.getParamString("email", false);
        
        if(email != null && email.length() > 0) {
          ListResults<AdminUsersView_Data> users = AdminUsersView_Factory.withEmail(C, email);
          if (users.size() > 0) throw new BadRequestException("email", "already exists");
        }
        Res.success();        
      }

  }
