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

import tilda.db.Connection;
import tilda.db.ConnectionPool;
import tilda.db.ListResults;
import tilda.utils.SystemValues;
import wanda.data.AdminUsersAndTenantsView_Data;
import wanda.data.AdminUsersAndTenantsView_Factory;
import wanda.data.AdminUsersView_Data;
import wanda.data.AdminUsersView_Factory;
import wanda.data.TenantUser_Data;
import wanda.data.TenantUser_Factory;
import wanda.data.User_Data;
import wanda.servlets.helpers.RoleHelper;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SessionUtil;
import wanda.web.SimpleServlet;

/**
 * Servlet implementation class UserList
 */
@WebServlet("/svc/admin/users")
public class UserListServlet extends SimpleServlet
  {

    /**
     * 
     */
    private static final long serialVersionUID = -1915570402303726836L;

    public UserListServlet()
      {
        super(false);
      }

    @Override
    protected void justDo(RequestUtil req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        throwIfUserInvalidRole(U, RoleHelper.ADMINROLES);
        String searchQuery = req.getParamString("query", false, "");
        String[] roles = req.getParamsString("role", false);
        String status = req.getParamString("status", false, "");
        String promoCode = req.getParamString("promoCode", false, "");
        boolean csv = req.getParamBoolean("csv", false);
        long TenantRefnum = SystemValues.EVIL_VALUE;
        long TenantUserRefnum = req.getSessionLong(SessionUtil.Attributes.TENANTUSERREFNUM.toString());
        if (ConnectionPool.isMultiTenant() && U.hasRoles(RoleHelper.SUPERADMIN) == false)
          {
            TenantUser_Data TenantUser = TenantUser_Factory.lookupByPrimaryKey(TenantUserRefnum);
            if (TenantUser.read(C) == true)
              {
                TenantRefnum = TenantUser.getTenantRefnum();
              }
            else
              {
                req.addError("TenantUserRefnum", "Cannot Find Tenant User with Refnum = " + TenantUserRefnum);
              }
          }
        req.throwIfErrors();
        if (ConnectionPool.isMultiTenant())
          {
            ListResults<AdminUsersAndTenantsView_Data> L = AdminUsersAndTenantsView_Factory.filter(C, U, searchQuery, roles, TenantRefnum, status, promoCode, 0, 500);
            if (csv == true)
             Res.successCsv("Simple", L);
            else
             Res.successJson("", L);
          }
        else
          {
            ListResults<AdminUsersView_Data> L = AdminUsersView_Factory.filter(C, U, searchQuery, roles, status, promoCode, 0, 500);
            if (csv == true)
              Res.successCsv("Simple", L);
             else
              Res.successJson("", L);
          }
      }
  }
