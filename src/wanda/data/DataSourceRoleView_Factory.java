/* ===========================================================================
 * Copyright (C) 2026 CapsicoHealth Inc.
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

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import tilda.db.*;
import wanda.web.exceptions.NotFoundException;

/**
This is the application class <B>Data_DataSourceRoleView</B> mapped to the table <B>WANDA.DataSourceRoleView</B>.
@see wanda.data._Tilda.TILDA__DATASOURCEROLEVIEW
*/
public class DataSourceRoleView_Factory extends wanda.data._Tilda.TILDA__DATASOURCEROLEVIEW_Factory
 {
   protected static final Logger LOG = LogManager.getLogger(DataSourceRoleView_Factory.class.getName());

   protected DataSourceRoleView_Factory() { }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//   Implement your customizations, if any, below.
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


   public static void init(Connection C) throws Exception
    {
      // Add logic to initialize your object, for example, caching some values, or validating some things.
    }

   // Simplified Cache Definition
   static private Cache<Long, Cache<Long, Character>> _ACL_DATASOURCE_CACHE = CacheBuilder.newBuilder().maximumSize(100).expireAfterAccess(30, TimeUnit.MINUTES).build();

   public static enum DataSourceRole
     {
     READER, WRITER, ADMIN, OWNER
     };

   public static void checkDataSourceAcl(Connection C, User_Data U, long dataSourceRefnum, DataSourceRole requiredRole)
   throws Exception
     {
       if (requiredRole == null)
         throw new IllegalArgumentException("Required Role cannot be null");

       // 1. Get or Create the data-source-specific cache atomically
       Cache<Long, Character> userCache = _ACL_DATASOURCE_CACHE.get(dataSourceRefnum, () -> {
         return CacheBuilder.newBuilder().maximumSize(100).expireAfterAccess(15, TimeUnit.MINUTES).build();
       });

       // 2. Get or Compute the specific user's role atomically
       char r = userCache.get(U.getRefnum(), () -> {
         DataSourceRoleView_Data DRV = DataSourceRoleView_Factory.lookupByDataSourceCreatorOrAccess(dataSourceRefnum, U.getRefnum(), U.getRefnum());
         if (DRV.read(C) == false)
           return Character.valueOf((char) Character.UNASSIGNED);
         if (DRV.getCreatorRefnum() == U.getRefnum())
           return Character.valueOf('O');
         return Character.valueOf(DRV.getRole());
       });

       if (requiredRole == DataSourceRole.OWNER && DataSourceRoleView_Data._roleOwner == r)
         return;
       if (requiredRole == DataSourceRole.READER && r != Character.UNASSIGNED)
         return;
       if (requiredRole == DataSourceRole.WRITER && (DataSourceRoleView_Data._roleOwner == r || DataSourceRoleView_Data._roleAdmin == r || DataSourceRoleView_Data._roleWriter == r))
         return;
       if (requiredRole == DataSourceRole.ADMIN && (DataSourceRoleView_Data._roleOwner == r || DataSourceRoleView_Data._roleAdmin == r))
         return;

       throw new NotFoundException("DataSource", "" + dataSourceRefnum, "Data source " + dataSourceRefnum + " is not " + (requiredRole == DataSourceRole.READER ? "accessible" : "updatable") + " by this user.");
     }

 }
