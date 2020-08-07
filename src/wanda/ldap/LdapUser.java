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

public class LdapUser {

	private String name;
	private String username;
	private String email;
	private String firstName;
	private String lastName;
	private String displayName;
	private String title;
	private String description;
	private String company;
	private String department;
	private String telephoneNumber;
	private String mobile;
	private String url;
	private String dn;

	private ZonedDateTime badPasswordTime;
	private ZonedDateTime lastLogonTimestamp;
	private ZonedDateTime lastLogoff;
	private ZonedDateTime lastLogon;
	private ZonedDateTime pwdLastSet;
	private ZonedDateTime whenCreated;
	private ZonedDateTime whenChanged;
	private int badPwdCount;
	private int logonCount;
	
	public LdapUser(String username) {
		super();
		this.username = username;
	}
	public String toString(){
		return "Name: " + getName() + " Username: " + getUsername() + " Email: " + getEmail();
	}
	
	public static void print( List<LdapUser> users, PrintWriter Out) throws IOException {
		boolean First = true;
		Out.println("[ ");

        for (LdapUser aLdapUser : users) {
        	if (First == true) {
				Out.print("   ");
				First = false;
			} else
				Out.print("  ,");
        	aLdapUser.toJSON(Out);
        }

		Out.println(" ]");
	}
	
	public void toJSON(PrintWriter Out) throws IOException {
		Out.println(" {");
		
		JSONUtil.print(Out, "name", true, this.name);
		JSONUtil.print(Out, "username", false, this.username);
		JSONUtil.print(Out, "email", false, this.email);
		JSONUtil.print(Out, "firstName", false, this.firstName);
		JSONUtil.print(Out, "lastName", false, this.lastName);
		JSONUtil.print(Out, "displayName", false, this.displayName);
		JSONUtil.print(Out, "title", false, this.title);
		JSONUtil.print(Out, "description", false, this.description);
		JSONUtil.print(Out, "company", false, this.company);
		JSONUtil.print(Out, "department", false, this.department);
		JSONUtil.print(Out, "telephoneNumber", false, this.telephoneNumber);
		JSONUtil.print(Out, "mobile", false, this.mobile);
		JSONUtil.print(Out, "url", false, this.url);
		JSONUtil.print(Out, "dn", false, this.dn);
		JSONUtil.print(Out, "badPasswordTime", false, this.badPasswordTime);
		JSONUtil.print(Out, "lastLogonTimestamp", false, this.lastLogonTimestamp);
		JSONUtil.print(Out, "lastLogoff", false, this.lastLogoff);
		JSONUtil.print(Out, "lastLogon", false, this.lastLogon);
		JSONUtil.print(Out, "pwdLastSet", false, this.pwdLastSet);
		JSONUtil.print(Out, "whenCreated", false, this.whenCreated);
		JSONUtil.print(Out, "whenChanged", false, this.whenChanged);
		JSONUtil.print(Out, "badPwdCount", false, this.badPwdCount);
		JSONUtil.print(Out, "logonCount", false, this.logonCount);

		Out.println(" }");

	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getCompany() {
		return company;
	}
	public void setCompany(String company) {
		this.company = company;
	}
	public String getDepartment() {
		return department;
	}
	public void setDepartment(String department) {
		this.department = department;
	}
	public String getTelephoneNumber() {
		return telephoneNumber;
	}
	public void setTelephoneNumber(String telephoneNumber) {
		this.telephoneNumber = telephoneNumber;
	}
	public String getMobile() {
		return mobile;
	}
	public void setMobile(String mobile) {
		this.mobile = mobile;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getDn() {
		return dn;
	}
	public void setDn(String dn) {
		this.dn = dn;
	}
	public ZonedDateTime getBadPasswordTime() {
		return badPasswordTime;
	}
	public void setBadPasswordTime(ZonedDateTime badPasswordTime) {
		this.badPasswordTime = badPasswordTime;
	}
	public ZonedDateTime getLastLogonTimestamp() {
		return lastLogonTimestamp;
	}
	public void setLastLogonTimestamp(ZonedDateTime lastLogonTimestamp) {
		this.lastLogonTimestamp = lastLogonTimestamp;
	}
	public ZonedDateTime getLastLogoff() {
		return lastLogoff;
	}
	public void setLastLogoff(ZonedDateTime lastLogoff) {
		this.lastLogoff = lastLogoff;
	}
	public ZonedDateTime getLastLogon() {
		return lastLogon;
	}
	public void setLastLogon(ZonedDateTime lastLogon) {
		this.lastLogon = lastLogon;
	}
	public ZonedDateTime getPwdLastSet() {
		return pwdLastSet;
	}
	public void setPwdLastSet(ZonedDateTime pwdLastSet) {
		this.pwdLastSet = pwdLastSet;
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
	public int getBadPwdCount() {
		return badPwdCount;
	}
	public void setBadPwdCount(int badPwdCount) {
		this.badPwdCount = badPwdCount;
	}
	public int getLogonCount() {
		return logonCount;
	}
	public void setLogonCount(int logonCount) {
		this.logonCount = logonCount;
	}
	
}
