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

package wanda.servlets.admin.tenants;

import java.io.PrintWriter;

import jakarta.servlet.annotation.WebServlet;

import wanda.data.TenantConnectionView_Data;
import wanda.data.TenantConnectionView_Factory;
import wanda.data.User_Data;
import wanda.servlets.helpers.RoleHelper;

import tilda.db.Connection;
import tilda.db.ListResults;
import tilda.utils.json.JSONUtil;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;

@WebServlet("/svc/admin/tenants/list")
public class TenantListServlet extends SimpleServlet
  {

    private static final long serialVersionUID = 2273012215627237821L;

    public TenantListServlet()
      {
        super(true);
      }

    @Override
    protected void justDo(RequestUtil req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        throwIfUserInvalidRole(U, RoleHelper.SUPERADMIN);
        String searchQuery = req.getParamString("query", false, "");
        int Page = req.getParamInt("Page", false);
        int Size = req.getParamInt("Size", false);
        if (Size < 1)
          Size = 20;
        if (Page < 1)
          Page = 0;

        ListResults<TenantConnectionView_Data> list = TenantConnectionView_Factory.filter(C, searchQuery, (Page - 1)*Size, Size);
        PrintWriter Out = Res.setContentType(ResponseUtil.ContentType.JSON);
        JSONUtil.response(Out, "", list);
      }

  }
