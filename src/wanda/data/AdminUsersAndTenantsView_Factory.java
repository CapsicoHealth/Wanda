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

/*
 Tilda V1.0 template application class.
*/

package wanda.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import wanda.servlets.helpers.RoleHelper;

import tilda.db.Connection;
import tilda.db.ListResults;
import tilda.db.SelectQuery;
import tilda.utils.SystemValues;
import tilda.utils.TextUtil;

/**
 * This is the application class <B>Data_ADMINUSERSANDTENANTSVIEW</B> mapped to the table <B>PEOPLE.ADMINUSERSANDTENANTSVIEW</B>.
 * 
 * @see wanda.data._Tilda.TILDA__ADMINUSERSANDTENANTSVIEW
 */
public class AdminUsersAndTenantsView_Factory extends wanda.data._Tilda.TILDA__ADMINUSERSANDTENANTSVIEW_Factory
  {
    protected static final Logger LOG = LogManager.getLogger(AdminUsersAndTenantsView_Factory.class.getName());

    protected AdminUsersAndTenantsView_Factory()
      {
      }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Implement your customizations, if any, below.
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    public static void init(Connection C)
    throws Exception
      {
        // Add logic to initialize your object, for example, caching some values, or validating some things.
      }


    /**
     * Returns list of {@link AdminUsersAndTenantsView_Data AdminUsersAndTenantsView}
     * filtered using account status, Tenant Name and User firstName, lastName.
     * 
     * @param C
     * @param U
     * @param searchQuery
     * @param roles
     * @param TenantRefnum
     * @param status
     * @param Start
     * @param Size
     * @return
     * @throws Exception
     */
    public static ListResults<AdminUsersAndTenantsView_Data> filter(Connection C, User_Data U, String searchQuery, String[] roles, long TenantRefnum, String status, String promoCode, int Start, int Size)
    throws Exception
      {
        SelectQuery Q = newWhereQuery(C);
        if (TenantRefnum != SystemValues.EVIL_VALUE)
          {
            Q.equals(COLS.TENANTREFNUM, TenantRefnum);
            Q.and();
          }
        if (U.hasRoles(RoleHelper.SUPERADMIN) == false)
          {
            Q.not().any(COLS.ROLES, RoleHelper.SUPERADMIN);
            Q.and();
          }

        Q.notEquals(COLS.PERSONREFNUM, U.getRefnum());

        if (searchQuery != null && searchQuery.length() > 0)
          {
            Q.and()
            .openPar()
            .like(COLS.TENANTNAME, "%" + searchQuery + "%", true)
            .or().like(COLS.NAMEFIRST, "%" + searchQuery + "%", true)
            .or().like(COLS.NAMELAST, "%" + searchQuery + "%", true)
            .or().like(COLS.PERSONID, "%" + searchQuery + "%", true) // that's the email!
            .closePar();
          }

        if (roles != null)
          {
            Q.and().any(COLS.ROLES, roles);
          }

        if (TextUtil.isNullOrEmpty(promoCode) == false)
          {
            if (promoCode.equals("___UNASSIGNED___") == true)
             Q.and().isNull(COLS.PROMOCODE);
            else
             Q.and().equals(COLS.PROMOCODE, promoCode);
          }
        
        // status = access_disabled, invited, invitation_cancelled, active, locked
        if ("access_disabled".equalsIgnoreCase(status))
          {
            Q.and().equals(COLS.TENANTUSERACTIVE, false);
          }
        else if ("invitation_cancelled".equalsIgnoreCase(status))
          {
            Q.and().isNotNull(COLS.INVITECANCELLED);
          }
        else if ("locked".equalsIgnoreCase(status))
          {
            Q.and().isNotNull(COLS.LOCKED);
          }
        else if ("active".equalsIgnoreCase(status))
          {
            Q.and().isNull(COLS.LOCKED);
            Q.and().equals(COLS.INVITEDUSER, false);
            Q.and().equals(COLS.TENANTUSERACTIVE, true);
          }
        else if ("invited".equalsIgnoreCase(status))
          {
            Q.and().equals(COLS.INVITEDUSER, true);
            Q.and().isNull(COLS.INVITECANCELLED);
          }

        // Just to keep list consistent,
        // No specific reason for ordering using them.
        Q.orderBy(COLS.NAMELAST, true);
        Q.orderBy(COLS.NAMEFIRST, true);
        Q.orderBy(COLS.TENANTNAME, true);
        return runSelect(C, Q, Start, Size);
      }


  }
