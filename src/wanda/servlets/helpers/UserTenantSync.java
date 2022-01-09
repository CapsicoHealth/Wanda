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

package wanda.servlets.helpers;

import javax.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;
import tilda.db.ConnectionPool;
import wanda.data.TenantUser_Data;
import wanda.data.TenantUser_Factory;
import wanda.data.Tenant_Data;
import wanda.data.Tenant_Factory;
import wanda.data.UserDetail_Data;
import wanda.data.UserDetail_Factory;
import wanda.data.User_Data;
import wanda.data.User_Factory;

public class UserTenantSync
  {

    protected static final Logger LOG = LogManager.getLogger(UserTenantSync.class.getName());

    public static void sync(Connection masterConnection, User_Data masterDbUser, long tenantUserRefnum)
    throws Exception
      {
        Connection tenantConnection = null;
        Exception currentException = null;
        UserDetail_Data masterDbPerson = masterDbUser.getUserDetails();
        try
          {
            // Load MasterDbPerson
            if (masterDbPerson == null)
              {
                masterDbPerson = UserDetail_Factory.lookupByUserRefnum(masterDbUser.getRefnum());
                if (masterDbPerson.read(masterConnection) == false)
                  throw new ServletException("Failed to load Person: " + masterDbUser.getRefnum() + "");
              }

            LOG.info("\n\n\n******************** Sync Start ***********************************************************************************\n");
            TenantUser_Data tenantUser = TenantUser_Factory.lookupByPrimaryKey(tenantUserRefnum);
            if (tenantUser.read(masterConnection) == false)
              throw new ServletException("Cannot Sync User " + masterDbUser.getRefnum() + ": No TenantUser Data with refnum " + tenantUserRefnum);
            Tenant_Data tenant = Tenant_Factory.lookupByPrimaryKey(tenantUser.getTenantRefnum());
            if (tenant.read(masterConnection) == false)
              throw new ServletException("Cannot Sync User " + masterDbUser.getRefnum() + ": No Tenant Data with refnum " + tenantUser.getTenantRefnum());

            LOG.debug("\n\nMasterDbUser: " + masterDbUser.getRefnum()
            + "\nMasterDbPerson: " + masterDbPerson.getUserRefnum()
            + "\n\n");

            tenantConnection = ConnectionPool.get(tenant.getConnectionId());
            User_Data tenantDbUser = User_Factory.lookupByEmail(masterDbUser.getEmail());
            if (tenantDbUser.read(tenantConnection) == true)
              {
                masterDbUser.copyTo(tenantDbUser);
                if (tenantDbUser.write(tenantConnection) == false)
                  throw new ServletException("Database error: cannot update tenant user");
              }
            else
              {
                tenantDbUser = User_Data.cloneWithCreateMode(masterDbUser);
                if (tenantDbUser.write(tenantConnection) == false)
                  throw new ServletException("Database error: cannot create tenant user");
                LOG.debug("\n\nmasterDbUser: " + masterDbUser.getRefnum()
                + "\ntenantDbUser: " + tenantDbUser.getRefnum()
                + "\n\n");
              }

            UserDetail_Data tenantDbUserDetail = UserDetail_Factory.lookupByUserRefnum(tenantDbUser.getRefnum());
            masterDbPerson.copyTo(tenantDbUserDetail);
            if (tenantDbUserDetail.write(tenantConnection) == false)
              {
                tenantDbUserDetail = UserDetail_Data.cloneWithCreateMode(masterDbPerson);
                LOG.debug("\n\nMasterDbUser: " + masterDbUser.getRefnum()
                + "\nMasterDbPerson: " + masterDbPerson.getUserRefnum()
                + "\nTenantDbUser: " + tenantDbUser.getRefnum()
                + "\nTenantDbPerson: " + tenantDbUserDetail.getUserRefnum()
                + "\n\n");
                if (tenantDbUserDetail.write(tenantConnection) == false)
                  throw new ServletException("Database error: cannot create tenant user detail");
              }
            tenantConnection.commit();
          }
        catch (Exception e)
          {
            if (tenantConnection != null)
              tenantConnection.rollback();
            currentException = e;
          }
        finally
          {
            if (tenantConnection != null)
              tenantConnection.close();
            LOG.info("\n\n\n******************** Sync Done ***********************************************************************************\n");
            if (currentException != null)
              throw currentException;
          }
      }

  }
