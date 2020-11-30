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
package wanda.servlets.admin;

import java.util.List;

import javax.servlet.annotation.WebServlet;

import wanda.data.Role_Data;
import wanda.data.Role_Factory;
import wanda.data.TenantUser_Factory;
import wanda.data.User_Data;
import wanda.data.User_Factory;
import wanda.servlets.helpers.RoleHelper;

import tilda.db.Connection;
import tilda.utils.CollectionUtil;
import tilda.utils.SystemValues;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;

@WebServlet("/svc/admin/user/invite")
public class InviteUserServlet extends SimpleServlet
  {

    private static final long serialVersionUID = -4044066138357179403L;

    public InviteUserServlet()
      {
        super(true);
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        throwIfUserInvalidRole(U, RoleHelper.ADMINROLES);
        String email = Req.getParamString("email", true);
        String firstName = Req.getParamString("firstName", true);
        String lastName = Req.getParamString("lastName", true);
        String[] roles = Req.getParamsString("role", true);
        long Refnum = Req.getParamLong("refnum", false);

        long[] tenantRefnums = Req.getParamsLong("tenant", false);
        long[] oldTenantRefnums = Req.getParamsLong("oldTenant", false);
        if(tenantRefnums == null)
            tenantRefnums = new long[]{};
        if(oldTenantRefnums == null)
            oldTenantRefnums = new long[]{};
        List<Long> tenantRefnumList = CollectionUtil.toList(tenantRefnums);

        long[] appRefnums = Req.getParamsLong("app", false);
        if(appRefnums == null)
            appRefnums = new long[]{};
        
        Req.throwIfErrors();
        if (U.hasRoles(RoleHelper.SUPERADMIN) == false)
          {
            for(long tenantRefnum : oldTenantRefnums)
             if(TenantUser_Factory.hasTenant(C, U.getRefnum(), tenantRefnum) == false)
              Req.addError("oldTenant["+tenantRefnum+"]", "User refnum="+U.getRefnum()+" does not have access to tenant refnum="+tenantRefnum);
            Req.throwIfErrors();
            for(long tenantRefnum : tenantRefnumList)
             if(TenantUser_Factory.hasTenant(C, U.getRefnum(), tenantRefnum) == true)
              Req.addError("tenant["+tenantRefnum+"]", "User refnum="+U.getRefnum()+" does not have access to tenant refnum="+tenantRefnum);
            Req.throwIfErrors();            
          }

        for (String role : roles)
          {
            Role_Data roleData = Role_Factory.lookupByValue(role);
            if (roleData == null)
              Req.addError("role", "cannot find role with value='" + role + "'");
            if ("SA".equalsIgnoreCase(roleData.getId()))
              throwIfUserInvalidRole(U, RoleHelper.SUPERADMIN);
          }
        Req.throwIfErrors();
       
        if (Refnum != SystemValues.EVIL_VALUE)
          { // valid refnum
            User_Data refnumUser = User_Factory.lookupByPersonRefNum(Refnum);
            if (refnumUser.read(C) == false)
              {
                if(refnumUser.hasRoles(RoleHelper.SUPERADMIN) == true)
                  {
                    Req.addError("refnum", "User not allowed to update");
                  }
                Req.addError("refnum", "User not found");
              }
            Req.throwIfErrors();

            User_Data.updateDetailsAndInvite(C, refnumUser, email, firstName, lastName, roles, appRefnums, tenantRefnumList, oldTenantRefnums);
          }
        else
          {
            User_Data emailUser = User_Factory.lookupByEmail(email);
            if (emailUser.read(C))
              Req.addError("email", "User already exists with email '" + email + "'");
            Req.throwIfErrors();
            User_Data.inviteUser(C, email, firstName, lastName, roles, tenantRefnums, appRefnums);
          }
        
        Req.throwIfErrors();
        Res.success();        
      }

  }
