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

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import tilda.utils.EncryptionUtil;
import tilda.utils.TextUtil;
import wanda.data.OrganizationACL_Data;
import wanda.data.OrganizationACL_Factory;
import wanda.data.OrganizationInvite_Data;
import wanda.data.OrganizationInvite_Factory;
import wanda.data.OrganizationRoleView_Factory;
import wanda.data.Organization_Data;
import wanda.data.Organization_Factory;
import wanda.data.Promo_Data;
import wanda.data.Promo_Factory;
import wanda.data.User_Data;
import wanda.data.User_Factory;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.exceptions.BadRequestException;
import wanda.web.exceptions.DuplicateResourceException;
import wanda.web.exceptions.NotFoundException;

/**
 * Invites a user (existing or brand new) to join an Organization with a specific ACL role.<BR>
 * <BR>
 * <B>Scenario A</B> (existing, already-registered user): a token-based OrganizationInvite is created, and an email
 * is sent to the invitee containing accept/decline links (see {@link OrganizationInvite_Data#sendOrgInviteEmailExistingUser(String)}).
 * The invite is finalized via {@link OrganizationInviteUpdate} (action=accept/decline/cancel).<BR>
 * <B>Scenario B</B> (brand new user, no account yet): a token-less OrganizationInvite is created (matched later by
 * email), and a new User account is created in "invited" state with a normal registration email (see
 * {@link User_Data#inviteUserForOrg(Connection, String, String, String, String, String)}). The invite is
 * auto-accepted once the user completes onboarding (see UserOnBoarding).<BR>
 * <BR>
 * <B>Promo code bound</B>: an Organization has no promo code of its own -- all ACLs (and any promo-code-bound user
 * limit) derive from the Organization's <B>owner</B> (its creator, see {@link Organization_Data#getOwner(Connection)}).
 * A brand new user invited under Scenario B automatically inherits that owner's promo code and counts towards its
 * maxUsers bound; an existing user invited under Scenario A keeps their own promo code (or lack thereof) and never
 * counts towards the owner's bound. This is enforced here, platform-wide (across all Organizations/users/pending
 * invites sharing that same promo code) via {@link Promo_Data#hasReachedMaxUsers(Connection)}.
 */
@WebServlet("/svc/wanda/organizations/invite/create")
public class OrganizationInviteCreate extends SimpleServlet
  {
    private static final long     serialVersionUID = 1L;
    protected static final Logger LOG              = LogManager.getLogger(OrganizationInviteCreate.class.getName());

    public OrganizationInviteCreate()
      {
        super(true, true, false);
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        long organizationRefnum = Req.getParamLong("organizationRefnum", true);
        String inviteeEmail = Req.getParamString("inviteeEmail", true);
        // Now captured directly on the OrganizationInvite record itself (WANDA.OrganizationInvite.inviteeNameFirst/
        // inviteeNameLast are non-nullable columns) rather than only on the eventual User/UserDetail row -- this
        // way the name is preserved/available even across a cancel + resend (see the front-end's "Update & Resend"
        // flow in module-login.js's _toggleInviteForm), without depending on a User account having been created yet.
        String inviteeNameFirst = Req.getParamString("nameFirst", true);
        String inviteeNameLast = Req.getParamString("nameLast", true);
        char role = Req.getParamChar("role", true);
        if (OrganizationACL_Data.checkRole(role) == false)
          Req.addError("role", "Invalid role specified: allowed are " + TextUtil.print(OrganizationACL_Data._role_Values, 0) + ".");

        Req.throwIfErrors();
        OrganizationRoleView_Factory.checkOrganizationAcl(C, U, organizationRefnum, OrganizationRoleView_Factory.OrganizationRole.ADMIN);

        Organization_Data org = Organization_Factory.lookupByPrimaryKey(organizationRefnum);
        if (org.read(C) == false)
          throw new NotFoundException("Organization", "" + organizationRefnum, "Organization " + organizationRefnum + " cannot be found.");

        // Prevent duplicate pending invites for the same org/email -- cancel first to re-invite with a different role.
        OrganizationInvite_Data existingInvite = OrganizationInvite_Factory.lookupByOrgEmailPending(organizationRefnum, inviteeEmail);
        if (existingInvite.read(C) == true)
          throw new DuplicateResourceException("OrganizationInvite", inviteeEmail, "A pending invite for '" + inviteeEmail + "' already exists for this organization.");

        User_Data target = User_Factory.lookupByEmail(inviteeEmail);
        boolean existingUser = target.read(C) == true;

        // A brand new user (Scenario B) inherits the inviting Organization owner's promo code and counts towards
        // its platform-wide maxUsers bound; an existing user (Scenario A) never touches the owner's promo code.
        String ownerPromoCode = null;
        if (existingUser == true)
          {
            // Prevent inviting someone who is already a member.
            OrganizationACL_Data existingAcl = OrganizationACL_Factory.lookupByOrganizationUser(organizationRefnum, target.getRefnum());
            if (existingAcl.read(C) == true)
              throw new DuplicateResourceException("OrganizationACL", inviteeEmail, "User '" + inviteeEmail + "' is already a member of this organization.");
          }
        else
          {
            User_Data owner = org.getOwner(C);
            ownerPromoCode = owner.isNullPromoCode() == true ? null : owner.getPromoCode();
            if (TextUtil.isNullOrEmpty(ownerPromoCode) == false)
              {
                Promo_Data promo = Promo_Factory.lookupByCode(ownerPromoCode);
                if (promo.read(C) == true && promo.hasReachedMaxUsers(C) == true)
                  throw new BadRequestException("inviteeEmail", "This organization's promo code has reached its maximum number of allowed users. Cannot invite new (not-yet-registered) users at this time.");
              }
          }

        OrganizationInvite_Data invite = OrganizationInvite_Factory.create(organizationRefnum, U.getRefnum(), U.getId(), inviteeEmail, inviteeNameFirst, inviteeNameLast, role);
        if (existingUser == true)
          {
            invite.setInviteeRefnum(target.getRefnum());
            invite.setInviteeUserId(target.getId());
            invite.setInviteToken(EncryptionUtil.getToken(24, true));
            invite.setInviteTokenCreatedNow();
          }

        if (invite.write(C) == false)
          throw new Exception("Cannot save organization invite for '" + inviteeEmail + "'.");

        if (existingUser == true)
          {
            LOG.debug("Sending existing-user org invite email to '" + inviteeEmail + "' for organization '" + org.getTitle() + "'.");
            invite.sendOrgInviteEmailExistingUser(org.getTitle());
          }
        else
          {
            LOG.debug("Creating new user and sending registration/org-invite email to '" + inviteeEmail + "' for organization '" + org.getTitle() + "'.");
            User_Data.inviteUserForOrg(C, inviteeEmail, org.getTitle(), ownerPromoCode, inviteeNameFirst, inviteeNameLast);
          }

        Res.successJson("", invite);
      }
  }
