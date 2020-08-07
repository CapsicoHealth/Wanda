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
This is the application class <B>Data_TENANTUSER</B> mapped to the table <B>PEOPLE.TENANTUSER</B>.
@see wanda.data._Tilda.TILDA__TENANTUSER
*/
public class TenantUser_Factory extends wanda.data._Tilda.TILDA__TENANTUSER_Factory
 {
   protected static final Logger LOG = LogManager.getLogger(TenantUser_Factory.class.getName());

   protected TenantUser_Factory() { }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//   Implement your customizations, if any, below.
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


   public static void init(Connection C) throws Exception
    {
      // Add logic to initialize your object, for example, caching some values, or validating some things.
    }

   public static ListResults<TenantUser_Data> getAllByUserRefnum(Connection C, Long UserRefnum, int Start, int Size)
   throws Exception
    {
       SelectQuery Q = newWhereQuery(C);
       Q.equals(COLS.USERREFNUM, UserRefnum);
       return runSelect(C, Q, Start, Size);
    }
   
   
   public static TenantUser_Data LookUpByTenantRefnum(Connection C, long TenantRefnum, long UserRefnum)
   throws Exception
    {
      SelectQuery Q = newWhereQuery(C);
      Q.equals(COLS.TENANTREFNUM, TenantRefnum);
      Q.and();
      Q.equals(COLS.USERREFNUM, UserRefnum);
      ListResults<TenantUser_Data> tUsers = runSelect(C, Q, 0, 1);
      return (tUsers == null || tUsers.size() == 0) ? null : tUsers.get(0);
    }
   
   public static boolean hasTenants(Connection C, long UserRefnum, long[] TenantRefnums)
   throws Exception
     {
       if(TenantRefnums.length == 0)
         {
           return false;
         }
       SelectQuery Q = newWhereQuery(C);
       Q.equals(COLS.USERREFNUM, UserRefnum);
       Q.and();
       Q.in(COLS.TENANTREFNUM, TenantRefnums);
       ListResults<TenantUser_Data> tUserData =  runSelect(C, Q, 0, TenantRefnums.length);
       return tUserData.size() == TenantRefnums.length;
     }

 
   public static boolean hasTenant(Connection C, long UserRefnum, long TenantRefnum)
   throws Exception
     {
       if(TenantRefnum == SystemValues.EVIL_VALUE)
         {
           return false;
         }
       SelectQuery Q = newWhereQuery(C);
       Q.equals(COLS.USERREFNUM, UserRefnum);
       Q.and();
       Q.equals(COLS.TENANTREFNUM, TenantRefnum);
       ListResults<TenantUser_Data> tUserData =  runSelect(C, Q, 0, 1);
       return tUserData.size() == 1;
     }
}
