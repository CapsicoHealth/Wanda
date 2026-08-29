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

package wanda.servlets;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import tilda.utils.json.JSONPrinter;
import wanda.data.UserPlanCreditLedger_Data;
import wanda.data.UserPlanCreditLedger_Factory;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;

/**
 * The signed-in user's USE-only credit ledger for a product, over a recent rolling window -- backs the
 * "Usage" tab of the Plans/Billing popup (FloriaPayments.PlansDialog.UsageDashboard, module-payments.js).
 * <P>
 * Unlike {@link UserCreditsHistory} (every movement type -- GRANT/USE/BONUS/ADJ -- simply paginated most
 * recent first, used for a plain billing statement), this is deliberately scoped to just the metered SPEND
 * (type=USE) within the requested window, which is what a "usage trends / cost hot-spots" dashboard actually
 * needs; GRANT/BONUS/ADJ rows would only be noise there (they're already visible via the Plans tab's billing
 * history and the balance banner).
 * <P>
 * All bucketing/grouping (by day for the trend line, by reference for the hot-spot cards, top-N by notes,
 * distinct notes for the filter dropdown) is deliberately left to the front end: the raw row count for a
 * single user's single-product wallet, even over 90 days, is small enough (one row per metered call) that
 * there is no real value in adding several bespoke aggregate queries here -- shipping the raw rows once and
 * letting the dashboard slice them every way it needs is both simpler and cheaper to maintain.
 */
@WebServlet("/svc/wanda/credits/usage")
public class UserCreditsUsage extends SimpleServlet
  {
    private static final long serialVersionUID = 5591943985398431822L;

    public UserCreditsUsage()
      {
        super(true, false, true);
      }

    @Override
    protected void justDo(RequestUtil req, ResponseUtil res, Connection C, User_Data U)
    throws Exception
      {
        String productId = req.getParamString("productId", true);
        int days = req.getParamInt("days", 30);

        req.throwIfErrors();

        // Only 30/60/90 are meaningful window sizes for this dashboard (see the "Days" selector in
        // module-payments.js); anything else quietly falls back to the default rather than erroring, since
        // this is a display convenience, not a security-sensitive parameter.
        if (days != 30 && days != 60 && days != 90)
          days = 30;

        // Scoped to the signed-in user by construction, same as UserCreditsHistory: a user can never see
        // someone else's ledger. 5000 is a generous cap for a single user's single-product wallet over 90
        // days -- see this class's docs above for why no further server-side aggregation is attempted.
        List<UserPlanCreditLedger_Data> All = UserPlanCreditLedger_Factory.lookupWhereUser(C, U.getRefnum(), productId, 0, 5000);

        ZonedDateTime cutoff = ZonedDateTime.now().minusDays(days);
        List<UserPlanCreditLedger_Data> Filtered = new ArrayList<UserPlanCreditLedger_Data>();
        for (UserPlanCreditLedger_Data L : All)
          if (L.isTypeUse() == true && L.getCreated().isAfter(cutoff) == true)
            Filtered.add(L);

        JSONPrinter j = new JSONPrinter();
        j.addElement("productId", productId);
        j.addElement("days", days);
        j.addElement("items", Filtered, "");
        res.successJson(j);
      }
  }
