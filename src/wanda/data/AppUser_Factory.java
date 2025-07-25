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
import tilda.db.DeleteQuery;

/**
 * This is the application class <B>Data_AppUser</B> mapped to the table <B>PEOPLE.AppUser</B>.
 * 
 * @see wanda.data._Tilda.TILDA__APPUSER
 */
public class AppUser_Factory extends wanda.data._Tilda.TILDA__APPUSER_Factory
  {
    protected static final Logger LOG = LogManager.getLogger(AppUser_Factory.class.getName());

    protected AppUser_Factory()
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

    public static int deleteUserApps(Connection C, long userRefnum)
    throws Exception
      {
        DeleteQuery Q = newDeleteQuery(C);
        Q.where().equals(COLS.USERREFNUM, userRefnum);
        return Q.execute();
      }

    public static void addUserAccess(Connection C, long appRefnum, long[] userRefnums) throws Exception
      {
        for (long userRefnum : userRefnums)
          {
            AppUser_Data ap = AppUser_Factory.create(appRefnum);
            ap.setUserRefnum(userRefnum);
            ap.write(C);
          }
      }

    public static int removeUserAccess(Connection C, long appRefnum, long[] userRefnums)
    throws Exception
      {
        DeleteQuery Q = newDeleteQuery(C);
        Q.where().equals(COLS.APPREFNUM, appRefnum)
        .and().in(COLS.USERREFNUM, userRefnums);
        return Q.execute();
      }
  }
