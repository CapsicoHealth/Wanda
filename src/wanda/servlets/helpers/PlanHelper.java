/* ===========================================================================
 * Copyright (C) 2024 CapsicoHealth Inc.
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

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;
import tilda.utils.DateTimeUtil;
import tilda.utils.SystemValues;
import tilda.utils.json.JSONUtil;
import wanda.data.PlanPricing_Data;
import wanda.data.PlanPricing_Factory;
import wanda.data.Plan_Factory;
import wanda.data.Promo_Data;
import wanda.data.Promo_Factory;
import wanda.data.UserPlanBilling_Data;
import wanda.data.UserPlanBilling_Factory;
import wanda.data.UserPlanSubscription_Data;
import wanda.data.UserPlanSubscription_Factory;
import wanda.data.User_Data;
import wanda.data.importers.promos.Plan;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SessionUtil;

/**
 * Handles the Plan selection process. Typically, it proceeds as follows:
 * <UL>
 * <LI>Check if the user needs to select a plan</LI>
 * <LI>If so, get the plan information and return to the client</LI>
 * <LI>Client displays the plan options and gets the user's selection</LI>
 * <LI>Client sends back the selection which we validate</LI>
 * <LI>We mark the user as having selected a plan</LI>
 * </UL>
 */
public class PlanHelper
  {
    protected static final Logger LOG = LogManager.getLogger(LoginHelper.class.getName());

    protected static boolean doPlan(RequestUtil req, ResponseUtil res, Connection C, User_Data U)
    throws Exception
      {
/*
        long planRefnum = req.getParamLong("planRefnum", false);
        if (planRefnum != SystemValues.EVIL_VALUE)
          {
            LOG.debug("Plan selection submitted: " + planRefnum);
            String planCurrency = req.getParamString("planCurrency", false);
            char planCycle = req.getParamChar("planCycle", false);

            if (UserPlanSubscription_Data.checkCycle(planCycle) == false)
              req.addError("planCycle", "Plan cycle '" + planCycle + "' is invalid.");

            List<PlanPricing_Data> L = PlanPricing_Factory.lookupWherePlanRefnum(C, planRefnum, 0, -1);
            boolean found = false;
            for (PlanPricing_Data P : L)
              if (P.getCurrency().equals(planCurrency) == true)
                {
                  found = true;
                  break;
                }
            if (found == false)
              req.addError("planCurrency", "Plan currency '" + planCurrency + "' is invalid for this plan.");

            req.throwIfErrors();

            UserPlanSubscription_Data UPS = UserPlanSubscription_Factory.lookupByUserActivePlan(U.getRefnum());
            if (UPS.read(C) == true)
              {
                UPS.setActive(false);
                UPS.setEndDt(DateTimeUtil.nowLocalDate());
                if (UPS.write(C) == false)
                  throw new Exception("Cannot update existing plan subscription for user " + U.getRefnum());
              }
            LocalDate start = DateTimeUtil.nowLocalDate();
//            LocalDate end = planCycle == UserPlanSubscription_Data._cycleYearly ? start.plusYears(1) : start.plusMonths(1);
            UPS = UserPlanSubscription_Factory.create(U.getRefnum(), true, planRefnum, planCurrency, planCycle, start);
            if (UPS.write(C) == false)
              throw new Exception("Cannot create plan subscription for user " + U.getRefnum());

            LOG.debug("Plan selection active");
            return true;
          }
*/
        if (needsPlan(C, U) == false)
          {
            LOG.debug("Plan not needed");
            clearUserForPlan(C, req, U, true);
            return true;
          }

        LOG.debug("Plan selection needed");
        PrintWriter out = res.setContentType(ResponseUtil.ContentType.JSON);
        JSONUtil.startOK(out, '{');
        JSONUtil.print(out, "pickPlan", true, true);
        JSONUtil.end(out, '}');
        return false; // force a client-side plan selection.
      }


    protected static boolean needsPlan(Connection C, User_Data U)
    throws Exception
      {
        // Does this user have plans they have to chose from?
        List<Plan> L = getAvailablePlans(C, U);
        if (L == null || L.isEmpty() == true)
          return false;

        // Every purchasable product (planType=S subscriptions AND planType=C credit packs alike -- see below for
        // why credit products no longer get a free pass) that this user's promo names is gated here.
        // Contact-us (planType=X) plans aren't purchasable online at all, so they never gate anything.
        for (String productId : getGatedProductIds(L))
          {
            // If they have an active billing for this product already (a real purchase, OR a prior
            // auto-assignment -- see autoAssignFreePlan below, which creates one too), they don't need to pick.
            UserPlanBilling_Data UPB = UserPlanBilling_Factory.lookupByUserActive(U.getRefnum(), productId);
            if (UPB.read(C) == true)
              continue;

            // Not resolved yet. Rather than force a pick, see if this product has exactly one plan flagged
            // Plan.autoPlan=true (a promo's free/trial credit pack, e.g. AGENTIC_CREDITS_TRIAL): if so, assign it
            // automatically -- no payment, no picker -- so plan-picking is never a blocker to getting started.
            // This is a one-time, idempotent action per product: the UserPlanBilling row it creates is exactly
            // what makes the check above skip this product on every later call.
            Plan auto = getSingleAutoPlan(L, productId);
            if (auto != null)
              {
                autoAssignFreePlan(C, U, auto);
                continue;
              }

            // No existing billing and no auto-assignable plan for this product: a manual pick is required.
            return true;
          }

        // Every gated product (if any) was either already billed or just auto-assigned above: nothing to pick.
        return false;
      }


    /**
     * The distinct product ids across the subscription-type (planType=S) plans in the supplied list. Since a
     * product groups several tiers, this collapses e.g. Individual/Professional/Enterprise down to one entry.
     *
     * @deprecated superseded by {@link #getGatedProductIds}, which also covers credit (planType=C) products now
     *             that {@link #needsPlan} gates on any purchasable product rather than subscriptions only. Kept
     *             for any external caller still relying on the subscription-only grouping.
     */
    @Deprecated
    public static List<String> getSubscriptionProductIds(List<Plan> plans)
      {
        List<String> L = new ArrayList<String>();
        if (plans != null)
          for (Plan p : plans)
            {
              if (p._Plan.isPlanTypeSubscription() == false)
                continue;
              String productId = p._Plan.getPaymentSystemProductId();
              if (L.contains(productId) == false)
                L.add(productId);
            }
        return L;
      }

    /**
     * The distinct product ids across every PURCHASABLE plan type (planType=S subscriptions and planType=C
     * credit packs) in the supplied list -- i.e., every product {@link #needsPlan} must resolve one way or
     * another for the user. Contact-us (planType=X) plans are excluded: they aren't purchasable online, so they
     * never gate a pick.
     */
    public static List<String> getGatedProductIds(List<Plan> plans)
      {
        List<String> L = new ArrayList<String>();
        if (plans != null)
          for (Plan p : plans)
            {
              if (p._Plan.isPlanTypeContactUs() == true)
                continue;
              String productId = p._Plan.getPaymentSystemProductId();
              if (L.contains(productId) == false)
                L.add(productId);
            }
        return L;
      }

    /**
     * The single {@code planType=C} plan flagged {@code autoPlan=true} for the given product within the supplied
     * plan list, or null if there is none. If MORE than one is found, this is treated as a misconfiguration
     * (which one would even be picked is ambiguous) -- a warning is logged and null is returned, which makes
     * {@link #needsPlan} fall back to a manual pick rather than guessing.
     */
    private static Plan getSingleAutoPlan(List<Plan> plans, String productId)
      {
        Plan found = null;
        for (Plan p : plans)
          {
            if (p._Plan.getPaymentSystemProductId().equals(productId) == false || p._Plan.isPlanTypeCredits() == false || p._Plan.getAutoPlan() != true)
              continue;
            if (found != null)
              {
                LOG.warn("Product '" + productId + "' has more than one autoPlan=true plan ('" + found._Plan.getCode() + "' and '" + p._Plan.getCode()
                + "'): auto-assignment is ambiguous, so it will be skipped in favor of a manual plan pick.");
                return null;
              }
            found = p;
          }
        return found;
      }

    /**
     * Grants a {@code Plan.autoPlan=true} credit plan to a user with no payment and no plan-picker prompt --
     * intended for a promo's free/trial tier (e.g. AGENTIC_CREDITS_TRIAL). Mirrors the paid-purchase path in
     * {@code PaymentOrderCapture} (subscription/wallet row, then a PAID+active {@link UserPlanBilling_Data} row,
     * then {@link CreditHelper#grant} and any {@link CreditHelper#grantSignupBonusIfEligible}), minus the
     * PayPal/{@code UserPlanPreOrder} step there is no order to capture. Only ever called once per product from
     * {@link #needsPlan}, which is itself made idempotent by the very {@link UserPlanBilling_Data} row this
     * method creates.
     */
    private static void autoAssignFreePlan(Connection C, User_Data U, Plan plan)
    throws Exception
      {
        String productId = plan._Plan.getPaymentSystemProductId();

        // Credits are currency-agnostic and this plan must be priced at $0 in every currency it defines (see the
        // Plan.autoPlan column description), so which currency we record on the ledger/subscription is purely
        // for bookkeeping consistency with the paid-purchase code path. Prefer USD; fall back to whatever
        // currency the plan actually defines pricing for.
        String currency = "USD";
        BigDecimal credits = getCredits(plan, currency);
        if (credits == null && plan._Pricings != null)
          for (PlanPricing_Data pp : plan._Pricings)
            {
              currency = pp.getCurrency();
              credits = getCredits(plan, currency);
              if (credits != null)
                break;
            }
        if (credits == null || credits.signum() <= 0)
          {
            LOG.error("Plan '" + plan._Plan.getCode() + "' is flagged autoPlan=true but defines no positive oneTimeCredits in any currency: skipping auto-assignment for user " + U.getRefnum() + ".");
            return;
          }

        LocalDate now = DateTimeUtil.nowLocalDate();
        UserPlanSubscription_Data UPS = UserPlanSubscription_Factory.create(U.getRefnum(), true, plan._Plan.getRefnum(), productId, currency, UserPlanSubscription_Data._cycleOneTime, now);
        if (UPS.write(C) == false)
          throw new Exception("Cannot create auto-assigned plan subscription for user " + U.getRefnum() + " and product " + productId + ".");

        UserPlanBilling_Data UPB = UserPlanBilling_Factory.create(UPS.getRefnum(), U.getRefnum(), plan._Plan.getRefnum(), productId, "NONE", "AUTO-" + UPS.getRefnum(), "AUTO-" + UPS.getRefnum(), true);
        UPB.setStatus(UserPlanBilling_Data._statusPaid);
        UPB.setOrderDt(DateTimeUtil.nowUTC());
        UPB.setExpiryDt(UPS.getExpiryDtFrom(now));
        UPB.setTotal(BigDecimal.ZERO);
        UPB.setCurrency(currency);
        UPB.setMessage("Auto-assigned free plan '" + plan._Plan.getCode() + "' at registration (Plan.autoPlan=true): no payment required.");
        if (UPB.write(C) == false)
          throw new Exception("Cannot create auto-assigned plan billing for user " + U.getRefnum() + " and product " + productId + ".");

        CreditHelper.grant(C, UPS, UPB, credits);

        Promo_Data promo = getUserPromo(C, U);
        if (promo != null && promo.isNullInitialCredits() == false)
          CreditHelper.grantSignupBonusIfEligible(C, UPS, UPB, promo.getInitialCredits());

        LOG.info("Auto-assigned free plan '" + plan._Plan.getCode() + "' (" + credits + " credits) to user " + U.getRefnum() + " for product " + productId + ".");
      }


    /**
     * Finds a plan by code in the supplied list of plans available to a user. Returns null if not found, i.e.,
     * if the plan doesn't exist or isn't available to that user.
     */
    public static Plan getPlan(List<Plan> plans, String planCode)
      {
        if (plans != null)
          for (Plan p : plans)
            if (p._Plan.getCode().equals(planCode) == true)
              return p;
        return null;
      }


    public static List<Plan> getAvailablePlans(Connection C, User_Data U)
    throws Exception
      {
        if (U == null)
         return Plan_Factory.getPlans();

        Promo_Data P = getUserPromo(C, U);
        if (P != null)
          return Plan_Factory.getPlans(P.getPlansAsArray(), P.getDiscountPct(), P.getDiscountMonths(), P.getDiscountYearPct(), P.getAutoRenew()
                                       , P.isNullInitialCredits() == true ? null : P.getInitialCredits());

        return null;
      }

    /**
     * @return the {@link Promo_Data} a user registered under, or null if they have no promo code, or the code
     * no longer resolves to a record. Centralized here (rather than each caller re-doing
     * {@code Promo_Factory.lookupByCode}) so every consumer -- plan availability, the signup credit bonus, etc. --
     * agrees on what "the user's promo" means.
     */
    public static Promo_Data getUserPromo(Connection C, User_Data U)
    throws Exception
      {
        if (U == null || U.isNullPromoCode() == true)
          return null;
        Promo_Data P = Promo_Factory.lookupByCode(U.getPromoCode());
        return P.read(C) == true ? P : null;
      }

    /**
     * Whether the user's CURRENTLY ACTIVE plan/billing for a product is one flagged {@code Plan.autoPlan=true}
     * -- i.e., they are still riding the auto-assigned free/trial plan (see {@link #autoAssignFreePlan}) rather
     * than a plan they actually purchased. Backs the credit-meter widget's one-time "you're on a free trial"
     * welcome message (see {@code FloriaPayments.CreditGauge} in module-payments.js) via
     * {@code /svc/wanda/credits/balance} -- display-only, never used for gating/mutation.
     *
     * @return false if the user has no active billing for the product at all (nothing to report), or if their
     *         active plan isn't an autoPlan one.
     */
    public static boolean isOnAutoPlan(Connection C, User_Data U, String productId)
    throws Exception
      {
        UserPlanBilling_Data UPB = UserPlanBilling_Factory.lookupByUserActive(U.getRefnum(), productId);
        if (UPB.read(C) == false)
          return false;

        List<Plan> plans = getAvailablePlans(C, U);
        if (plans != null)
          for (Plan p : plans)
            if (p._Plan.getRefnum() == UPB.getPlanRefnum())
              return p._Plan.isPlanTypeCredits() == true && p._Plan.getAutoPlan() == true;

        return false;
      }


    public static class SelectedPlan
      {
        protected final Plan             _plan;
        protected final PlanPricing_Data _pricing;
        protected final char             _cycle;

        public SelectedPlan(Plan plan, PlanPricing_Data pricing, char cycle)
          {
            _plan = plan;
            _pricing = pricing;
            _cycle = cycle;
          }

        public String getPlanCode()
          {
            return _plan._Plan.getCode();
          }

        public long getPlanRefnum()
          {
            return _plan._Plan.getRefnum();
          }

        /**
         * The product grouping key: all tiers of a same product share it, and a user can hold at most one
         * active subscription/billing/pre-order per product.
         */
        public String getProductId()
          {
            return _plan._Plan.getPaymentSystemProductId();
          }

        public char getPlanType()
          {
            return _plan._Plan.getPlanType();
          }

        public boolean isCreditPlan()
          {
            return _plan._Plan.isPlanTypeCredits();
          }

        /**
         * The credit UNITS granted by a purchase of this plan at this price point. Only meaningful for
         * planType=C. Null otherwise.
         */
        public BigDecimal getCredits()
          {
            return _pricing.isNullOneTimeCredits() == true ? null : _pricing.getOneTimeCredits();
          }

        public String getBillingCurrency()
          {
            return _pricing.getCurrency();
          }

        public BigDecimal getBillingPrice()
          {
            if (_cycle == UserPlanSubscription_Data._cycleOneTime)
              return _pricing.isNullOneTime() == true ? null : _pricing.getOneTime();
            if (_cycle == UserPlanSubscription_Data._cycleYearly)
              return _pricing.isNullYearly() == true ? null : _pricing.getYearly();
            return _pricing.isNullMonthly() == true ? null : _pricing.getMonthly();
          }

        public char getBillingCycle()
          {
            return _cycle;
          }

        public double getDiscountPct()
          {
            return _plan.getDiscountPct();
          }

        public short getDiscountMonths()
          {
            return _plan.getDiscountMonths();
          }
      }

    public static SelectedPlan getPlanPrice(List<Plan> plans, String planCode, String currency, char cycle)
      {
        if (plans == null || plans.isEmpty() == true)
          return null;
        boolean foundPlan = false;
        for (Plan p : plans)
          if (p._Plan.getCode().equals(planCode) == true)
            {
              foundPlan = true;
              if (p._Plan.isPlanTypeContactUs() == true)
                {
                  LOG.error("Plan '" + planCode + "' is a contact-us plan and cannot be purchased online.");
                  return null;
                }
              // The cycle must match the nature of the plan: credit packs are one-time only, subscriptions are monthly/yearly only.
              if (p._Plan.isPlanTypeCredits() == true && cycle != UserPlanSubscription_Data._cycleOneTime
              || p._Plan.isPlanTypeSubscription() == true && cycle == UserPlanSubscription_Data._cycleOneTime)
                {
                  LOG.error("Plan '" + planCode + "' of type '" + p._Plan.getPlanType() + "' cannot be purchased with cycle '" + cycle + "'.");
                  return null;
                }
              for (PlanPricing_Data pp : p._Pricings)
                {
                  if (pp.getCurrency().equals(currency) == true)
                    {
                      SelectedPlan SP = new SelectedPlan(p, pp, cycle);
                      // Guard against a pricing row that simply doesn't define the requested cycle's amount: without this,
                      // a null price flows all the way into the payment provider call.
                      if (SP.getBillingPrice() == null)
                        {
                          LOG.error("Plan '" + planCode + "' has a pricing row for currency '" + currency + "' but no amount defined for cycle '" + cycle + "'.");
                          return null;
                        }
                      return SP;
                    }
                }
              LOG.error("Plan found, but no pricing for currency " + currency);
            }
        if (foundPlan == false)
          LOG.error("No plan found for code " + planCode);
        return null;
      }


    /**
     * The credit UNITS granted by a purchase of this plan in the supplied currency, or null if this isn't a
     * credit plan, has no pricing row for that currency, or that row defines no credits.
     */
    public static BigDecimal getCredits(Plan p, String currency)
      {
        if (p == null || p._Plan.isPlanTypeCredits() == false || p._Pricings == null)
          return null;
        for (PlanPricing_Data pp : p._Pricings)
          if (pp.getCurrency().equals(currency) == true)
            return pp.isNullOneTimeCredits() == true ? null : pp.getOneTimeCredits();
        return null;
      }


    public static void clearUserForPlan(Connection C, RequestUtil Req, User_Data U, boolean refreshTS)
    throws Exception
      {
        if (refreshTS == true)
          {
            U.setLastPlanNow();
            if (U.write(C) == false)
              throw new Exception("Cannot update user " + U.getRefnum());
          }
        Req.setSessionInt(SessionUtil.Attributes.PLAN_CLEAR.toString(), 1);
      }

  }
