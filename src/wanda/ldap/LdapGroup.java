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
import java.time.ZonedDateTime;
import java.util.List;

import tilda.utils.json.JSONUtil;

public class LdapGroup {
	private String name;
	private String samaccountname;
	private String cn;
	private String description;
	private String email;
	private String dn;

	private ZonedDateTime whenCreated;
	private ZonedDateTime whenChanged;
	
	public static void printGroups( List<LdapGroup> groups, PrintWriter Out) throws IOException {
		boolean First = true;
		Out.println("[ ");

        for (LdapGroup ldapGroup : groups) {
        	if (First == true) {
				Out.print("   ");
				First = false;
			} else
				Out.print("  ,");
        	ldapGroup.toJSON(Out);
        }

		Out.println(" ]");
	}
	
	public void toJSON(PrintWriter Out) throws IOException {
		Out.println(" {");
		
		JSONUtil.print(Out, "name", true, this.name);
		JSONUtil.print(Out, "samaccountname", false, this.samaccountname);
		JSONUtil.print(Out, "cn", false, this.cn);
		JSONUtil.print(Out, "description", false, this.description);
		JSONUtil.print(Out, "email", false, this.email);
		
		JSONUtil.print(Out, "whenCreated", false, this.whenCreated);
		JSONUtil.print(Out, "whenChanged", false, this.whenChanged);

		Out.println(" }");

	}
	
	public LdapGroup(String cn) {
		super();
		this.cn = cn;
	}
	public String getSamaccountname() {
		return samaccountname;
	}

	public void setSamaccountname(String samaccountname) {
		this.samaccountname = samaccountname;
	}
	public String getDn() {
		return dn;
	}
	public void setDn(String dn) {
		this.dn = dn;
	}
	public String getCn() {
		return cn;
	}

	public void setCn(String cn) {
		this.cn = cn;
	}
	public String toString(){
		return "Name: " + getName() + " samaccountname: " + getSamaccountname() + " eMail: " + getEmail() + " CN: " + getCn() + " DN: " + getDn() + " description: " + getDescription();
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public ZonedDateTime getWhenCreated() {
		return whenCreated;
	}
	public void setWhenCreated(ZonedDateTime whenCreated) {
		this.whenCreated = whenCreated;
	}
	public ZonedDateTime getWhenChanged() {
		return whenChanged;
	}
	public void setWhenChanged(ZonedDateTime whenChanged) {
		this.whenChanged = whenChanged;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	
}
