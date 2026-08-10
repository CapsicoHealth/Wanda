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
This is the application class <B>Data_OrganizationRoleView</B> mapped to the table <B>WANDA.OrganizationRoleView</B>.
@see wanda.data._Tilda.TILDA__ORGANIZATIONROLEVIEW
*/
public class OrganizationRoleView_Factory extends wanda.data._Tilda.TILDA__ORGANIZATIONROLEVIEW_Factory
 {
   protected static final Logger LOG = LogManager.getLogger(OrganizationRoleView_Factory.class.getName());

   protected OrganizationRoleView_Factory() { }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//   Implement your customizations, if any, below.
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


   public static void init(Connection C) throws Exception
    {
      // Add logic to initialize your object, for example, caching some values, or validating some things.
    }

   // Simplified Cache Definition
   static private Cache<Long, Cache<Long, Character>> _ACL_ORGANIZATION_CACHE = CacheBuilder.newBuilder().maximumSize(100).expireAfterAccess(30, TimeUnit.MINUTES).build();

   public static enum OrganizationRole
     {
     READER, WRITER, ADMIN, OWNER
     };

   public static void checkOrganizationAcl(Connection C, User_Data U, long organizationRefnum, OrganizationRole requiredRole)
   throws Exception
     {
       if (requiredRole == null)
         throw new IllegalArgumentException("Required Role cannot be null");

       // 1. Get or Create the organization-specific cache atomically
       Cache<Long, Character> userCache = _ACL_ORGANIZATION_CACHE.get(organizationRefnum, () -> {
         return CacheBuilder.newBuilder().maximumSize(100).expireAfterAccess(15, TimeUnit.MINUTES).build();
       });

       // 2. Get or Compute the specific user's role atomically
       char r = userCache.get(U.getRefnum(), () -> {
         OrganizationRoleView_Data ORV = OrganizationRoleView_Factory.lookupByOrganizationCreatorOrAccess(organizationRefnum, U.getRefnum(), U.getRefnum());
         if (ORV.read(C) == false)
           return Character.valueOf((char) Character.UNASSIGNED);
         if (ORV.getCreatorRefnum() == U.getRefnum())
           return Character.valueOf('O');
         return Character.valueOf(ORV.getRole());
       });

       if (requiredRole == OrganizationRole.OWNER && OrganizationRoleView_Data._roleOwner == r)
         return;
       if (requiredRole == OrganizationRole.READER && r != Character.UNASSIGNED)
         return;
       if (requiredRole == OrganizationRole.WRITER && (OrganizationRoleView_Data._roleOwner == r || OrganizationRoleView_Data._roleAdmin == r || OrganizationRoleView_Data._roleWriter == r))
         return;
       if (requiredRole == OrganizationRole.ADMIN && (OrganizationRoleView_Data._roleOwner == r || OrganizationRoleView_Data._roleAdmin == r))
         return;

       throw new NotFoundException("Organization", "" + organizationRefnum, "Organization " + organizationRefnum + " is not " + (requiredRole == OrganizationRole.READER ? "accessible" : "updatable") + " by this user.");
     }

 }
