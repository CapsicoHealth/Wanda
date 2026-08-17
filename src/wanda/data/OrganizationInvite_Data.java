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

package wanda.data;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;
import tilda.utils.TextUtil;
import wanda.web.EMailSender;
import wanda.web.config.Wanda;

/**
This is the application class <B>Data_OrganizationInvite</B> mapped to the table <B>WANDA.OrganizationInvite</B>.
@see wanda.data._Tilda.TILDA__ORGANIZATIONINVITE
*/
public class OrganizationInvite_Data extends wanda.data._Tilda.TILDA__ORGANIZATIONINVITE
 {
   protected static final Logger LOG = LogManager.getLogger(OrganizationInvite_Data.class.getName());

   public OrganizationInvite_Data() { }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//   Implement your customizations, if any, below.
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


   @Override
   protected boolean beforeWrite(Connection C) throws Exception
     {
       // Do things before writing the object to disk, for example, take care of AUTO fields.
       return true;
     }

   @Override
   protected boolean afterRead(Connection C) throws Exception
     {
       // Do things after an object has just been read form the data store, for example, take care of AUTO fields.
       return true;
     }

   /**
    * Returns true if this invite has a token (Scenario A - existing user) and that token has aged past the
    * configured TTL (see {@link Wanda#getOrgInviteTokenTTLDays()}). An invite without a token creation date is
    * always considered expired.
    *
    * @return
    */
   public boolean isTokenExpired()
     {
       if (isNullInviteTokenCreated() == true)
         return true;
       return ChronoUnit.DAYS.between(getInviteTokenCreated(), ZonedDateTime.now()) >= Wanda.getOrgInviteTokenTTLDays();
     }

   /**
    * Validates a token supplied by a caller (e.g., an Accept/Decline servlet) against this invite: the token must
    * match, the invite must still be Pending, and the token must not have expired.
    *
    * @param token
    * @return
    */
   public boolean isTokenValid(String token)
     {
       if (token == null || isNullInviteToken() == true || token.equals(getInviteToken()) == false)
         {
           LOG.error("Invalid invite token supplied for OrganizationInvite " + getRefnum() + ".");
           return false;
         }
       if (_statusPending.equals(getStatus()) == false)
         {
           LOG.error("OrganizationInvite " + getRefnum() + " is not pending (status=" + getStatus() + ").");
           return false;
         }
       if (isTokenExpired() == true)
         {
           LOG.error("Invite token for OrganizationInvite " + getRefnum() + " has expired.");
           return false;
         }
       return true;
     }

   /**
    * Sends the token-based invitation email for Scenario A (existing, already-registered user), asynchronously via
    * a background thread. The email contains links back to the app for accepting or declining the invitation.
    *
    * @param orgTitle the display title of the organization, substituted into the configured copy texts.
    */
   public void sendOrgInviteEmailExistingUser(String orgTitle)
     {
       String[] to = { getInviteeEmail() }, cc = {}, bcc = {};
       String inviterId = getInviterId();
       String token = getInviteToken();
       long refnum = getRefnum();
       ZonedDateTime expiry = (isNullInviteTokenCreated() == false ? getInviteTokenCreated() : ZonedDateTime.now()).plusDays(Wanda.getOrgInviteTokenTTLDays());
       new Thread()
         {
           @Override
           public void run()
             {
               super.run();
               try
                 {
                   StringBuilder sb = new StringBuilder();
                   List<String> copyTexts = Wanda.getOrgInviteUserTexts();
                   if (copyTexts != null)
                     for (String t : copyTexts)
                       sb.append(t.replace("%%ORG_TITLE%%", orgTitle).replace("%%INVITER_ID%%", TextUtil.print(inviterId, "")));

                   sb.append("<p>This invitation will expire on <b>" + expiry.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm a zzz")) + "</b>.</p>");
                   String baseUrl = Wanda.getHostName() + Wanda.getAppPath() + Wanda.getHomePagePath();
                   String acceptUrl = baseUrl + "?action=orgInviteAccept&token=" + token;
                   String declineUrl = baseUrl + "?action=orgInviteDecline&token=" + token;
                   sb.append("<p><a href='" + acceptUrl + "'>Accept invitation</a>&nbsp;&nbsp;|&nbsp;&nbsp;<a href='" + declineUrl + "'>Decline invitation</a></p>");
                   LOG.debug("Sending org invite email (existing user) for invite " + refnum + " to " + to[0] + " for organization '" + orgTitle + "'.");
                   EMailSender.sendMailUsr(to, cc, bcc, "You have been invited to join " + orgTitle + " -- " + Wanda.getAppName(), sb.toString(), true, true);
                 }
               catch (Throwable T)
                 {
                   LOG.error("Failed sending org invite email for invite " + refnum + " to '" + to[0] + "'.\n", T);
                 }
             }
         }.start();
     }

 }
