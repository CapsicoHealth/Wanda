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
This is the application class <B>Data_LDAPRoleGroup</B> mapped to the table <B>PEOPLE.LDAPRoleGroup</B>.
@see wanda.data._Tilda.TILDA__LDAPROLEGROUP
*/
public class LDAPRoleGroup_Data extends wanda.data._Tilda.TILDA__LDAPROLEGROUP
 {
   protected static final Logger LOG = LogManager.getLogger(LDAPRoleGroup_Data.class.getName());

   public LDAPRoleGroup_Data() { }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//   Implement your customizations, if any, below.
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


   public LDAPRoleGroup_Data(String roleId, long domainRn, String groupDn)
     {
       try
         {
           this.setRoleId(roleId);
           this.setDomain_rn(domainRn);
           this.setGroupCn(groupDn);
         }
       catch (Exception e)
         {
           // TODO Auto-generated catch block
           e.printStackTrace();
         }
     }

   @Override
   protected boolean beforeWrite(Connection C)
   throws Exception
     {
       // Do things before writing the object to disk, for example, take care of AUTO fields.
       return true;
     }

   @Override
   protected boolean afterRead(Connection C)
   throws Exception
     {
       // Do things after an object has just been read form the data store, for example, take care of AUTO fields.
       return true;
     }

   public void initDomain_rn(long refnum) throws Exception
     {
       setDomain_rn(refnum);
     }

 }
