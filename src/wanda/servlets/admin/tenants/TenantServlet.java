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

import javax.servlet.annotation.WebServlet;

import wanda.data.Tenant_Data;
import wanda.data.Tenant_Factory;
import wanda.data.User_Data;
import wanda.servlets.helpers.RoleHelper;

import tilda.db.Connection;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.exceptions.NotFoundException;

@WebServlet("/svc/admin/tenant")
public class TenantServlet extends SimpleServlet
  {

    private static final long serialVersionUID = 7106117710859964843L;

    public TenantServlet()
      {
        super(true);
      }

    @Override
    protected void justDo(RequestUtil req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        throwIfUserInvalidRole(U, RoleHelper.SUPERADMIN);
        long tenantRefnum = req.getParamLong("refnum", true);
        Boolean active = req.getParamBoolean("active", false);
        req.throwIfErrors();

        Tenant_Data tenant = Tenant_Factory.lookupByPrimaryKey(tenantRefnum);
        if (tenant.read(C) == false)
          {
            throw new NotFoundException("refnum", "Tenant not found with refnum: " + tenantRefnum);
          }

        if (active != null)
          {
            tenant.setActive(active);
            tenant.write(C);
            // LDH-NOTE: why has this been taken out????
            // Connection_Data connection = Connection_Factory.lookupByPrimaryKey(tenant.getConnectionId());
            // connection.setActive(active);
            // connection.write(C);
          }
        Res.success();        
      }

  }
