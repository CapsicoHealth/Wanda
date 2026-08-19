/* ===========================================================================
 * Copyright (C) 2025 CapsicoHealth Inc.
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

import java.math.BigDecimal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.*;

/**
This is the application class <B>Data_UserPlanSubscription</B> mapped to the table <B>WANDA.UserPlanSubscription</B>.
@see wanda.data._Tilda.TILDA__USERPLANSUBSCRIPTION
*/
public class UserPlanSubscription_Factory extends wanda.data._Tilda.TILDA__USERPLANSUBSCRIPTION_Factory
 {
   protected static final Logger LOG = LogManager.getLogger(UserPlanSubscription_Factory.class.getName());

   protected UserPlanSubscription_Factory() { }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//   Implement your customizations, if any, below.
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


   public static void init(Connection C) throws Exception
    {
      // Add logic to initialize your object, for example, caching some values, or validating some things.
    }

   /**
    * Atomically applies a signed credit adjustment to a wallet's balance (and, optionally, its lifetime
    * "purchased" counter) at the database level, in a single UPDATE statement, entirely bypassing the
    * traditional getCreditsBalance() -&gt; compute in Java -&gt; setCreditsBalance()+write() chain.
    * <P>
    * This is the fix for the read-modify-write race documented in CreditHelper/payment-documentation.md
    * §4: because "creditsBalance = creditsBalance + delta" is expressed as SQL arithmetic evaluated
    * against whatever value the row holds AT THE MOMENT the UPDATE actually runs (under the database's
    * own row lock), two concurrent calls for the same wallet can never silently overwrite one another the
    * way two independent Java-side read-then-write sequences could: the database serializes them and each
    * one's arithmetic is applied on top of the other's already-committed result.
    * 
    * @param balanceDelta the SIGNED number of credit units to apply to the balance (positive to grant,
    *        negative to debit). Never null or zero.
    * @param purchasedDelta optional: when non-null and non-zero, also atomically increments the lifetime
    *        creditsPurchased counter by this amount in the SAME statement (only meaningful for grants;
    *        pass null for a debit/adjustment that shouldn't touch it).
    * @param floorGuard when true, the update is conditioned on the resulting balance remaining &gt;= 0
    *        (i.e., it silently applies to zero rows rather than let the balance go negative) — this is
    *        what a pre-authorized, strict debit (CreditHelper.consume) needs. When false, the update
    *        always applies unconditionally, which is what a post-hoc charge (CreditHelper.charge) needs:
    *        the cost of work already performed must always be recorded, even if it overdraws the wallet.
    * @return true if exactly one row was updated. False means either the wallet doesn't exist (wrong
    *         refnum), or floorGuard blocked the update because the balance would have gone negative — the
    *         caller cannot distinguish those two cases from the return value alone and should not need to:
    *         both mean "the debit did not happen".
    */
   public static boolean incrementCreditsBalance(Connection C, long subscriptionRefnum, BigDecimal balanceDelta, BigDecimal purchasedDelta, boolean floorGuard)
   throws Exception
    {
      if (balanceDelta == null || balanceDelta.signum() == 0)
        throw new Exception("Cannot increment a credits balance by a null or zero delta.");

      UpdateQuery Q = newUpdateQuery(C);
      Q.setIncrement(COLS.CREDITSBALANCE, balanceDelta);
      if (purchasedDelta != null && purchasedDelta.signum() != 0)
        Q.setIncrement(COLS.CREDITSPURCHASED, purchasedDelta);
      Q.where().equals(COLS.REFNUM, subscriptionRefnum);
      if (floorGuard == true)
        // coalesce(creditsBalance,0) >= -balanceDelta  <=>  coalesce(creditsBalance,0) + balanceDelta >= 0
        Q.and().gte(COLS.CREDITSBALANCE, BigDecimal.ZERO, balanceDelta.negate());

      return Q.execute() == 1;
    }

   /** @see #incrementCreditsBalance(Connection, long, BigDecimal, BigDecimal, boolean) */
   public static boolean incrementCreditsBalance(Connection C, long subscriptionRefnum, BigDecimal balanceDelta, boolean floorGuard)
   throws Exception
    {
      return incrementCreditsBalance(C, subscriptionRefnum, balanceDelta, null, floorGuard);
    }

   /**
    * Atomically bumps the wallet's cumulative signup-bonus-granted counter, in the same single-UPDATE-statement
    * style as {@link #incrementCreditsBalance}. This column is the idempotency guard that makes
    * {@link wanda.data.Promo_Data#getInitialCredits()} a first-purchase-only bonus per wallet: see
    * {@code CreditHelper.grantSignupBonusIfEligible}, the only caller.
    *
    * @param bonusDelta a POSITIVE number of credit units. Never null or <= 0: there is no legitimate reason to
    *        decrement a lifetime-granted counter (a clawback would go through CreditHelper.adjust instead, which
    *        does not touch this column).
    * @return true if exactly one row was updated (false only means the wallet doesn't exist).
    */
   public static boolean incrementCreditsBonusGranted(Connection C, long subscriptionRefnum, BigDecimal bonusDelta)
   throws Exception
    {
      if (bonusDelta == null || bonusDelta.signum() <= 0)
        throw new Exception("Cannot increment a signup-bonus-granted counter by a null or non-positive delta.");

      UpdateQuery Q = newUpdateQuery(C);
      Q.setIncrement(COLS.CREDITSBONUSGRANTED, bonusDelta);
      Q.where().equals(COLS.REFNUM, subscriptionRefnum);
      return Q.execute() == 1;
    }

 }


