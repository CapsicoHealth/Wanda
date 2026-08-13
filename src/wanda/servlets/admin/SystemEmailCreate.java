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

import wanda.data.SystemEmail_Data;
import wanda.data.SystemEmail_Factory;
import wanda.data.User_Data;
import wanda.servlets.helpers.RoleHelper;

import tilda.db.Connection;
import tilda.utils.HttpStatus;
import tilda.utils.SystemValues;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SessionUtil;
import wanda.web.SimpleServlet;
import wanda.web.exceptions.SimpleServletException;

@WebServlet("/svc/admin/emails/create")
public class SystemEmailCreate extends SimpleServlet
  {

    private static final long serialVersionUID = 6140358228371010178L;
    public SystemEmailCreate()
    {
      super(true);
    }
    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        throwIfUserInvalidRole(U, RoleHelper.ADMINROLES);
        String subject = Req.getParamString("subject", true);
        String body = Req.getParamString("body", true);
        Req.throwIfErrors();
        long tenantRefnum = Req.getSessionLong(SessionUtil.Attributes.TENANTUSERREFNUM.toString());
        SystemEmail_Data Email = SystemEmail_Factory.create(U.getRefnum(), subject, body);
        if(tenantRefnum != SystemValues.EVIL_VALUE)
          {
            Email.setTenantRefnum(tenantRefnum);
          }
        if(Email.write(C) == false)
          {
            throw new SimpleServletException(HttpStatus.BadRequest, "SystemEmail persistence failed");
          }
        Email.deliver(U, C);
        Res.success();
      }

  }
