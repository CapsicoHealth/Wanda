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
import wanda.data.Organization_Data;
import wanda.data.Organization_Factory;

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import wanda.data.User_Data;
import wanda.data.User_Factory;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.exceptions.NotFoundException;

/**
 * Transfers ownership of an Organization from the current owner (the caller, who must currently be the OWNER) to
 * one of its existing ADMIN members. Since there can only ever be one owner per Organization (implicit via
 * {@link Organization_Data#getCreatorRefnum()}, not an ACL row), this:
 * <UL>
 * <LI>Requires the target user to already hold an active ADMIN {@link OrganizationACL_Data} entry on this
 * Organization.</LI>
 * <LI>Removes the new owner's now-redundant ACL entry (owners don't need one -- ownership is implicit).</LI>
 * <LI>Grants the previous owner (the caller) an ADMIN ACL entry, since they are downgraded to ADMIN.</LI>
 * <LI>Updates the Organization's creatorRefnum/creatorId to the new owner.</LI>
 * </UL>
 */
@WebServlet("/svc/wanda/organizations/transferOwnership")
public class OrganizationOwnerTransfer extends SimpleServlet
  {
    private static final long     serialVersionUID = 1L;
    protected static final Logger LOG              = LogManager.getLogger(OrganizationOwnerTransfer.class.getName());

    public OrganizationOwnerTransfer()
      {
        super(true, true, false);
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        long organizationRefnum = Req.getParamLong("organizationRefnum", true);
        long newOwnerUserRefnum = Req.getParamLong("newOwnerUserRefnum", true);
        if (newOwnerUserRefnum == U.getRefnum())
          Req.addError("newOwnerUserRefnum", "You are already the owner of this organization.");
        Req.throwIfErrors();

        OrganizationRoleView_Factory.checkOrganizationAcl(C, U, organizationRefnum, OrganizationRoleView_Factory.OrganizationRole.OWNER);

        Organization_Data o = Organization_Factory.lookupByPrimaryKey(organizationRefnum);
        if (o.read(C) == false)
          throw new NotFoundException("Organization", "" + organizationRefnum, "Organization " + organizationRefnum + " cannot be found.");

        // The target must already be an active ADMIN member of this organization.
        OrganizationACL_Data newOwnerAcl = OrganizationACL_Factory.lookupByOrganizationUser(organizationRefnum, newOwnerUserRefnum);
        if (newOwnerAcl.read(C) == false || newOwnerAcl.isNullDeleted() == false || newOwnerAcl.isRoleAdmin() == false)
          throw new Exception("Ownership can only be transferred to a current Admin member of this organization.");

        User_Data newOwner = User_Factory.lookupByPrimaryKey(newOwnerUserRefnum);
        if (newOwner.read(C) == false)
          throw new NotFoundException("User", "" + newOwnerUserRefnum, "User " + newOwnerUserRefnum + " cannot be found.");

        // Owners don't carry an ACL row (ownership is implicit via creatorRefnum): remove the new owner's now-redundant entry.
        newOwnerAcl.setDeletedNow();
        if (newOwnerAcl.write(C) == false)
          throw new Exception("Database error: cannot update the new owner's access entry.");

        // Downgrade the previous owner (the caller) to ADMIN, granting/updating their ACL entry accordingly.
        OrganizationACL_Data oldOwnerAcl = OrganizationACL_Factory.lookupByOrganizationUser(organizationRefnum, U.getRefnum());
        if (oldOwnerAcl.read(C) == false)
          oldOwnerAcl = OrganizationACL_Factory.create(organizationRefnum, U.getRefnum(), U.getId(), OrganizationACL_Data._roleAdmin, U.getRefnum(), U.getId());
        else
          {
            oldOwnerAcl.setRole(OrganizationACL_Data._roleAdmin);
            oldOwnerAcl.setNullDeleted();
          }
        if (oldOwnerAcl.upsert(C) == false)
          throw new Exception("Database error: cannot grant the previous owner an Admin access entry.");

        // Transfer ownership itself.
        o.setCreatorRefnum(newOwnerUserRefnum);
        o.setCreatorId(newOwner.getId());
        o.setLastUpdatorRefnum(U.getRefnum());
        o.setLastUpdatorId(U.getId());
        if (o.write(C) == false)
          throw new Exception("Database error: cannot transfer ownership of organization " + organizationRefnum + ".");

        // Both the old and new owner's cached roles are now stale.
        OrganizationRoleView_Factory.evict(organizationRefnum, U.getRefnum());
        OrganizationRoleView_Factory.evict(organizationRefnum, newOwnerUserRefnum);

        Res.successJson("", o);
      }
  }
