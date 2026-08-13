/* ===========================================================================
 * Copyright (C) 2022 CapsicoHealth Inc.
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

import tilda.db.Connection;
import wanda.data.SystemEmail_Data;
import wanda.data.SystemEmail_Factory;
import wanda.data.User_Data;
import wanda.servlets.helpers.RoleHelper;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.exceptions.NotFoundException;

@WebServlet("/svc/admin/emails/resend")
public class SystemEmailResend extends SimpleServlet
  {
    private static final long serialVersionUID = 1650604646546394526L;
  
    public SystemEmailResend()
      {
        super(true);
      }
  
    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        throwIfUserInvalidRole(U, RoleHelper.ADMINROLES);
        long refnum = Req.getParamLong("refnum", true);
        Req.throwIfErrors();
        SystemEmail_Data Email = SystemEmail_Factory.lookupByPrimaryKey(refnum);
        if(Email.read(C) == false)
          {
            throw new NotFoundException("SystemEmail", refnum);
          }
        Email.deliver(U, C);
        Res.success();        
      }

  }
