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

import java.io.PrintWriter;
import jakarta.servlet.annotation.WebServlet;
import wanda.data.Tenant_Data;
import wanda.data.Tenant_Factory;
import wanda.data.TenantView_Data;
import wanda.data.TenantView_Factory;
import wanda.data.User_Data;
import wanda.servlets.helpers.RoleHelper;

import tilda.db.Connection;
import tilda.db.ListResults;
import tilda.utils.json.JSONUtil;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;

/**
*
* @author mohan
* API to return list of Tenants to which logged in user has access.
*/

@WebServlet("/svc/admin/tenants")
public class TenantListServlet extends SimpleServlet
  {
    private static final long serialVersionUID = 5942786964807123071L;

    public TenantListServlet()
      {
        super(true);
      }

    @Override
    protected void justDo(RequestUtil req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        throwIfUserInvalidRole(U, RoleHelper.ADMINROLES);
        if (U.hasRoles(RoleHelper.SUPERADMIN) == true)
          {
            ListResults<Tenant_Data> tenants = Tenant_Factory.getAll(C, 0, 1000);
            PrintWriter Out = Res.setContentType(ResponseUtil.ContentType.JSON);
            JSONUtil.response(Out, "", tenants);
          }
        else
          {
            ListResults<TenantView_Data> tenants = TenantView_Factory.getAllByUserRefnum(C, U.getRefnum(), 0, 1000);
            PrintWriter Out = Res.setContentType(ResponseUtil.ContentType.JSON);
            JSONUtil.response(Out, "tenantJson", tenants);
          }
      }
  }
