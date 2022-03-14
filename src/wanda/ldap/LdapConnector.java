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

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import wanda.data.LDAPDomain_Data;
import wanda.data.LDAPServer_Data;
import tilda.utils.TextUtil;

/**
 * Query Active Directory using Java
 * 
 */
public class LdapConnector {
	// Logger
	private static final Logger LOG = Logger.getLogger(LdapConnector.class.getName());

	// required private variables
	private Properties properties;
	private DirContext dirContext;
	private SearchControls searchCtls;
	private String[] returnAttributes = { "badPasswordTime", "badPwdCount", "lastLogoff", "lastLogon",
			"lastLogonTimestamp", "logonCount", "pwdLastSet", "whenChanged", "whenCreated", "sAMAccountName", "cn",
			"userPrincipalName", "name", "givenName", "sn", "displayName", "mail", "title", "description", "company",
			"department", "telephoneNumber", "url", "mobile", "distinguishedName" };
	private LDAPDomain_Data domainData;
	private LDAPServer_Data serverData;

	/**
	 * constructor with parameter for initializing a LDAP context
	 * 
	 * @param serverData
	 *            a {@link LDAPServer_Data} object - ServerData to establish a LDAP
	 *            connection
	 * @param domainData
	 *            a {@link LDAPDomain_} object - DomainData to establish a LDAP
	 *            connection
	 */
	public LdapConnector(LDAPServer_Data serverData, LDAPDomain_Data domainData) {

		this.domainData = domainData;
		this.serverData = serverData;

		properties = new Properties();
		properties.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		properties.put(Context.PROVIDER_URL, getProviderUrl());
		properties.put(Context.SECURITY_PRINCIPAL, domainData.getAdmin_username());
		properties.put(Context.SECURITY_CREDENTIALS, domainData.getAdmin_password());

		// initializing active directory LDAP connection
		try {
			dirContext = new InitialDirContext(properties);
		} catch (NamingException e) {
			LOG.severe(e.getMessage());
		}

		// initializing search controls
		searchCtls = new SearchControls();
		searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
		searchCtls.setReturningAttributes(returnAttributes);
	}

	private Object getProviderUrl() {
		StringBuffer sb = new StringBuffer();
		sb.append(serverData.getSecureConnection() ? "LDAPS://" : "LDAP://");
		sb.append(serverData.getUrl());
		sb.append(":" + serverData.getPort());
		return sb.toString();
	}

	public List<LdapUser> searchUser(String searchValue) {
		List<LdapUser> users = new ArrayList<LdapUser>();
		String filter = getUserSearchFilter(searchValue);
		String searchBase = getUserSearchDn();
		NamingEnumeration<SearchResult> result = null;

		try {
			result = this.dirContext.search(searchBase, filter, this.searchCtls);
			while (true) {
				if (result.hasMore()) {
					SearchResult rs = (SearchResult) result.next();
					Attributes attrs = rs.getAttributes();
					users.add(getLdapUser(attrs));
				} else {
					break;
				}
			}
		} catch (NamingException e) {
			LOG.warning(e.getMessage());
		}
		return users;
	}

	public List<LdapGroup> searchGroups(String searchValue) {
		List<LdapGroup> groups = new ArrayList<LdapGroup>();
		String filter = getGroupSearchFilter(searchValue);
		String searchBase = getGroupSearchDn();
		NamingEnumeration<SearchResult> result = null;

		try {
			result = this.dirContext.search(searchBase, filter, this.searchCtls);
			while (true) {
				if (result.hasMore()) {
					SearchResult rs = (SearchResult) result.next();
					Attributes attrs = rs.getAttributes();
					groups.add(getLdapGroup(attrs));
				} else {
					break;
				}
			}
		} catch (NamingException e) {
			LOG.warning(e.getMessage());
		}
		return groups;
	}

	private static LdapGroup getLdapGroup(Attributes attrs) {
		
		LdapGroup group = new LdapGroup(getAttributeValue("cn", attrs));
		group.setName(getAttributeValue("name", attrs));
		group.setSamaccountname(getAttributeValue("samaccountname", attrs));
		group.setDescription(getAttributeValue("description", attrs));
		group.setEmail(getAttributeValue("mail", attrs));
		group.setDn(getAttributeValue("distinguishedName", attrs));
		//group.setWhenCreated(getZoneDateTime(getAttributeValue("whenCreated", attrs)));
		//group.setWhenChanged(getZoneDateTime(getAttributeValue("whenChanged", attrs)));
		
		return group;
	}
	
