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

package wanda.servlets.organization;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import wanda.data.OrganizationACL_Data;
import wanda.data.OrganizationACL_Factory;
import wanda.data.OrganizationRoleView_Factory;

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import tilda.utils.TextUtil;
import wanda.data.User_Data;
import wanda.data.User_Factory;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.exceptions.NotFoundException;

@WebServlet("/svc/wanda/organizations/acl/create")
public class OrganizationACLCreate extends SimpleServlet
  {
    private static final long     serialVersionUID = 1L;
    protected static final Logger LOG              = LogManager.getLogger(OrganizationACLCreate.class.getName());

    public OrganizationACLCreate()
      {
        super(true, true, false);
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        long organizationRefnum = Req.getParamLong("organizationRefnum", true);
        long userRefnum = Req.getParamLong("userRefnum", true);
        char role = Req.getParamChar("role", true);
        if (OrganizationACL_Data.checkRole(role) == false)
          Req.addError("role", "Invalid role specified: allowed are " + TextUtil.print(OrganizationACL_Data._role_Values, 0) + ".");
        // Prevent granting access to yourself.
        if (userRefnum == U.getRefnum())
          Req.addError("userRefnum", "Cannot grant ACL to yourself — the creator always has full access.");

        Req.throwIfErrors();
        OrganizationRoleView_Factory.checkOrganizationAcl(C, U, organizationRefnum, OrganizationRoleView_Factory.OrganizationRole.ADMIN);

        // Look up the target user to capture their id.
        User_Data target = User_Factory.lookupByPrimaryKey(userRefnum);
        if (target.read(C) == false)
          throw new NotFoundException("User", "" + userRefnum, "User " + userRefnum + " cannot be found.");

        // Upsert: if an active entry already exists, update the role; otherwise create.
        OrganizationACL_Data acl = OrganizationACL_Factory.create(organizationRefnum, target.getRefnum(), target.getId(), role, U.getRefnum(), U.getId());
        acl.setNullDeleted(); // clear deleted in case of upsert
        if (acl.upsert(C) == false)
          throw new Exception("Database error: cannot save ACL entry for organization " + organizationRefnum + " / user " + userRefnum + ".");

        Res.successJson("", acl);
      }
  }
