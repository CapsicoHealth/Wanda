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
import tilda.db.ListResults;
import tilda.db.SelectQuery;
import tilda.utils.SystemValues;

/**
This is the application class <B>Data_SYSTEMEMAIL</B> mapped to the table <B>ADMIN.SYSTEMEMAIL</B>.
@see wanda.data._Tilda.TILDA__SYSTEMEMAIL
*/
public class SystemEmail_Factory extends wanda.data._Tilda.TILDA__SYSTEMEMAIL_Factory
 {
   protected static final Logger LOG = LogManager.getLogger(SystemEmail_Factory.class.getName());

   protected SystemEmail_Factory() { }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//   Implement your customizations, if any, below.
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   public static ListResults<SystemEmail_Data> getAll(Connection C, User_Data U, long tenantRefnum,int Start, int Size)
   throws Exception
     {
       SelectQuery Q = newWhereQuery(C);
       Q.equals(COLS.USERREFNUM, U.getRefnum());
       if(tenantRefnum != SystemValues.EVIL_VALUE)
         {
           Q.and();
           Q.equals(COLS.TENANTREFNUM, tenantRefnum);
         }
       Q.orderBy(COLS.LASTUPDATED, false);
       return runSelect(C, Q, Start, Size);
     }

   
   public static void init(Connection C) throws Exception
    {
      // Add logic to initialize your object, for example, caching some values, or validating some things.
    }

 }
