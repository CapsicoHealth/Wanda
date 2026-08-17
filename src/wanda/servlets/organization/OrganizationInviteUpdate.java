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
import wanda.data.OrganizationACL_Data;
import wanda.data.OrganizationACL_Factory;
import wanda.data.OrganizationInvite_Data;
import wanda.data.OrganizationInvite_Factory;
import wanda.data.OrganizationRoleView_Factory;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.exceptions.BadRequestException;
import wanda.web.exceptions.NotFoundException;
import wanda.web.exceptions.ResourceNotAuthorizedException;

/**
 * Single consolidated endpoint replacing the previously separate AcceptOrgInvite/DeclineOrgInvite/CancelOrgInvite
 * servlets, for the 3 ways a pending Organization invitation can be resolved by a human action (as opposed to
 * automatically, e.g. expiry, or auto-accept on new-user onboarding -- see UserOnBoarding). Which action is
 * performed is picked via the required "action" parameter (one of {@value #ACTION_ACCEPT}, {@value #ACTION_DECLINE},
 * {@value #ACTION_CANCEL}); the identifier used to look up the invite, and the access-control rule enforced, differ
 * per action since they represent two different actors:<BR>
 * <UL>
 * <LI><B>accept</B> / <B>decline</B> (Scenario A -- existing, already-registered user): looked up by the one-time
 * "token" parameter (see {@link OrganizationInvite_Data#isTokenValid(String)}); the caller must be authenticated
 * and must be the invitee the token was issued for.</LI>
 * <LI><B>cancel</B>: looked up by the invite's "refnum" parameter; the caller must be an Admin/Owner of the
 * Organization the invite belongs to (see {@link OrganizationRoleView_Factory#checkOrganizationAcl}). Cancelling
 * does not affect any User account that may have already been created for a Scenario B (new user) invite -- it
 * merely prevents that invite from being auto-accepted, and prevents a Scenario A token from being used.</LI>
 * </UL>
 */
@WebServlet("/svc/wanda/organizations/invite/update")
public class OrganizationInviteUpdate extends SimpleServlet
  {
    private static final long     serialVersionUID = 1L;
    protected static final Logger LOG              = LogManager.getLogger(OrganizationInviteUpdate.class.getName());

    public static final String    ACTION_ACCEPT    = "accept";
    public static final String    ACTION_DECLINE   = "decline";
    public static final String    ACTION_CANCEL    = "cancel";

    public OrganizationInviteUpdate()
      {
        super(true, true, false);
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        String action = Req.getParamString("action", true);
        if (action != null
            && ACTION_ACCEPT.equalsIgnoreCase(action) == false
            && ACTION_DECLINE.equalsIgnoreCase(action) == false
            && ACTION_CANCEL.equalsIgnoreCase(action) == false)
          Req.addError("action", "Invalid action specified: allowed are '" + ACTION_ACCEPT + "', '" + ACTION_DECLINE + "', '" + ACTION_CANCEL + "'.");
        Req.throwIfErrors();

        if (ACTION_CANCEL.equalsIgnoreCase(action) == true)
          cancel(Req, Res, C, U);
        else
          acceptOrDecline(Req, Res, C, U, ACTION_ACCEPT.equalsIgnoreCase(action));
      }

    /**
     * Accepts or declines a token-based Organization invitation (Scenario A - existing, already-registered user).
     * The caller must be authenticated, and the authenticated user must be the invitee the token was issued for.
     * On acceptance, an OrganizationACL entry is created (or reactivated) for the user with the role that was
     * specified at invite time. Either way, the invite is marked Accepted/Declined so the token can no longer be
     * used.
     */
    private static void acceptOrDecline(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U, boolean accept)
    throws Exception
      {
        String token = Req.getParamString("token", true);
        Req.throwIfErrors();

        OrganizationInvite_Data invite = OrganizationInvite_Factory.lookupByInviteToken(token);
        if (invite.read(C) == false)
          throw new NotFoundException("OrganizationInvite", token, "This invitation cannot be found.");

        if (invite.isTokenValid(token) == false)
          throw new BadRequestException("token", "This invitation is invalid, expired, or has already been used.");

        if (invite.isNullInviteeRefnum() == true || invite.getInviteeRefnum() != U.getRefnum())
          throw new ResourceNotAuthorizedException("OrganizationInvite", "" + invite.getRefnum(), "This invitation was not issued to the currently logged in user.");

        if (accept == false)
          {
            invite.setStatusDeclined();
            if (invite.write(C) == false)
              throw new Exception("Cannot mark OrganizationInvite " + invite.getRefnum() + " as declined.");
            Res.successJson("", invite);
            return;
          }

        long organizationRefnum = invite.getOrganizationRefnum();

        OrganizationACL_Data acl = OrganizationACL_Factory.create(organizationRefnum, U.getRefnum(), U.getId(), invite.getRole(), invite.getInviterRefnum(), invite.getInviterId());
        acl.setNullDeleted(); // clear deleted in case of upsert (e.g., re-joining after having left).
        if (acl.upsert(C) == false)
          throw new Exception("Database error: cannot save ACL entry for organization " + organizationRefnum + " / user " + U.getRefnum() + ".");

        invite.setStatusAccepted();
        if (invite.write(C) == false)
          throw new Exception("Cannot mark OrganizationInvite " + invite.getRefnum() + " as accepted.");

        // Force a re-lookup of this user's role for this organization on their next access check.
        OrganizationRoleView_Factory.evict(organizationRefnum, U.getRefnum());

        Res.successJson("", acl);
      }

    /**
     * Cancels a still-pending Organization invitation. Only an org Admin/Owner may cancel an invite. Cancelling
     * does not affect any User account that may have already been created for a Scenario B (new user) invite -- it
     * merely prevents that invite from being auto-accepted, and prevents a Scenario A token from being used.
     */
    private static void cancel(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        long refnum = Req.getParamLong("refnum", true);
        Req.throwIfErrors();

        OrganizationInvite_Data invite = OrganizationInvite_Factory.lookupByPrimaryKey(refnum);
        if (invite.read(C) == false)
          throw new NotFoundException("OrganizationInvite", "" + refnum, "OrganizationInvite " + refnum + " cannot be found.");

        OrganizationRoleView_Factory.checkOrganizationAcl(C, U, invite.getOrganizationRefnum(), OrganizationRoleView_Factory.OrganizationRole.ADMIN);

        if (OrganizationInvite_Data._statusPending.equals(invite.getStatus()) == false)
          throw new BadRequestException("refnum", "This invitation is no longer pending (status=" + invite.getStatus() + ") and cannot be cancelled.");

        invite.setStatusCancelled();
        if (invite.write(C) == false)
          throw new Exception("Cannot mark OrganizationInvite " + refnum + " as cancelled.");

        Res.successJson("", invite);
      }
  }
