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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.*;
import tilda.db.processors.ScalarRP;
import tilda.utils.TextUtil;

/**
This is the application class <B>Data_OrganizationInvite</B> mapped to the table <B>WANDA.OrganizationInvite</B>.
@see wanda.data._Tilda.TILDA__ORGANIZATIONINVITE
*/
public class OrganizationInvite_Factory extends wanda.data._Tilda.TILDA__ORGANIZATIONINVITE_Factory
 {
   protected static final Logger LOG = LogManager.getLogger(OrganizationInvite_Factory.class.getName());

   protected OrganizationInvite_Factory() { }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//   Implement your customizations, if any, below.
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


   public static void init(Connection C) throws Exception
    {
      // Add logic to initialize your object, for example, caching some values, or validating some things.
    }

   /**
    * Looks up all Pending invites for a given email address, across all organizations, where no invitee account
    * has been linked yet (i.e., inviteeRefnum is still null). This is used by the new-user onboarding flow
    * (Scenario B) to auto-join a brand new user to any organization(s) they were invited to before they had an
    * account.
    *
    * @param C
    * @param inviteeEmail
    * @param start
    * @param size
    * @return
    * @throws Exception
    */
   public static ListResults<wanda.data.OrganizationInvite_Data> lookupWhereByInviteeEmailPending(Connection C, String inviteeEmail, int start, int size)
   throws Exception
     {
       SelectQuery Q = newWhereQuery(C);
       Q.equals(COLS.INVITEEEMAIL, inviteeEmail)
        .and().equals(COLS.STATUS, wanda.data.OrganizationInvite_Data._statusPending)
        .and().isNull(COLS.INVITEEREFNUM);
       Q.orderBy(COLS.CREATED, false);
       return runSelect(C, Q, start, size);
     }

   /**
    * Counts pending, not-yet-linked-to-an-account Organization invites (i.e., Scenario B: inviteeRefnum is still
    * null) across the <B>entire platform</B> -- every Organization, every inviter -- whose Organization's owner
    * (creator) is currently bound to the given promo code.<BR>
    * <BR>
    * These invites represent users who don't exist yet but, the moment they complete onboarding, will be created
    * with this exact same promo code (see {@link User_Data#inviteUserForOrg}). They must therefore be counted
    * alongside already-active users (see {@link User_Factory#countUsersByPromoCode}) when enforcing a promo code's
    * maxUsers bound -- otherwise a burst of concurrent invites across multiple Organizations owned by users who
    * happen to share the same promo code could push the total well past the allowed limit before any of them
    * actually register.
    *
    * @param C
    * @param promoCode
    * @return
    * @throws Exception
    */
   public static long countPendingByOwnerPromoCode(Connection C, String promoCode) throws Exception
     {
       ScalarRP rp = new ScalarRP();
       String q = "select count(*) from " + SCHEMA_TABLENAME_LABEL + " oi "
                + "join WANDA.Organization o on o.\"refnum\" = oi.\"organizationRefnum\" and o.\"deleted\" is null "
                + "join WANDA.User u on u.\"refnum\" = o.\"creatorRefnum\" and u.\"deleted\" is null "
                + "where oi.\"status\" = " + TextUtil.escapeSingleQuoteForSQL(wanda.data.OrganizationInvite_Data._statusPending) + " "
                + "and oi.\"inviteeRefnum\" is null and oi.\"deleted\" is null "
                + "and u.\"promoCode\" = " + TextUtil.escapeSingleQuoteForSQL(promoCode);
       C.executeSelect(SCHEMA_LABEL, TABLENAME_LABEL, q, rp);
       return rp.getResult();
     }

 }
