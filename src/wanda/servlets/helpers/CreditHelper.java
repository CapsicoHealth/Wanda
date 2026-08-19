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
package wanda.servlets.helpers;

import java.math.BigDecimal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;
import tilda.utils.json.JSONPrinter;
import wanda.data.UserPlanBilling_Data;
import wanda.data.UserPlanCreditLedger_Data;
import wanda.data.UserPlanCreditLedger_Factory;
import wanda.data.UserPlanSubscription_Data;
import wanda.data.UserPlanSubscription_Factory;
import wanda.data.User_Data;

/**
 * Single entry point for every credit-balance mutation on pre-paid (planType=C) plans.
 * <P>
 * The model is a <B>wallet per user per product</B>: the user's active {@link UserPlanSubscription_Data} for a
 * product holds the running balance, and every movement is appended to the WORM
 * {@link UserPlanCreditLedger_Data}. The ledger is the source of truth; the balance on the subscription is a
 * denormalized cache that, in principle, could be rebuilt at any time with <CODE>SUM(amount)</CODE> over the
 * ledger for that subscription. <B>No reconciliation/rebuild helper exists yet</B> — this is flagged as
 * follow-up work in payment-documentation.md, not implemented here.
 * <P>
 * Because the wallet is keyed by product and not by plan, buying <I>any</I> tier of a product tops up the same
 * wallet: a user with 200 credits left who buys the 5,000-credit pack ends up with 5,200, not 5,000.
 * <P>
 * Credits are <B>units, not money</B> (1 credit = 1 US cent by convention). They are deliberately
 * currency-agnostic: pack prices are native per market, but the credits granted are identical everywhere, so
 * metering never needs to know about currencies.
 * <P>
 * <B>Concurrency:</B> every balance mutation goes through {@link UserPlanSubscription_Factory#incrementCreditsBalance}
 * — a single atomic <CODE>UPDATE ... SET creditsBalance = creditsBalance + ?</CODE> statement — rather than the
 * traditional read (getCreditsBalance()) -&gt; compute in Java -&gt; write (setCreditsBalance()+write()) chain.
 * Because the increment is SQL arithmetic evaluated against whatever the row holds AT THE MOMENT the UPDATE
 * runs (under the database's own row lock), two concurrent calls for the same wallet — e.g. the same user with
 * two browser tabs open — can never silently overwrite one another the way two independent Java-side
 * read-then-write sequences could. See payment-documentation.md §4 for the full write-up of the risk this
 * replaces.
 */
