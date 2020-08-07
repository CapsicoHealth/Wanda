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
import tilda.db.*;

/**
This is the application class <B>Data_TENANTVIEW</B> mapped to the table <B>PEOPLE.TENANTVIEW</B>.
@see wanda.data._Tilda.TILDA__TENANTVIEW
*/
public class TenantView_Factory extends wanda.data._Tilda.TILDA__TENANTVIEW_Factory
 {
   protected static final Logger LOG = LogManager.getLogger(TenantView_Factory.class.getName());

   protected TenantView_Factory() { }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//   Implement your customizations, if any, below.
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


   public static void init(Connection C) throws Exception
    {
      // Add logic to initialize your object, for example, caching some values, or validating some things.
    }
   
   public static ListResults<TenantView_Data> getAllByUserRefnum(Connection C, Long UserRefnum, int Start, int Size)
   throws Exception
     {
        SelectQuery Q = newWhereQuery(C);
        Q.equals(COLS.USERREFNUM, UserRefnum);
        return runSelect(C, Q, Start, Size);
     }
   
   public static boolean hasTenantAccess(Connection C, Long UserRefnum, Long TenantRefnum)
   throws Exception
     {
       return getTenant(C, UserRefnum, TenantRefnum) == null;
     }

   public static TenantView_Data getTenant(Connection C, Long UserRefnum, Long TenantRefnum)
   throws Exception
     {
       SelectQuery Q = newWhereQuery(C);
       Q.equals(COLS.USERREFNUM, UserRefnum);
       Q.and().equals(COLS.REFNUM, TenantRefnum);
       Q.and().equals(COLS.TENANTUSERACTIVE, true);
       ListResults<TenantView_Data> L = runSelect(C, Q, 0, 1);
       return L.size() > 0 ? L.get(0) : null;
     }

   public static TenantView_Data getTenantByTenantUserRefnum(Connection C, Long UserRefnum, Long TenantUserRefnum)
   throws Exception
     {
       SelectQuery Q = newWhereQuery(C);
       Q.equals(COLS.USERREFNUM, UserRefnum);
       Q.and().equals(COLS.TENANTUSERREFNUM, TenantUserRefnum);
       Q.and().equals(COLS.TENANTUSERACTIVE, true);
       Q.and().equals(COLS.TENANTACTIVE, true);
       ListResults<TenantView_Data> L = runSelect(C, Q, 0, 1);
       return L.size() > 0 ? L.get(0) : null;
     }


   public static ListResults<TenantView_Data> getAllActiveByUserRefnum(Connection C, Long UserRefnum, int Start, int Size)
   throws Exception
     {
        SelectQuery Q = newWhereQuery(C);
        Q.equals(COLS.USERREFNUM, UserRefnum);
        Q.and().equals(COLS.TENANTUSERACTIVE, true);
        Q.and().equals(COLS.TENANTACTIVE, true);
        return runSelect(C, Q, Start, Size);
     }


 }
