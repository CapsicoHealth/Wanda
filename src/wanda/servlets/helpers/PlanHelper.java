package wanda.servlets.helpers;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
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
        // If they do, check if they have an active billing
        UserPlanBilling_Data UPB = UserPlanBilling_Factory.lookupByUserActive(U.getRefnum());
        // return true if no active plan found
        return UPB.read(C) == false;
      }


    public static List<Plan> getAvailablePlans(Connection C, User_Data U)
    throws Exception
      {
        if (U.isNullPromoCode() == false)
          {
            Promo_Data P = Promo_Factory.lookupByCode(U.getPromoCode());
            if (P.read(C) == true)
              return Plan_Factory.getPlans(P.getPlansAsArray(), P.getDiscountPct(), P.getDiscountMonths(), P.getDiscountYearPct(), P.getAutoRenew());
          }

        return null;
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

        public String getBillingCurrency()
          {
            return _pricing.getCurrency();
          }

        public BigDecimal getBillingPrice()
          {
            return _cycle == UserPlanSubscription_Data._cycleYearly ? _pricing.getYearly() : _pricing.getMonthly();
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
              for (PlanPricing_Data pp : p._Pricings)
                {
                  if (pp.getCurrency().equals(currency) == true)
                    return new SelectedPlan(p, pp, cycle);
                }
              LOG.error("Plan found, but no pricing for currency " + currency);
            }
        if (foundPlan == false)
          LOG.error("No plan found for code " + planCode);
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
