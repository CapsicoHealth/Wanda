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

import wanda.data.Tenant_Data;
import wanda.data.Tenant_Factory;
import wanda.data.User_Data;
import wanda.servlets.helpers.RoleHelper;

import tilda.data.Connection_Data;
import tilda.data.Connection_Factory;
import tilda.db.Connection;
import tilda.utils.HttpStatus;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.exceptions.BadRequestException;
import wanda.web.exceptions.SimpleServletException;

//@WebServlet("/svc/admin/tenants/create")
public class TenantCreateServlet extends SimpleServlet
  {

    private static final long serialVersionUID = -858813614538822539L;
  
    public TenantCreateServlet()
      {
        super(true);
      }
  
    @Override
    protected void justDo(RequestUtil req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        throwIfUserInvalidRole(U, RoleHelper.SUPERADMIN);
        String name = req.getParamString("name", true);
        String description = req.getParamString("description", true);
        String loginMessage = req.getParamString("loginMsg", false);
        String connectionRefnum = req.getParamString("connectionId", true);
        req.throwIfErrors();
        
        Connection_Data connectionObj = Connection_Factory.lookupByPrimaryKey(connectionRefnum);
        if(connectionObj.read(C) == false)
          throw new BadRequestException("connectionId", "is Invalid or doesn't exist");
        
        Tenant_Data tenant = Tenant_Factory.create(name, description, loginMessage, connectionRefnum);
        try
          {
            tenant.write(C);
          }
        catch(Exception E)
          {
            LOG.error("Exception while creating Tenant.", E);
            throw new SimpleServletException(HttpStatus.BadRequest, "Failed to create Tenant");
          }

        Res.Success();        
      }  
    

  }
