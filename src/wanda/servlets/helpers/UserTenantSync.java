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

    public static void sync(Connection MasterConnection,
    User_Data MasterDbUser, long TenantUserRefnum)
    throws Exception
      {
        Connection TenantConnection = null;
        Exception currentException = null;
        UserDetail_Data MasterDbPerson = MasterDbUser.getUserDetails(); 
        try
          {
            // Load MasterDbPerson
            if (MasterDbPerson == null)
              {
                MasterDbPerson = UserDetail_Factory.lookupByUserRefnum(MasterDbUser.getRefnum());
                if(MasterDbPerson.read(MasterConnection) == false)
                  {
                    String ErrMessage = "Failed to load Person: "+MasterDbUser.getRefnum()+"";
                    throw new ServletException(ErrMessage);
                  }            
              }
            
            LOG.info("********************Sync**************************");
            TenantUser_Data TenantUser = TenantUser_Factory.lookupByPrimaryKey(TenantUserRefnum);
            if(TenantUser.read(MasterConnection) == false)
              {
                String ErrMessage = "Cannot Sync User "+MasterDbUser.getRefnum()+": No TenantUser Data with refnum "+TenantUserRefnum;
                throw new ServletException(ErrMessage);
              }
            Tenant_Data Tenant = Tenant_Factory.lookupByPrimaryKey(TenantUser.getTenantRefnum());
            if(Tenant.read(MasterConnection) == false)
              {
                String ErrMessage = "Cannot Sync User "+MasterDbUser.getRefnum()+": No Tenant Data with refnum "+TenantUser.getTenantRefnum();
                throw new ServletException(ErrMessage);
              }
            TenantConnection = ConnectionPool.get(Tenant.getConnectionId());
            
            User_Data TenantDbUser = User_Factory.lookupByPrimaryKey(MasterDbUser.getRefnum());
            if(TenantDbUser.read(TenantConnection) == true)
              {
                MasterDbUser.copyTo(TenantDbUser);
                TenantDbUser.write(TenantConnection);
              }              
            else
              {
                User_Data tenantDbUser = User_Data.cloneWithCreateMode(MasterDbUser);
                tenantDbUser.write(TenantConnection);
              }

            UserDetail_Data TenantDbPerson = UserDetail_Factory.lookupByUserRefnum(MasterDbUser.getRefnum());
            MasterDbPerson.copyTo(TenantDbPerson);
            if (TenantDbPerson.write(TenantConnection) == false)
              {
                TenantDbPerson = UserDetail_Data.cloneWithCreateMode(MasterDbPerson);
                LOG.debug("\n\nMasterDbUser: "  +MasterDbUser.getRefnum()
                           +"\nTenantDbUser: "  +TenantDbUser.getRefnum()
                           +"\nMasterDbPerson: "+MasterDbPerson.getUserRefnum()                           
                           +"\nTenantDbPerson: "+TenantDbPerson.getUserRefnum()
                           +"\n\n");
                TenantDbPerson.write(TenantConnection);
              }
            TenantConnection.commit();
          }
        catch(Exception e)
          {
            if(TenantConnection != null)
              TenantConnection.rollback();
            currentException = e;
          }
        finally 
          {
            if(TenantConnection != null)
              TenantConnection.close();
            LOG.info("********************Sync Done**************************");
            if(currentException != null)
                throw currentException;
          }
      }

  }
