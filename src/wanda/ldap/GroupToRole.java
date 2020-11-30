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

import javax.servlet.ServletConfig;
import javax.servlet.annotation.WebServlet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import wanda.data.LDAPRoleGroup_Data;
import wanda.data.User_Data;

import tilda.db.Connection;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.exceptions.NotFoundException;

@WebServlet("/svc/ldap/group-role")
public class GroupToRole extends SimpleServlet
  {
    private static final long     serialVersionUID = 1018123535563202342L;
    protected static final Logger LOG              = LogManager.getLogger(GroupToRole.class.getName());

    /*
     * Sample test urls
     * https://localhost:8443/reports/svc/ldap/group-role?role_id=CarCo&domain_rn=2&group_cn=Care Coordinators
     */
    public GroupToRole()
      {
        super(false);
      }

    @Override
    public void init(ServletConfig Conf)
      {
        // TODO: do a dry run. validate the config and basic login tests
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {

        String roleId = Req.getParamString("role_id", true);
        long domainRn = Req.getParamLong("domain_rn", true);
        String groupCn = Req.getParamString("group_cn", false);

        Req.throwIfErrors();

        RoleGroup roleGroup = new RoleGroup(roleId, domainRn, groupCn);
        LDAPRoleGroup_Data rg = new LdapUtils().mapGroupToRole(roleGroup, C);

        if (rg != null)
          {
            Res.success();
          }
        else
          throw new NotFoundException("RoleGroup", groupCn+"|"+domainRn);

      }

  }
