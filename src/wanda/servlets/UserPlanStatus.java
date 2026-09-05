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


package wanda.servlets;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import tilda.utils.TextUtil;
import tilda.utils.json.JSONPrinter;
import wanda.data.UserBillingView_Data;
import wanda.data.UserBillingView_Factory;
import wanda.data.User_Data;
import wanda.data.importers.promos.Plan;
import wanda.servlets.helpers.CreditHelper;
import wanda.servlets.helpers.PlanHelper;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.config.Wanda;

@WebServlet("/svc/user/plan/status")
public class UserPlanStatus extends SimpleServlet
  {
    private static final long serialVersionUID = 2358369573367773870L;

    public UserPlanStatus()
      {
        super(false, false, true);
      }

    @Override
    public void init(ServletConfig Conf)
      {
      }

    String _ENTERPRISE_LEARNING_EMAIL = Wanda.getExtra("learning", "ENTERPRISE_LEARNING_EMAIL");
    String _ENTERPRISE_LEARNING_LABEL = Wanda.getExtra("learning", "ENTERPRISE_LEARNING_LABEL");

    String _ENTERPRISE_AGENTIC_EMAIL  = Wanda.getExtra("llmserver-main", "enterpriseEmail");
    String _ENTERPRISE_AGENTIC_LABEL  = Wanda.getExtra("llmserver-main", "enterpriseLabel");


    @Override
    protected void justDo(RequestUtil req, ResponseUtil res, Connection C, User_Data U)
    throws Exception
      {
        // Optional: restrict the catalog to a single product (e.g., only the credit packs for a "buy more
        // credits" prompt). Absent means all products, which is the pre-existing behavior.
        String productId = req.getParamString("productId", false);

        List<Plan> plans = PlanHelper.getAvailablePlans(C, U);
        if (plans != null && TextUtil.isNullOrEmpty(productId) == false)
          {
            List<Plan> filtered = new ArrayList<Plan>();
            for (Plan p : plans)
              if (p._Plan.getPaymentSystemProductId().equals(productId) == true)
                filtered.add(p);
            plans = filtered;
          }

        List<UserBillingView_Data> UBVL = U == null ? null : UserBillingView_Factory.lookupWhereUserRefnum(C, U.getRefnum(), 0, 24);
        boolean current = isCurrent(UBVL);
        if (plans != null)
          for (Plan p : plans)
            {
              List<String> descrs = new ArrayList<String>();
              Iterator<String> I = p._Plan.getDescr();
              while (I.hasNext() == true)
                {
                  String descr = I.next();
                  descr = descr.replace("${ENTERPRISE_LEARNING_EMAIL}", "<A href=\"mailto:" + URLEncoder.encode(_ENTERPRISE_LEARNING_EMAIL, "UTF-8") + "\">" + _ENTERPRISE_LEARNING_LABEL + "</A>");
                  descr = descr.replace("${ENTERPRISE_AGENTIC_EMAIL}", "<A href=\"mailto:" + URLEncoder.encode(_ENTERPRISE_AGENTIC_EMAIL, "UTF-8") + "\">" + _ENTERPRISE_AGENTIC_LABEL + "</A>");
                  descrs.add(descr);
                }
              p._Plan.setDescr(descrs);

              // The promo signup bonus is a first-purchase-only perk per product wallet: stop advertising it as
              // available once this user's wallet for that product has already received it.
              if (U != null && p.getInitialCredits() != null && CreditHelper.hasReceivedSignupBonus(C, U, p._Plan.getPaymentSystemProductId()) == true)
                p.clearInitialCredits();
            }
        JSONPrinter j = new JSONPrinter();
        j.addElement("plans", plans, "");
        j.addElement("billingHistory", UBVL, "");
        j.addElement("billingCurrent", current);
        // When scoped to one product, hand back that product's credit status too, so a "buy more credits"
        // screen can be painted from this single call rather than a second round trip.
        if (U != null && TextUtil.isNullOrEmpty(productId) == false)
          CreditHelper.check(C, U, productId).toJSON(j);
        res.successJson(j);
      }

    /**
     * Check if the user's SUBSCRIPTION billing status is current.
     * <UL>
     * <LI>Subscription is active</LI>
     * <LI>Billing is active</LI>
     * <LI>Billing status is "Paid"</LI>
     * <LI>Billing expiry date is in the future</LI>
     * </UL>
     * Note this deliberately ignores credit (planType=C) billings: a credit pack purchase is not a subscription,
     * and letting the most recent row decide would make "billingCurrent" flip depending on whether the user's
     * last purchase happened to be a top-up rather than a plan.
     * 
     * @param UBVL
     * @return
     */
    private static boolean isCurrent(List<UserBillingView_Data> UBVL)
      {
        if (UBVL == null || UBVL.isEmpty() == true)
          return false;

        for (UserBillingView_Data UBV : UBVL)
          if (UBV.isPlanTypeSubscription() == true && UBV.getActive() == true)
            return true;

        return false;
      }

  }
