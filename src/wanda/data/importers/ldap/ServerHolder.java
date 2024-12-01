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

package wanda.data.importers.ldap;

import java.util.ArrayList;
import java.util.List;

import wanda.data.LDAPDomain_Data;
import wanda.data.LDAPRoleGroup_Data;
import wanda.data.LDAPServer_Data;
import com.google.gson.annotations.SerializedName;

import tilda.db.Connection;

public class ServerHolder
  {
    /* @formatter:off */
	@SerializedName("server")
	public LDAPServer_Data server = null;
	@SerializedName("domain")
	public LDAPDomain_Data domain = null;
	@SerializedName("roleGroups")
	public List<LDAPRoleGroup_Data> roleGroups = new ArrayList<LDAPRoleGroup_Data>();

	/* @formatter:on */

    public int write(Connection C)
    throws Exception
      {
        int count = 0;

        if (server == null)
          throw new Exception("The element 'server' cannot be null or missing.");

        count++;
        
        if (server.upsert(C) == false)
          throw new Exception("Cannot create Server record");

        if (domain != null)
          {
            domain.initServer_rn(server.getRefnum());
            if (domain.write(C) == false)
              throw new Exception("Cannot insert domain " + domain.getDomain_name());
            if (roleGroups != null && !roleGroups.isEmpty())
              {
                for (LDAPRoleGroup_Data roleGroup : roleGroups)
                  {
                    roleGroup.initDomain_rn(domain.getRefnum());
                    if (roleGroup.write(C) == false)
                      throw new Exception("Cannot insert roleGroup " + roleGroup.getRoleId());
                  }
              }
          }

        return count;
      }
    /* @formatter:on */
  }
