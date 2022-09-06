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

import javax.servlet.annotation.WebServlet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import wanda.data.TenantView_Data;
import wanda.data.TenantView_Factory;
import wanda.data.Tenant_Data;
import wanda.data.Tenant_Factory;
import wanda.data.User_Data;
import wanda.data.User_Factory;
import wanda.servlets.helpers.RoleHelper;

import tilda.db.Connection;
import tilda.db.ListResults;
import tilda.utils.json.JSONUtil;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.exceptions.NotFoundException;

@WebServlet("/svc/admin/users/tenants")
/**
 *
 * @author mohan
 * API to return list of Tenants to which logged in user has access.
 */
public class UserTenantList extends SimpleServlet
  {
    /**
   * 
   */
    private static final long serialVersionUID = -628664735749111388L;
    protected static final Logger LOG = LogManager.getLogger(UserTenantList.class.getName());


    /**
     * Default constructor.
     */
    public UserTenantList()
      {
        super(true);
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
      throws Exception
      {
        throwIfUserInvalidRole(U, RoleHelper.ADMINROLES);

        long UserRefnum = Req.getParamLong("userRefnum", true);
        Req.throwIfErrors();
        User_Data User = User_Factory.lookupByPrimaryKey(UserRefnum);
        if(User.read(C) == false)
          {
            throw new NotFoundException("User", UserRefnum);
          }
        PrintWriter Out = Res.setContentType(ResponseUtil.ContentType.JSON);
        if(User.hasRoles(RoleHelper.SUPERADMIN) == true)
          {
            ListResults<Tenant_Data> tenants = Tenant_Factory.getAll(C, 0, 1000);
            JSONUtil.response(Out, "", tenants);
          }
        else
          {
            ListResults<TenantView_Data> list = TenantView_Factory.getAllByUserRefnum(C, UserRefnum, 0, 1000);
            if(U.hasRoles(RoleHelper.SUPERADMIN) == false)
              {
                for(TenantView_Data TV : list)
                  {
                    if(TenantView_Factory.hasTenantAccess(C, U.getRefnum(), TV.getRefnum()) == false)
                      {
                        list.remove(TV);
                      }
                  }
              }
            JSONUtil.response(Out, "tenantJson", list);            
          }
      }

  }
