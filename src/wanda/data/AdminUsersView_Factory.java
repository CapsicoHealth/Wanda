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
import tilda.utils.DateTimeUtil;
import tilda.utils.TextUtil;

/**
 * This is the application class <B>Data_ADMINUSERSVIEW</B> mapped to the table <B>PEOPLE.ADMINUSERSVIEW</B>.
 * 
 * @see wanda.data._Tilda.TILDA__ADMINUSERSVIEW
 */
public class AdminUsersView_Factory extends wanda.data._Tilda.TILDA__ADMINUSERSVIEW_Factory
  {
    protected static final Logger LOG = LogManager.getLogger(AdminUsersView_Factory.class.getName());

    protected AdminUsersView_Factory()
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
     * Returns all {@link AdminUsersView_Data AdminUsersView}
     * 
     * @param C
     * @param Start
     * @param Size
     * @return
     * @throws Exception
     */
    public static ListResults<AdminUsersView_Data> getAll(Connection C, int Start, int Size)
    throws Exception
      {
        SelectQuery Q = newWhereQuery(C);
        Q.like(COLS.TELMOBILE, "%%")
        .orderBy(COLS.PERSONREFNUM, true);
        return runSelect(C, Q, Start, Size);
      }

    /**
     * Returns list of {@link AdminUsersView_Data AdminUsersView} filtered using account status and user firstName, lastName.
     * 
     * @param C
     * @param U
     * @param searchQuery
     * @param roles
     * @param status
     * @param Start
     * @param Size
     * @return
     * @throws Exception
     */
    public static ListResults<AdminUsersView_Data> filter(Connection C, User_Data U, String searchQuery, String[] roles, String status, String promoCode, int Start, int Size)
    throws Exception
      {
        SelectQuery Q = newWhereQuery(C);
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
            .like(COLS.NAMEFIRST, "%" + searchQuery + "%", true)
            .or().like(COLS.NAMELAST, "%" + searchQuery + "%", true)
            .or().like(COLS.PERSONID, "%" + searchQuery + "%", true)
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
        if ("access_disabled".equalsIgnoreCase(status) || "locked".equalsIgnoreCase(status))
          {
            Q.and().isNotNull(COLS.LOCKED);
            Q.and().gt(COLS.LOCKED, DateTimeUtil.nowUTC());
          }
        else if ("invitation_cancelled".equalsIgnoreCase(status))
          {
            Q.and().isNotNull(COLS.INVITECANCELLED);
          }
        else if ("active".equalsIgnoreCase(status))
          {
            Q.and().isNull(COLS.LOCKED);
            Q.or().lte(COLS.LOCKED, DateTimeUtil.nowUTC());
            Q.and().equals(COLS.INVITEDUSER, false);
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
        return runSelect(C, Q, Start, Size);
      }

    /**
     * Returns list of {@link AdminUsersView_Data AdminUsersView} filtered with email
     * 
     * @param C
     * @param email
     * @return
     * @throws Exception
     */
    public static ListResults<AdminUsersView_Data> withEmail(Connection C, String email)
    throws Exception
      {
        SelectQuery Q = newWhereQuery(C);
        Q.equals(COLS.PERSONID, email.toLowerCase());

        return runSelect(C, Q, 0, 1);
      }
  }
