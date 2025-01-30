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

import tilda.db.Connection;
import tilda.db.ListResults;
import tilda.db.SelectQuery;
import tilda.utils.TextUtil;
import wanda.web.config.Wanda;

/**
 * This is the application class <B>Data_AppUserView</B> mapped to the table <B>PEOPLE.AppUserView</B>.
 * 
 * @see wanda.data._Tilda.TILDA__APPUSERVIEW
 */
public class AppUserView_Factory extends wanda.data._Tilda.TILDA__APPUSERVIEW_Factory
  {
    protected static final Logger LOG = LogManager.getLogger(AppUserView_Factory.class.getName());

    protected AppUserView_Factory()
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
     * Allows to transparently get a list of apps for a specific user and include, if the requester is an administrator, the list
     * of apps the user doesn't have access to. Allows to returns a list of apps both for an administrative UI where all available
     * apps should be shown, vs a regular use (e.g., a hamburger menu) where only accessible apps should be used (and avoid leakage
     * of data not accessible).<BR>
     * This method delagates appropriately to lookupWhereUserByAllApp or lookupWhereUserByActiveApp.
     * 
     * @param C
     * @param currentUser The current request/transaction user
     * @param userRefnum The user this request is for
     * @param start start index for the returned result set
     * @param size size of the returned result set
     * @return
     * @throws Exception
     */
    public static ListResults<AppUserView_Data> getUserApps(Connection C, User_Data currentUser, long userRefnum, int start, int size)
    throws Exception
      {
        ListResults<AppUserView_Data> L = currentUser.isSuperAdmin() == true ? lookupWhereUserByAllApp(C, Wanda.getHostName(), userRefnum, 0, -1)
        : lookupWhereUserByActiveApp(C, Wanda.getHostName(), userRefnum, 0, -1);
        return L;
      }



    public static ListResults<AppUserView_Data> getAppUsers(Connection C, User_Data U, App_Data A, boolean enabledOnly, String promoCode, String searchQuery, int start, int size)
    throws Exception
      {
        SelectQuery Q = newWhereQuery(C);
        
        Q.equals(COLS.APPREFNUM, A.getRefnum())
        .and().notEquals(COLS.USERREFNUM, U.getRefnum());
        
        if (enabledOnly == true)
         Q.equals(COLS.ENABLED, true);
        
        if (searchQuery != null && searchQuery.length() > 0)
          {
            Q.and()
            .openPar()
            .or().like(COLS.USERNAMEFIRST, "%" + searchQuery + "%", true)
            .or().like(COLS.USERNAMELAST, "%" + searchQuery + "%", true)
            .or().like(COLS.USERID, "%" + searchQuery + "%", true) // that's the email!
            .closePar();
          }

        if (TextUtil.isNullOrEmpty(promoCode) == false)
          {
            if (promoCode.equals("___UNASSIGNED___") == true)
              Q.and().isNull(COLS.USERPROMOCODE);
            else
              Q.and().equals(COLS.USERPROMOCODE, promoCode);
          }
        
        Q.orderBy(COLS.USERNAMELAST, true);
        Q.orderBy(COLS.USERNAMEFIRST, true);
        
        return runSelect(C, Q, start, size);
      }
  }
