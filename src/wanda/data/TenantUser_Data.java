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


package wanda.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;

/**
 * This is the application class <B>Data_TENANTUSER</B> mapped to the table <B>PEOPLE.TENANTUSER</B>.
 * 
 * @see wanda.data._Tilda.TILDA__TENANTUSER
 */
public class TenantUser_Data extends wanda.data._Tilda.TILDA__TENANTUSER
  {
    protected static final Logger LOG = LogManager.getLogger(TenantUser_Data.class.getName());

    public TenantUser_Data()
      {
      }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Implement your customizations, if any, below.
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected User_Data   _U = null;
    protected Tenant_Data _T = null;

    public void setUser(User_Data u)
      {
        _U = u;
      }

    public User_Data getUser()
      {
        return _U;
      }

    public void setTenant(Tenant_Data t)
      {
        _T = t;
      }

    public Tenant_Data getTenant()
      {
        return _T;
      }

    @Override
    protected boolean beforeWrite(Connection C)
    throws Exception
      {
        return true;
      }

    @Override
    protected boolean afterRead(Connection C)
    throws Exception
      {
        return true;
      }

    public Tenant_Data getTenant(Connection C)
    throws Exception
      {
        Tenant_Data tenant = null;
        tenant = Tenant_Factory.lookupByPrimaryKey(getTenantRefnum());
        if (tenant.read(C) == false)
          return null;
        return tenant;
      }

    public void loadDeps(Connection C)
    throws Exception
      {
        Tenant_Data tenant = getTenant(C);
        if (tenant != null)
          setTenant(tenant);
      }
  }
