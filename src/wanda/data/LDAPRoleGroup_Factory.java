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

import java.sql.ResultSet;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.*;
import tilda.db.processors.RecordProcessor;

/**
This is the application class <B>Data_LDAPRoleGroup</B> mapped to the table <B>PEOPLE.LDAPRoleGroup</B>.
@see wanda.data._Tilda.TILDA__LDAPROLEGROUP
*/
public class LDAPRoleGroup_Factory extends wanda.data._Tilda.TILDA__LDAPROLEGROUP_Factory
 {
   protected static final Logger LOG = LogManager.getLogger(LDAPRoleGroup_Factory.class.getName());

   protected LDAPRoleGroup_Factory() { }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//   Implement your customizations, if any, below.
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


   public static void init(Connection C) throws Exception
     {
       // Add logic to initialize your object, for example, caching some values, or validating some things.
     }

    public static class RoleGroupRP implements RecordProcessor
    {
        
      public RoleGroupRP(Connection C, String roleId, long domainRn, String groupCn) throws Exception
      {
     
        SelectQuery Q = newSelectQuery(C);
        Q.selectColumn(COLS.ROLEID)
        .selectColumn(COLS.DOMAIN_RN)
        .selectColumn(COLS.GROUPCN);

         Q.where().equals(COLS.ROLEID, roleId);
         Q.and().equals(COLS.DOMAIN_RN, domainRn);
         Q.and().equals(COLS.GROUPCN, groupCn);
         
        Q.execute(this, 0, -1);
        
      }

      public List<LDAPRoleGroup_Data> _L = new ArrayList<LDAPRoleGroup_Data>();

      @Override
      public void start()
        {
        }

      @Override
      public boolean process(int Index, ResultSet RS)
      throws Exception
        {
          
          TimeZone timezone = TimeZone.getTimeZone(ZoneId.systemDefault());  
          timezone.getOffset(Calendar.ZONE_OFFSET);

          String roleId = RS.getString(1);
          long domainRn = RS.getLong(2);
          String groupCn = RS.getString(3);
          
          LDAPRoleGroup_Data roleGroup = new LDAPRoleGroup_Data(roleId, domainRn, groupCn);
          _L.add(roleGroup);
          return true;
        }

      @Override
      public void end(boolean hasMore, int MaxIndex)
        {
        }
    }
     
     public static LDAPRoleGroup_Data lookupWherePatientEpisode(Connection C, String roleId, long domainRn, String groupCn   ) throws Exception
       {
         RoleGroupRP RP = new RoleGroupRP(C, roleId, domainRn, groupCn);
         return RP._L.isEmpty() == true ? null : RP._L.get(0);
       }


 }
