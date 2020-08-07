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

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;
import tilda.utils.EncryptionUtil;
import tilda.utils.RandomUtil;
import tilda.utils.TextUtil;
import wanda.data.LDAPDomain_Data;
import wanda.data.LDAPDomain_Factory;
import wanda.data.LDAPRoleGroup_Data;
import wanda.data.LDAPRoleGroup_Factory;
import wanda.data.LDAPServer_Data;
import wanda.data.LDAPServer_Factory;
import wanda.data.UserDetail_Data;
import wanda.data.UserDetail_Factory;
import wanda.data.User_Data;
import wanda.data.User_Factory;
import wanda.web.LoginProvider;

public class LdapUtils implements LoginProvider.Interface
  {
    protected static final Logger LOG = LogManager.getLogger(LdapUtils.class.getName());

    static
      {
        LoginProvider.RegisterProvider(new LdapUtils());
      }

    public User_Data login(Connection C, String username, String password, String domain)
    throws Exception
      {
        LdapUser user;
        if (TextUtil.isNullOrEmpty(domain) == true)
          {
            LOG.debug("Loging in user locally");
            // Authenticate using local Login
            username = username.toLowerCase();
            User_Data u = User_Factory.lookupByEmail(username);
            if (u.read(C) == false)
              {
                LOG.error("User '" + username + "' not found in the local DB");
                return null;
              }

            if (EncryptionUtil.hash(password).equals(u.getPswd()) == false)
              {
                LOG.error("Invalid password for User '" + username + "' in the local DB");
                return null;
              }

            return u;
          }

        LOG.debug("Loging in user via Domain '" + domain + "'.");
        // authenticate using LDAP login
        LDAPDomain_Data domainData = LDAPDomain_Factory.lookupByDomainName(domain);
        if (domainData.read(C) == false)
          {
            LOG.error("Domain '" + domain + "' cannot be found in the DB");
            return null;
          }

        LDAPServer_Data serverData = LDAPServer_Factory.lookupByPrimaryKey(domainData.getServer_rn());
        if (serverData.read(C) == false)
          {
            LOG.error("Server '" + domainData.getServer_rn() + "' for domain '" + domain + "' cannot be found in the DB");
            return null;
          }

        LdapConnector ldap = new LdapConnector(serverData, domainData);
        user = ldap.authenticateUser(username, password);
        if (user != null)
          {
            return createOrUpdateUser(user, C);
          }
        return null;
      }


    private User_Data createOrUpdateUser(LdapUser user, Connection C)
      {
        // LDAP user authenticated successfully
        User_Data userFromDb;
        try
          {
            userFromDb = User_Factory.lookupByid(user.getUsername());
            if (userFromDb.read(C) == false)
              {
                userFromDb = createUserInDb(user, C);
              }
            else
              {
                userFromDb = updateUserInDd(userFromDb, user, C);
              }
            return userFromDb;
          }
        catch (Exception e)
          {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        return null;
      }


    private User_Data updateUserInDd(User_Data userFromDb, LdapUser user, Connection C)
      {
        // existing user authenticate session
        // update their information
        try
          {
            UserDetail_Data p = UserDetail_Factory.lookupByUserRefnum(userFromDb.getRefnum());
            if (!p.read(C))
              {
                throw new Exception("Cannot find person with primary key " + userFromDb.getRefnum());
              }
            p.setNameFirst(user.getFirstName());
            p.setNameLast(user.getLastName());
            if (!p.write(C))
              {
                throw new Exception("Cannot write person with primary key " + userFromDb.getRefnum());
              }

            userFromDb.setLastLogin(user.getLastLogon());
            userFromDb.setLoginCount(user.getLogonCount());
            userFromDb.setFailCount(user.getBadPwdCount());
            userFromDb.setFailFirst(user.getBadPasswordTime());
            userFromDb.setEmail(user.getEmail());

            if (!userFromDb.write(C))
              {
                throw new Exception("Cannot update details of authenticated user");
              }
            return userFromDb;
          }
        catch (Exception E)
          {
            LOG.error("An exception occurred", E);
            try
              {
                C.rollback();
              }
            catch (SQLException x)
              {
              }
          }
        return null;


        // TODO: fetch user roles and save them too.
      }


    private static User_Data createUserInDb(LdapUser user, Connection c)
      {
        try
          {
            Set<String> roles = new HashSet<String>();
            roles.add("CarCo");
            User_Data U = User_Factory.create(user.getUsername(), user.getUsername(), roles, EncryptionUtil.hash("" + RandomUtil.pick(0, 100000000)));
            U.setLoginTypeLdap();

            if (U.write(c) == false)
              throw new Exception("Cannot create user");

            UserDetail_Data P = UserDetail_Factory.create(U.getRefnum(), user.getLastName(), user.getFirstName());
            if (P.write(c) == false)
              throw new Exception("Cannot create person");


            return U;
          }
        catch (Exception E)
          {
            LOG.error("An exception occurred", E);
            try
              {
                c.rollback();
              }
            catch (SQLException x)
              {
              }
          }
        return null;
      }

    public List<LdapGroup> getGroups(Connection C, String domain)
    throws Exception
      {
        if (TextUtil.isNullOrEmpty(domain))
          {
            return null;
          }

        LOG.debug("Loging in user via Domain '" + domain + "'.");
        // authenticate using LDAP login
        LDAPDomain_Data domainData = LDAPDomain_Factory.lookupByDomainName(domain);
        if (domainData.read(C) == false)
          {
            LOG.error("Domain '" + domain + "' cannot be found in the DB");
            return null;
          }

        LDAPServer_Data serverData = LDAPServer_Factory.lookupByPrimaryKey(domainData.getServer_rn());
        if (serverData.read(C) == false)
          {
            LOG.error("Server '" + domainData.getServer_rn() + "' for domain '" + domain + "' cannot be found in the DB");
            return null;
          }

        LdapConnector ldap = new LdapConnector(serverData, domainData);
        List<LdapGroup> groups = ldap.searchGroups(null);

        return groups;
      }

    public List<LdapUser> getUsers(Connection C, String domain)
    throws Exception
      {
        if (TextUtil.isNullOrEmpty(domain))
          {
            return null;
          }

        LOG.debug("Loging in user via Domain '" + domain + "'.");
        // authenticate using LDAP login
        LDAPDomain_Data domainData = LDAPDomain_Factory.lookupByDomainName(domain);
        if (domainData.read(C) == false)
          {
            LOG.error("Domain '" + domain + "' cannot be found in the DB");
            return null;
          }

        LDAPServer_Data serverData = LDAPServer_Factory.lookupByPrimaryKey(domainData.getServer_rn());
        if (serverData.read(C) == false)
          {
            LOG.error("Server '" + domainData.getServer_rn() + "' for domain '" + domain + "' cannot be found in the DB");
            return null;
          }

        LdapConnector ldap = new LdapConnector(serverData, domainData);
        List<LdapUser> users = ldap.searchUser(null);

        return users;
      }


    public LDAPRoleGroup_Data mapGroupToRole(RoleGroup roleGroup, Connection C)
      {
        // LDAP user authenticated successfully
        LDAPRoleGroup_Data roleGroupFromDb;
        try
          {
            roleGroupFromDb = LDAPRoleGroup_Factory.lookupWherePatientEpisode(C, roleGroup.getRoleId(),
            roleGroup.getDomainRn(), roleGroup.getGroupCn());

            if (roleGroupFromDb == null)
              {
                roleGroupFromDb = createRoleGroupInDb(roleGroup, C);
              }
            else
              {
                roleGroupFromDb = updateRoleGroupInDd(roleGroupFromDb, roleGroup, C);
              }
            return roleGroupFromDb;
          }
        catch (Exception e)
          {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        return null;
      }

    private LDAPRoleGroup_Data updateRoleGroupInDd(LDAPRoleGroup_Data rg, RoleGroup roleGroup, Connection C)
      {
        // existing user authenticate session
        // update their information
        try
          {
            if (!rg.read(C))
              {
                throw new Exception("Cannot find RoleGroup");
              }
            rg.setRoleId(roleGroup.getRoleId());
            rg.setGroupCn(roleGroup.getGroupCn());

            if (!rg.write(C))
              {
                throw new Exception("Cannot write roleGroup with primary key ");
              }

            return rg;
          }
        catch (Exception E)
          {
            LOG.error("An exception occurred", E);
            try
              {
                C.rollback();
              }
            catch (SQLException x)
              {
              }
          }
        return null;

      }

    private static LDAPRoleGroup_Data createRoleGroupInDb(RoleGroup roleGroup, Connection c)
      {
        try
          {
            LDAPRoleGroup_Data RG = LDAPRoleGroup_Factory.create(roleGroup.getRoleId(), roleGroup.getDomainRn(),
            roleGroup.getGroupCn());

            if (RG.write(c) == false)
              throw new Exception("Cannot create RoleGroup");

            return RG;
          }
        catch (Exception E)
          {
            LOG.error("An exception occurred", E);
            try
              {
                c.rollback();
              }
            catch (SQLException x)
              {
              }
          }
        return null;
      }

  }