	private static LdapUser getLdapUser(Attributes attrs) {
		
		LdapUser user = new LdapUser(getAttributeValue("userPrincipalName", attrs));
		user.setFirstName(getAttributeValue("givenName", attrs));
		user.setLastName(getAttributeValue("sn", attrs));
		user.setName(getAttributeValue("name", attrs));
		user.setDisplayName(getAttributeValue("displayName", attrs));
		user.setTitle(getAttributeValue("title", attrs));
		user.setDescription(getAttributeValue("description", attrs));
		user.setEmail(getAttributeValue("mail", attrs));
		user.setCompany(getAttributeValue("company", attrs));
		user.setDepartment(getAttributeValue("department", attrs));
		user.setMobile(getAttributeValue("mobile", attrs));
		user.setTelephoneNumber(getAttributeValue("telephoneNumber", attrs));
		user.setUrl(getAttributeValue("url", attrs));
		user.setDn(getAttributeValue("distinguishedName", attrs));
		
		user.setLastLogoff(getZoneDateTime(getAttributeValue("lastLogoff", attrs)));
		user.setLastLogon(getZoneDateTime(getAttributeValue("lastLogon", attrs)));
		user.setLastLogonTimestamp(getZoneDateTime(getAttributeValue("lastLogonTimestamp", attrs)));
		user.setLogonCount(Integer.parseInt(getAttributeValue("logonCount", attrs)));
		
		user.setBadPasswordTime(getZoneDateTime(getAttributeValue("badPasswordTime", attrs)));
		user.setBadPwdCount(Integer.parseInt(getAttributeValue("badPwdCount", attrs)));
		user.setPwdLastSet(getZoneDateTime(getAttributeValue("pwdLastSet", attrs)));
		
		//user.setWhenCreated(getZoneDateTime(getAttributeValue("whenCreated", attrs)));
		//user.setWhenChanged(getZoneDateTime(getAttributeValue("whenChanged", attrs)));
		
		return user;
	}
	private static ZonedDateTime getZoneDateTime(String ldapDateString){
		//LOG.info("Ldap Date String: "+ldapDateString);
		if(TextUtil.isNullOrEmpty(ldapDateString))
			return null;
		long dateLong = Long.parseLong(ldapDateString, 10);
		if(dateLong == 0)
			return null;
		long javaTime = dateLong - 0x19db1ded53e8000L;
		javaTime /= 10000;
		//LOG.info("Ldap Java date: "+new Date(javaTime));
		Instant i = Instant.ofEpochSecond( javaTime / 1000);
		ZonedDateTime z = ZonedDateTime.ofInstant( i,  ZoneId.of("Etc/UTC") );
		//LOG.info("Ldap Java zone dt: "+z.toString());
		return z;
	}

	private String getUserSearchDn() {
		return domainData.getUser_dn() + "," + domainData.getBase_dn();
	}

	private String getGroupSearchDn() {
		return domainData.getGroup_dn() + "," + domainData.getBase_dn();
	}

	private String getUserSearchFilter(String searchValue) {
		if(TextUtil.isNullOrEmpty(searchValue))
			return domainData.getUser_filter();
		else
		return "(&" + domainData.getUser_filter() + domainData.getUser_search_filter().replace("?", searchValue) + ")";
	}

	private String getGroupSearchFilter(String searchValue) {
		if(TextUtil.isNullOrEmpty(searchValue))
			return domainData.getGroup_filter();
		else 
			return "(&" + domainData.getGroup_filter() + domainData.getGroup_search_filter().replace("?", searchValue) + ")";
	}

	private static String getAttributeValue(String attributeName, Attributes attributes) {
		Attribute attribute = attributes.get(attributeName);
		if (attribute == null)
			return "";
		String attributeValue = attribute.toString();
		if (attributeValue == null)
			return "";
		attributeValue = attributeValue.substring(attributeValue.indexOf(":") + 2);
		return attributeValue;
	}

	public LdapUser authenticateUser(String username, String password) {

		boolean authFailed;

		try {
			Properties props = new Properties();
			props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
			props.put(Context.PROVIDER_URL, getProviderUrl());
			props.put(Context.SECURITY_PRINCIPAL, username);
			props.put(Context.SECURITY_CREDENTIALS, password);

			InitialDirContext context = new InitialDirContext(props);
			context.close();
			authFailed = false;
		} catch (Exception e) {
			System.out.print(e.toString());
			authFailed = true;
		}
		if (authFailed)
			return null;
		List<LdapUser> users = searchUser(getSamAccountNameFromEmail(username));
		if (!users.isEmpty()) {
			return users.get(0);
		}
		return null;
	}

	private static String getSamAccountNameFromEmail(String email) {
		return email.substring(0, email.indexOf("@"));
	}

	public void closeLdapConnection() {
		try {
			if (dirContext != null)
				dirContext.close();
		} catch (NamingException e) {
			LOG.severe(e.getMessage());
		}
	}
}
