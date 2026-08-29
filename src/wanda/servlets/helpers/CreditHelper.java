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
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import tilda.db.Connection;
import tilda.db.ListResults;
import tilda.utils.SystemValues;
import tilda.utils.json.JSONPrinter;
import wanda.data.Project_Data;
import wanda.data.Project_Factory;
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
     * A read-only, cache-friendly snapshot of a wallet's balance for a widget/UI poll (e.g. the credit-meter
     * gauge). Deliberately a separate, smaller shape than {@link CreditStatus}: this is never used to gate or
     * mutate anything, only to paint a number on screen, so it is safe to serve slightly stale (see
     * {@link #getSnapshot}).
     */
    public static class CreditSnapshot
      {
        protected CreditSnapshot(String productId, boolean hasWallet, BigDecimal balance, BigDecimal creditsPurchased, BigDecimal lastTopUpAmount)
          {
            _productId = productId;
            _hasWallet = hasWallet;
            _balance = balance;
            _creditsPurchased = creditsPurchased;
            _lastTopUpAmount = lastTopUpAmount;
          }

        public final String     _productId;
        public final boolean    _hasWallet;
        public final BigDecimal _balance;
        public final BigDecimal _creditsPurchased;

        /**
         * The wallet's balance right after its MOST RECENT top-up (order) finished -- i.e. any credits left over
         * from before the top-up PLUS what it added -- see
         * {@link CreditHelper#getLastTopUpAmount(Connection, UserPlanSubscription_Data)}. ZERO when the wallet
         * has never received one (or has no wallet at all), in which case a gauge should fall back to a fixed
         * scale instead of trying to use this as "100%".
         */
        public final BigDecimal _lastTopUpAmount;
      }

    /**
     * Bounded, thread-safe, short-TTL cache of {@link CreditSnapshot}s keyed by "userRefnum|productId", so a
     * credit-meter widget polling every few seconds (potentially from several open tabs/products) does not hit
     * the database on every poll. Deliberately used ONLY by {@link #getSnapshot}, which is a display-only,
     * read-only path -- every balance-affecting entry point ({@link #check}, {@link #charge}, {@link #consume},
     * {@link #grant}, {@link #adjust}) reads straight from the database as before and additionally invalidates
     * this cache (see {@link #post}), so gating/mutation decisions can never be made against a stale value.
     * <P>
     * Backed by Guava's {@link Cache} -- the same pattern already used elsewhere in this codebase (e.g.
     * {@code TrialTrackedDetails._TRIAL_SUMMARY_CACHE}, {@code CohortDefinition_Data._JOURNEY_PATHWAY_PATIENTS_CACHE})
     * -- rather than a hand-rolled {@code LinkedHashMap} + lock: {@code maximumSize} gives bounded, evicted
     * storage and {@code expireAfterWrite} gives the TTL, both handled internally (including thread-safety) by
     * Guava, so there is no manual locking or LRU bookkeeping to get right here.
     * <P>
     * {@link #CACHE_TTL_MS} is deliberately generous (minutes, not seconds): {@link #post} invalidates a
     * wallet's entry synchronously on every grant/charge/consume/adjust, so within a single JVM the cache is
     * NEVER stale between a mutation and the next {@link #getSnapshot} call, no matter how long the TTL is --
     * the TTL only exists as a safety net for the one case invalidation can't cover: a clustered deployment with
     * more than one JVM (this cache is plain in-process memory, not shared/distributed), where a mutation
     * handled by one instance doesn't invalidate another instance's copy. If/when this runs behind a
     * non-sticky load balancer across multiple instances, either lower this back down or move the cache to a
     * shared store; for a single instance (or sticky sessions), a long TTL costs nothing and just means more
     * cache hits.
     */
    private static final int  CACHE_MAX_ENTRIES = 50;
    private static final long CACHE_TTL_MS      = 10 * 60_000L; // 10 minutes

    private static final Cache<String, CreditSnapshot> _cache = CacheBuilder.newBuilder()
    .maximumSize(CACHE_MAX_ENTRIES)
    .expireAfterWrite(CACHE_TTL_MS, TimeUnit.MILLISECONDS)
    .build();

    private static String cacheKey(long userRefnum, String paymentSystemProductId)
      {
        return userRefnum + "|" + paymentSystemProductId;
      }

    /**
     * The signed-in user's wallet snapshot for a product, for a display-only widget (e.g. a credit-meter gauge).
     * Served from a small in-memory cache (see {@link #_cache}) when a fresh-enough entry exists, so a widget
     * that polls every few seconds does not put repeated load on the database. On a miss (or an expired entry),
     * reads through to {@link #getWallet} and caches the result.
     * <P>
     * Within a single JVM this is always exactly as fresh as the last mutation: {@link #post} invalidates the
     * entry synchronously on every grant/charge/consume/adjust, regardless of {@link #CACHE_TTL_MS}'s length.
     * The TTL is only a safety net against a clustered/multi-instance deployment -- see {@link #_cache}'s docs.
     * <P>
     * <B>Do not use this for gating or mutating a purchase/consumption decision</B> -- use {@link #check} /
     * {@link #consume} / {@link #charge} instead, which always read the database directly and never touch this
     * cache.
     */
    public static CreditSnapshot getSnapshot(Connection C, User_Data U, String paymentSystemProductId)
    throws Exception
      {
        String key = cacheKey(U.getRefnum(), paymentSystemProductId);
        CreditSnapshot cached = _cache.getIfPresent(key);
        if (cached != null)
          return cached;

        UserPlanSubscription_Data UPS = getWallet(C, U, paymentSystemProductId);
        CreditSnapshot snapshot = new CreditSnapshot(paymentSystemProductId, UPS != null, getBalance(UPS),
                                                      UPS == null || UPS.isNullCreditsPurchased() == true ? BigDecimal.ZERO : UPS.getCreditsPurchased(),
                                                      getLastTopUpAmount(C, UPS));
        _cache.put(key, snapshot);
        return snapshot;
      }

    /**
     * Drops any cached {@link CreditSnapshot} for this wallet, so the very next {@link #getSnapshot} call after a
     * mutation reads the fresh value rather than potentially serving a stale one for up to {@link #CACHE_TTL_MS}
     * more milliseconds. Called from {@link #post}, the single choke point every balance mutation goes through.
     */
    private static void invalidateSnapshot(long userRefnum, String paymentSystemProductId)
      {
        _cache.invalidate(cacheKey(userRefnum, paymentSystemProductId));
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
     * <P>
     * This overload records no organization/project attribution: use it only for a charge that genuinely has no
     * container context (e.g. a standalone action run outside any project). Everything that DOES run within a
     * project should call
     * {@link #charge(Connection, User_Data, String, BigDecimal, long, long, String, String, String)} instead, so
     * per-project/per-org spend reporting can see it.
     * 
     * @param credits a POSITIVE number of credit units to debit
     * @param itemType the filterable TYPE-OF-COST bucket, e.g. "Agent"/"Flow"/"Document". Free-form on purpose:
     *        apps exist outside Wanda, so Wanda can never exhaustively enumerate asset types.
     * @param itemId the charged item's own id (e.g. a flow/agent/document UUID), for per-item roll-ups. The app
     *        picks whatever key it uses itself -- Wanda never interprets it.
     * @param itemLabel a human-readable label for the item (e.g. its title), for display/drill-down only.
     * @return the status AFTER the charge. When {@link CreditStatus#isOK} is false, the app should return its
     *         results to the client <B>and</B> the {@link #CODE_INSUFFICIENT_CREDITS} code alongside them.
     */
    public static CreditStatus charge(Connection C, User_Data U, String paymentSystemProductId, BigDecimal credits, String itemType, String itemId, String itemLabel)
    throws Exception
      {
        return charge(C, U, paymentSystemProductId, credits, SystemValues.EVIL_VALUE, SystemValues.EVIL_VALUE, itemType, itemId, itemLabel);
      }

    /**
     * Same as {@link #charge(Connection, User_Data, String, BigDecimal, String, String, String)}, but also
     * attributes the charge to an organization and/or a project, which is what makes per-org and per-project
     * spend reporting possible (see UserPlanCreditLedgerMinimal*View).
     * <P>
     * Both refnums are optional: pass {@link SystemValues#EVIL_VALUE} for either one that does not apply, and
     * the corresponding ledger column is simply left null.
     * 
     * @param organizationRefnum the owning organization, or {@link SystemValues#EVIL_VALUE} when there is none.
     * @param projectRefnum the owning project, or {@link SystemValues#EVIL_VALUE} when the work is not being
     *        done within a project.
     */
    public static CreditStatus charge(Connection C, User_Data U, String paymentSystemProductId, BigDecimal credits, long organizationRefnum, long projectRefnum, String itemType, String itemId, String itemLabel)
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
          post(C, UPS, UserPlanCreditLedger_Data._typeUse, credits.negate(), organizationRefnum, projectRefnum, itemType, itemId, itemLabel, false);

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
     * How many ledger rows to scan, per page, while walking backwards from "now" to find the last top-up's own
     * GRANT/BONUS rows (see {@link #getLastTopUpAmount}) -- generous since a heavy user can rack up many USE
     * rows between purchases, but still bounded via {@link #_TOPUP_SCAN_MAX_ROWS} so a wallet with an unusually
     * long USE-only history can never turn this into an unbounded scan.
     */
    private static final int _TOPUP_SCAN_PAGE     = 200;
    private static final int _TOPUP_SCAN_MAX_ROWS = 2000;

    /**
     * The wallet's balance right after its MOST RECENT top-up (order) finished posting -- i.e. the
     * {@code balanceAfter} snapshot of the last of its GRANT row (the pack itself) and, when that purchase was
     * also the wallet's first ever, the BONUS row (the promo signup bonus granted alongside it -- see
     * {@link #grant}/{@link #grantSignupBonusIfEligible}) posted for that order.
     * <P>
     * This is deliberately the resulting BALANCE, not merely the sum of the GRANT/BONUS deltas: any credits the
     * wallet still had left over from BEFORE the top-up are part of "what the user has to spend until the next
     * top-up", so they belong in the 100% ceiling too. E.g. a user sitting at 100 who buys a 1,000-credit pack
     * ends the top-up at 1,100 -- that is the new "100%", not 1,000. This is what a credit-meter gauge should
     * treat as "100%", so e.g. a Starter pack (0 + 1,000 + 500 bonus) reads "1,409 / 1,500" after spending some
     * of it, rather than the current balance always being its own 100% ceiling (which reads as "N / N" no matter
     * how much has actually been spent).
     * <P>
     * Walks the WORM ledger (see {@link UserPlanCreditLedger_Factory#lookupWhereSubscription}, already ordered
     * {@code created desc}) backwards from "now": skips the trailing run of USE/ADJ rows (spending since the
     * last top-up), then identifies the contiguous run of GRANT/BONUS rows immediately behind them that share
     * the SAME {@code billingRefnum} (the same order) -- exactly the last top-up's own rows, since GRANT and
     * BONUS are only ever posted together, back-to-back, for the same order, and never interleaved with USE/ADJ
     * rows belonging to a different order. Because the ledger is walked most-recent-first, the VERY FIRST row of
     * that run encountered (the BONUS row when one was posted, else the GRANT row) is chronologically the LAST
     * one posted for that order, so its {@code balanceAfter} already reflects the whole top-up (plus whatever
     * was left over before it) -- that single snapshot is the answer; no summing needed.
     * <P>
     * Returns {@link BigDecimal#ZERO} for a null wallet, or for a wallet that has (implausibly) never received a
     * top-up, so callers can treat "unknown" and "zero" identically and fall back to a fixed-tier scale instead.
     */
    public static BigDecimal getLastTopUpAmount(Connection C, UserPlanSubscription_Data UPS)
    throws Exception
      {
        if (UPS == null)
          return BigDecimal.ZERO;

        BigDecimal topUpBalanceAfter = null;
        Long topUpBillingRefnum = null;
        boolean topUpFound = false;
        int start = 0;
        int scanned = 0;
        while (scanned < _TOPUP_SCAN_MAX_ROWS)
          {
            ListResults<UserPlanCreditLedger_Data> page = UserPlanCreditLedger_Factory.lookupWhereSubscription(C, UPS.getRefnum(), start, _TOPUP_SCAN_PAGE);
            if (page == null || page.isEmpty() == true)
              break;

            for (UserPlanCreditLedger_Data L : page)
              {
                ++scanned;
                boolean isTopUpRow = L.isTypeGrant() == true || L.isTypeBonus() == true;
                Long rowBillingRefnum = L.isNullBillingRefnum() == true ? null : Long.valueOf(L.getBillingRefnum());

                if (topUpFound == false)
                  {
                    if (isTopUpRow == false)
                      continue; // still walking back through USE/ADJ rows spent since the last top-up
                    topUpFound = true;
                    topUpBillingRefnum = rowBillingRefnum;
                    topUpBalanceAfter = L.getBalanceAfter(); // most-recent row of the group: the final ceiling.
                  }
                else if (isTopUpRow == false || java.util.Objects.equals(rowBillingRefnum, topUpBillingRefnum) == false)
                  return topUpBalanceAfter; // walked past the last top-up's own rows: done.
              }

            if (page.size() < _TOPUP_SCAN_PAGE)
              break;
            start += _TOPUP_SCAN_PAGE;
          }
        return topUpBalanceAfter == null ? BigDecimal.ZERO : topUpBalanceAfter;
      }

    /**
     * Convenience overload of {@link #getLastTopUpAmount(Connection, UserPlanSubscription_Data)} that looks the
     * wallet up first; returns ZERO when the user has no wallet at all for the product.
     */
    public static BigDecimal getLastTopUpAmount(Connection C, User_Data U, String paymentSystemProductId)
    throws Exception
      {
        return getLastTopUpAmount(C, getWallet(C, U, paymentSystemProductId));
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

        UserPlanCreditLedger_Data L = post(C, UPS, UserPlanCreditLedger_Data._typeGrant, credits, SystemValues.EVIL_VALUE, SystemValues.EVIL_VALUE, "order:" + UPB.getOrderId(), null, null, false);
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

        UserPlanCreditLedger_Data L = post(C, UPS, UserPlanCreditLedger_Data._typeBonus, bonusCredits, SystemValues.EVIL_VALUE, SystemValues.EVIL_VALUE, "signup-bonus:order:" + UPB.getOrderId(), null, null, false);
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
     * @param organizationRefnum the owning organization, or {@link SystemValues#EVIL_VALUE} when there is none.
     * @param projectRefnum the owning project, or {@link SystemValues#EVIL_VALUE} when there is none.
     * @param itemType the filterable TYPE-OF-COST bucket (free-form -- see {@link #charge}).
     * @param itemId the consumed item's own id, or null.
     * @param itemLabel a human-readable label for the item, or null.
     * @return false, having written nothing, if the user has no wallet or an insufficient balance.
     */
    public static boolean consume(Connection C, User_Data U, String paymentSystemProductId, BigDecimal credits, long organizationRefnum, long projectRefnum, String itemType, String itemId, String itemLabel)
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
        return post(C, UPS, UserPlanCreditLedger_Data._typeUse, credits.negate(), organizationRefnum, projectRefnum, itemType, itemId, itemLabel, true) != null;
      }

    /**
     * Administrative correction. The amount is signed: positive to credit the user, negative to debit them.
     * Applies unconditionally (no floor guard) — an admin override is expected to be able to push a balance
     * negative deliberately, e.g. clawing back a refunded grant that has already been partly spent.
     * <P>
     * Admin corrections carry no organization/project attribution: they are a wallet-level financial action, not
     * metered work done inside a project, so they deliberately stay out of per-project spend reporting.
     */
    public static UserPlanCreditLedger_Data adjust(Connection C, UserPlanSubscription_Data UPS, BigDecimal amount, String itemType, String itemLabel)
    throws Exception
      {
        if (amount == null || amount.signum() == 0)
          throw new Exception("Cannot post a null or zero adjustment to subscription " + UPS.getRefnum() + ".");
        return post(C, UPS, UserPlanCreditLedger_Data._typeAdjustment, amount, SystemValues.EVIL_VALUE, SystemValues.EVIL_VALUE, itemType, null, itemLabel, false);
      }

    /**
     * The one place where a balance ever changes: atomically applies the (signed) movement to the wallet's
     * balance at the database level, re-reads the now-authoritative balance, and appends a ledger row that
     * snapshots it. Everything else in this class routes through here so the ledger and the cache can never
     * drift apart, and so every mutation gets the same concurrency-safe treatment.
     * 
     * @param organizationRefnum the owning organization, or {@link SystemValues#EVIL_VALUE} to leave it null.
     * @param projectRefnum the owning project, or {@link SystemValues#EVIL_VALUE} to leave it null.
     * @param floorGuard when true, the debit is refused (this returns null, having written nothing) if it would
     *        drive the balance negative. Pass true only for {@link #consume}'s strict pre-authorization; every
     *        other caller applies unconditionally.
     * @return the ledger entry, or null if floorGuard blocked the update.
     */
    private static UserPlanCreditLedger_Data post(Connection C, UserPlanSubscription_Data UPS, String type, BigDecimal signedAmount, long organizationRefnum, long projectRefnum, String itemType, String itemId, String itemLabel, boolean floorGuard)
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
        // EVIL_VALUE is the caller-side "not applicable" sentinel for these optional FKs, mirroring how Tilda's
        // own generated request parsers treat it: anything else is a real refnum and gets recorded.
        if (projectRefnum != SystemValues.EVIL_VALUE)
          {
            L.setProjectRefnum(projectRefnum);
            // A project belongs to exactly one organization, so callers only ever need to supply the project:
            // deriving the org here (rather than making every charge site pass both, or making the reporting
            // views join back through Project) is what keeps the ORG-level report from silently coming back
            // empty just because a client only knew about its own project. Denormalizing it onto the WORM
            // ledger row is deliberate: the row records the attribution AS OF the charge, so later moving a
            // project between orgs can never retroactively rewrite historical spend.
            if (organizationRefnum == SystemValues.EVIL_VALUE)
              {
                Project_Data P = Project_Factory.lookupByPrimaryKey(projectRefnum);
                // Best-effort only: a project that cannot be read (deleted/never existed), or one that isn't
                // attached to an organization at all (the generated getter reports that as 0), simply leaves
                // the org null -- an unattributed-at-org-level charge, exactly as before. Never a reason to
                // fail the charge itself, which is real work the user already consumed.
                if (P.read(C) == true && P.getOrganizationRefnum() > 0)
                  organizationRefnum = P.getOrganizationRefnum();
              }
          }
        if (organizationRefnum != SystemValues.EVIL_VALUE)
          L.setOrganizationRefnum(organizationRefnum);
        if (itemType != null)
          L.setItemType(itemType);
        if (itemId != null)
          L.setItemId(itemId);
        if (itemLabel != null)
          L.setItemLabel(itemLabel);
        if (L.write(C) == false)
          throw new Exception("Cannot write the credit ledger entry for subscription " + UPS.getRefnum() + ".");

        // Any cached CreditSnapshot for this wallet (see getSnapshot) is now stale: drop it so the widget's
        // next poll reads the fresh balance instead of waiting out the TTL.
        invalidateSnapshot(UPS.getUserRefnum(), UPS.getPaymentSystemProductId());

        return L;
      }
  }
