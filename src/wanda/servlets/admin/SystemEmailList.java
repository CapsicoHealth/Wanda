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

import java.io.PrintWriter;

import jakarta.servlet.annotation.WebServlet;

import tilda.db.Connection;
import tilda.db.ListResults;
import tilda.utils.json.JSONUtil;
import wanda.data.SystemEmail_Data;
import wanda.data.SystemEmail_Factory;
import wanda.data.User_Data;
import wanda.servlets.helpers.RoleHelper;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SessionUtil;
import wanda.web.SimpleServlet;

@WebServlet("/svc/admin/emails/list")
public class SystemEmailList extends SimpleServlet
  {
    private static final long serialVersionUID = 6063008426571562323L;

    public SystemEmailList()
    {
      super(true, false);
    }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        throwIfUserInvalidRole(U, RoleHelper.ADMINROLES);
        int Page = Req.getParamInt("Page", false);
        int Size = Req.getParamInt("Size", false);
        long tenantRefnum = Req.getSessionLong(SessionUtil.Attributes.TENANTUSERREFNUM.toString());
        
        if (Size < 1)
          Size = 1000;
        if (Page < 1)
          Page = 1;

        ListResults<SystemEmail_Data> Results = SystemEmail_Factory.getAll(C, U, tenantRefnum, (Page - 1) * Size, Size);
        PrintWriter Out = Res.setContentType(ResponseUtil.ContentType.JSON);
        JSONUtil.response(Out, "", Results);
      }

  }
