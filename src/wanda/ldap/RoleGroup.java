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

package wanda.ldap;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import tilda.utils.json.JSONUtil;

public class RoleGroup {

	private String roleId;
	private long domainRn;
	private String groupCn;	
	
	public RoleGroup(String roleId, long domainRn, String groupCn) {
		this.roleId = roleId;
		this.domainRn = domainRn;
		this.groupCn = groupCn;
	}
	
	public String getRoleId() {
		return roleId;
	}

	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}

	public long getDomainRn() {
		return domainRn;
	}

	public void setDomainRn(long domainRn) {
		this.domainRn = domainRn;
	}

	public String getGroupCn() {
		return groupCn;
	}

	public void setGroupCn(String groupCn) {
		this.groupCn = groupCn;
	}
	
	public static void printGroups( List<RoleGroup> roleGroups, PrintWriter Out) throws IOException {
		boolean First = true;
		Out.println("[ ");

        for (RoleGroup roleGroup : roleGroups) {
        	if (First == true) {
				Out.print("   ");
				First = false;
			} else
				Out.print("  ,");
        	roleGroup.toJSON(Out);
        }

		Out.println(" ]");
	}
	
	public void toJSON(PrintWriter Out) throws IOException {
		Out.println(" {");
		
		JSONUtil.print(Out, "roleId", true, this.roleId);
		JSONUtil.print(Out, "domainRn", false, this.domainRn);
		JSONUtil.print(Out, "groupDn", false, this.groupCn);

		Out.println(" }");

	}
	
	public RoleGroup(String cn) {
		super();
	}
}