public class CreditHelper
  {
    protected static final Logger LOG = LogManager.getLogger(CreditHelper.class.getName());

    /**
     * The application-level code an app service returns to its own front-end when the user must top up before
     * (or before being allowed to continue after) a metered operation.
     * <P>
     * This is deliberately <B>not</B> an HTTP status: the request itself is perfectly valid and, in the
     * post-operation case, actually succeeded. It travels inside the app's own successful response payload, and
     * the app's front-end reacts to it by opening the top-up popup. It is defined here so every app uses the
     * same constant rather than inventing one each.
     */
    public static final String CODE_INSUFFICIENT_CREDITS = "INSUFFICIENT_CREDITS";

    /**
     * The outcome of a balance check or a charge, in a form an app service can both branch on and hand straight
     * back to its front-end.
     */
    public static class CreditStatus
      {
        protected CreditStatus(String productId, boolean hasWallet, BigDecimal balance)
          {
            _productId = productId;
            _hasWallet = hasWallet;
            _balance = balance;
          }

        public final String     _productId;
        public final boolean    _hasWallet;
        public final BigDecimal _balance;

        /**
         * Whether the user may start (or continue with) metered work.
         * <P>
         * Blocked when they have no wallet at all (they have never bought a pack, so there is nothing to draw
         * from), or when the balance has gone negative. Note a zero balance is NOT blocked: that is what allows
         * an operation whose cost is only known once it completes to overdraw slightly. The user then settles up
         * before the next one.
         */
        public boolean isOK()
          {
            return _hasWallet == true && _balance.signum() >= 0;
          }

        /**
         * The app-level code to forward to the client, or null when nothing is needed. Apps should include this
         * in their response payload whenever it is non-null.
         */
        public String getCode()
          {
            return isOK() == true ? null : CODE_INSUFFICIENT_CREDITS;
          }

        /**
         * Writes this status into an app's response payload under a "creditStatus" element, so every app reports
         * it identically and one piece of client-side code can handle them all.
         */
        public void toJSON(JSONPrinter j)
        throws Exception
          {
            j.addElementStart("creditStatus");
            j.addElement("productId", _productId);
            j.addElement("code", getCode());
            j.addElement("ok", isOK());
            j.addElement("hasWallet", _hasWallet);
            j.addElement("balance", _balance, 0);
            j.addElementClose("creditStatus");
          }
      }

    /**
     * <B>Entry gate.</B> Checks, without writing anything, whether a user may begin metered work for a product.
     * An app service calls this first and, when {@link CreditStatus#isOK} is false, returns
     * {@link #CODE_INSUFFICIENT_CREDITS} to its front-end without doing any work at all.
     */
    public static CreditStatus check(Connection C, User_Data U, String paymentSystemProductId)
    throws Exception
      {
        UserPlanSubscription_Data UPS = getWallet(C, U, paymentSystemProductId);
        return new CreditStatus(paymentSystemProductId, UPS != null, getBalance(UPS));
      }

    /**
     * <B>Post-operation charge.</B> Debits credits for work that has ALREADY been performed, and reports where
     * the balance stands afterwards.
     * <P>
     * Unlike {@link #consume}, this <B>always</B> applies the charge, even when it drives the balance negative.
     * That is the whole point: the cost of an agentic operation is generally not known until it finishes, so
     * refusing to record it would mean giving the work away and losing the audit trail. The user is simply
     * blocked by {@link #check} the next time around, which is what triggers the top-up prompt.
     * 
     * @param credits a POSITIVE number of credit units to debit
     * @return the status AFTER the charge. When {@link CreditStatus#isOK} is false, the app should return its
     *         results to the client <B>and</B> the {@link #CODE_INSUFFICIENT_CREDITS} code alongside them.
     */
    public static CreditStatus charge(Connection C, User_Data U, String paymentSystemProductId, BigDecimal credits, String reference)
    throws Exception
      {
        if (credits == null || credits.signum() < 0)
          throw new Exception("Cannot charge a null or negative number of credits (" + credits + ").");

        UserPlanSubscription_Data UPS = getWallet(C, U, paymentSystemProductId);
        if (UPS == null)
          {
            // No wallet to debit. The work is already done, so this is not an exception, but it must be loud:
            // it means an app charged without gating on check() first.
            LOG.error("User " + U.getRefnum() + " was charged " + credits + " credits for product " + paymentSystemProductId + " but has no wallet. The charge could not be recorded. Did the app skip CreditHelper.check() at entry?");
            return new CreditStatus(paymentSystemProductId, false, BigDecimal.ZERO);
          }

        if (credits.signum() > 0)
          post(C, UPS, UserPlanCreditLedger_Data._typeUse, credits.negate(), reference, null, false);

        return new CreditStatus(paymentSystemProductId, true, getBalance(UPS));
      }

    /**
     * The user's active credit wallet for a product, or null if they have none.
     */
    public static UserPlanSubscription_Data getWallet(Connection C, User_Data U, String paymentSystemProductId)
    throws Exception
      {
        UserPlanSubscription_Data UPS = UserPlanSubscription_Factory.lookupByUserActivePlan(U.getRefnum(), paymentSystemProductId);
        return UPS.read(C) == false ? null : UPS;
      }

    /**
     * The user's current credit balance for a product. Returns ZERO (never null) when the user has no wallet, so
     * callers can treat "no wallet" and "empty wallet" identically.
     */
    public static BigDecimal getBalance(Connection C, User_Data U, String paymentSystemProductId)
    throws Exception
      {
        UserPlanSubscription_Data UPS = getWallet(C, U, paymentSystemProductId);
        return getBalance(UPS);
      }

    public static BigDecimal getBalance(UserPlanSubscription_Data UPS)
      {
        if (UPS == null || UPS.isNullCreditsBalance() == true)
          return BigDecimal.ZERO;
        return UPS.getCreditsBalance();
      }

    /**
     * Whether the user's wallet for this product has already received the one-time promo signup bonus (see
     * {@link #grantSignupBonusIfEligible}). Used to stop advertising {@code Promo.initialCredits} as an
     * available perk once it has been claimed on that wallet -- it is a first-purchase-only grant.
     * <P>
     * Returns false (i.e., "still available") when the user has no wallet yet for this product: a brand-new
     * wallet cannot have received the bonus.
     */
    public static boolean hasReceivedSignupBonus(Connection C, User_Data U, String paymentSystemProductId)
    throws Exception
      {
        UserPlanSubscription_Data UPS = getWallet(C, U, paymentSystemProductId);
        return UPS != null && UPS.isNullCreditsBonusGranted() == false && UPS.getCreditsBonusGranted().signum() > 0;
      }

    /**
     * Grants credits following a successful purchase. Called from the payment capture flow once, and only once,
     * per paid order.
     * 
     * @param UPS the wallet (the user's active subscription for the product)
     * @param UPB the billing record that paid for this grant, for traceability
     * @param credits a POSITIVE number of credit units
     */
    public static UserPlanCreditLedger_Data grant(Connection C, UserPlanSubscription_Data UPS, UserPlanBilling_Data UPB, BigDecimal credits)
    throws Exception
      {
        if (credits == null || credits.signum() <= 0)
          throw new Exception("Cannot grant a null or non-positive number of credits (" + credits + ") to subscription " + UPS.getRefnum() + ".");

        UserPlanCreditLedger_Data L = post(C, UPS, UserPlanCreditLedger_Data._typeGrant, credits, "order:" + UPB.getOrderId(), null, false);
        if (L == null) // floorGuard is false above, so this would only happen if the wallet row vanished concurrently.
          throw new Exception("Failed to grant " + credits + " credits to subscription " + UPS.getRefnum() + ": the wallet row could not be found for the update.");
        L.setBillingRefnum(UPB.getRefnum());
        if (L.write(C) == false)
          throw new Exception("Cannot link credit ledger entry " + L.getRefnum() + " to billing " + UPB.getRefnum() + ".");
        return L;
      }

    /**
     * Grants the one-time promo signup bonus, if any, defined by {@code Promo.initialCredits}, IN ADDITION to a
     * purchase's own pack credits already granted via {@link #grant}. Idempotent per wallet: a wallet whose
     * {@code creditsBonusGranted} is already positive is left untouched, which is what makes this a
     * first-purchase-only bonus per product -- re-buying/topping-up the same wallet does nothing here. Must be
     * called AFTER {@link #grant} for the SAME purchase, so the wallet already exists.
     * <P>
     * Bonus credits are still counted within {@code creditsPurchased} (documented as "lifetime credits ever
     * granted", not "lifetime paid"), so the wallet's reporting total is unaffected by this distinction;
     * {@code creditsBonusGranted} is tracked purely so this method can tell whether it has already fired once.
     *
     * @param UPS the wallet (the user's active subscription for the product), already reflecting the purchase's
     *        own {@link #grant} call.
     * @param UPB the billing record that paid for the purchase that triggered this bonus check, for traceability.
     * @param bonusCredits the promo's configured bonus, or null/zero/negative if the promo defines none: either
     *        case is a silent no-op.
     * @return the BONUS ledger entry, or null if there was nothing to grant (no bonus configured on the promo, or
     *         already granted once on this wallet).
     */
    public static UserPlanCreditLedger_Data grantSignupBonusIfEligible(Connection C, UserPlanSubscription_Data UPS, UserPlanBilling_Data UPB, BigDecimal bonusCredits)
    throws Exception
      {
        if (bonusCredits == null || bonusCredits.signum() <= 0)
          return null; // This promo defines no signup bonus.
        if (UPS.isNullCreditsBonusGranted() == false && UPS.getCreditsBonusGranted().signum() > 0)
          return null; // Already granted once on this wallet: the first-purchase-only guard.

        UserPlanCreditLedger_Data L = post(C, UPS, UserPlanCreditLedger_Data._typeBonus, bonusCredits, "signup-bonus:order:" + UPB.getOrderId(), null, false);
        if (L == null) // post() only returns null when floorGuard blocks it, and floorGuard is false here.
          throw new Exception("Failed to grant a signup bonus of " + bonusCredits + " credits to subscription " + UPS.getRefnum() + ": the wallet row could not be found for the update.");
        L.setBillingRefnum(UPB.getRefnum());
        if (L.write(C) == false)
          throw new Exception("Cannot link the signup-bonus ledger entry " + L.getRefnum() + " to billing " + UPB.getRefnum() + ".");

        if (UserPlanSubscription_Factory.incrementCreditsBonusGranted(C, UPS.getRefnum(), bonusCredits) == false)
          throw new Exception("Cannot record the signup bonus granted on subscription " + UPS.getRefnum() + ".");
        if (UPS.refresh(C) == false)
          throw new Exception("Cannot refresh subscription " + UPS.getRefnum() + " after recording the signup bonus.");

        return L;
      }

    /**
     * <B>Pre-authorized consumption.</B> Deducts credits only if the wallet can cover them in full, and writes
     * nothing at all otherwise. The sufficiency check and the debit are a SINGLE atomic database operation (see
     * {@link UserPlanSubscription_Factory#incrementCreditsBalance}), so this is safe against two concurrent
     * calls for the same wallet in a way a separate "read balance, compare in Java, then write" never could be.
     * <P>
     * Use this only when the cost is known BEFORE the work is done. When the cost is only known afterwards (the
     * usual case for agentic operations), use {@link #check} at entry and {@link #charge} at exit instead.
     * 
     * @param credits a POSITIVE number of credit units to deduct
     * @return false, having written nothing, if the user has no wallet or an insufficient balance.
     */
    public static boolean consume(Connection C, User_Data U, String paymentSystemProductId, BigDecimal credits, String reference)
    throws Exception
      {
        if (credits == null || credits.signum() <= 0)
          throw new Exception("Cannot consume a null or non-positive number of credits (" + credits + ").");

        UserPlanSubscription_Data UPS = getWallet(C, U, paymentSystemProductId);
        if (UPS == null)
          {
            LOG.debug("User " + U.getRefnum() + " has no credit wallet for product " + paymentSystemProductId + ".");
            return false;
          }

        // floorGuard=true: the underlying UPDATE only applies (and returns true) if the resulting balance would
        // remain >= 0. If another request already spent the last of it a moment ago, this simply returns false,
        // even though the in-memory UPS we read a moment ago still shows enough credits.
        return post(C, UPS, UserPlanCreditLedger_Data._typeUse, credits.negate(), reference, null, true) != null;
      }

    /**
     * Administrative correction. The amount is signed: positive to credit the user, negative to debit them.
     * Applies unconditionally (no floor guard) — an admin override is expected to be able to push a balance
     * negative deliberately, e.g. clawing back a refunded grant that has already been partly spent.
     */
    public static UserPlanCreditLedger_Data adjust(Connection C, UserPlanSubscription_Data UPS, BigDecimal amount, String reference, String notes)
    throws Exception
      {
        if (amount == null || amount.signum() == 0)
          throw new Exception("Cannot post a null or zero adjustment to subscription " + UPS.getRefnum() + ".");
        return post(C, UPS, UserPlanCreditLedger_Data._typeAdjustment, amount, reference, notes, false);
      }

    /**
     * The one place where a balance ever changes: atomically applies the (signed) movement to the wallet's
     * balance at the database level, re-reads the now-authoritative balance, and appends a ledger row that
     * snapshots it. Everything else in this class routes through here so the ledger and the cache can never
     * drift apart, and so every mutation gets the same concurrency-safe treatment.
     * 
     * @param floorGuard when true, the debit is refused (this returns null, having written nothing) if it would
     *        drive the balance negative. Pass true only for {@link #consume}'s strict pre-authorization; every
     *        other caller applies unconditionally.
     * @return the ledger entry, or null if floorGuard blocked the update.
     */
    private static UserPlanCreditLedger_Data post(Connection C, UserPlanSubscription_Data UPS, String type, BigDecimal signedAmount, String reference, String notes, boolean floorGuard)
    throws Exception
      {
        BigDecimal purchasedDelta = signedAmount.signum() > 0 ? signedAmount : null;
        if (UserPlanSubscription_Factory.incrementCreditsBalance(C, UPS.getRefnum(), signedAmount, purchasedDelta, floorGuard) == false)
          return null; // floorGuard blocked it: insufficient balance. Nothing was written.

        // The atomic UPDATE above is the only place the balance actually changed. Re-read it fresh rather than
        // computing it in Java, so the ledger snapshot (and the in-memory UPS the caller keeps using) reflect
        // the database's own authoritative value, not a value we merely assumed.
        if (UPS.refresh(C) == false)
          throw new Exception("Cannot refresh subscription " + UPS.getRefnum() + " after a credit balance update.");
        BigDecimal balanceAfter = getBalance(UPS);

        UserPlanCreditLedger_Data L = UserPlanCreditLedger_Factory.create(UPS.getUserRefnum(), UPS.getRefnum(), UPS.getPaymentSystemProductId(), type, signedAmount, balanceAfter);
        if (reference != null)
          L.setReference(reference);
        if (notes != null)
          L.setNotes(notes);
        if (L.write(C) == false)
          throw new Exception("Cannot write the credit ledger entry for subscription " + UPS.getRefnum() + ".");

        return L;
      }
  }
