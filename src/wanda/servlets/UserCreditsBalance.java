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

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import tilda.utils.json.JSONPrinter;
import wanda.data.User_Data;
import wanda.servlets.helpers.CreditHelper;
import wanda.servlets.helpers.CreditHelper.CreditSnapshot;
import wanda.servlets.helpers.PlanHelper;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;

/**
 * Returns the signed-in user's credit balance for a product, for the credit-meter widget. Backed by
 * {@link CreditHelper#getSnapshot}'s small, bounded, short-TTL cache, so a widget polling every few seconds (or
 * embedded more than once on the same page) doesn't put repeated load on the database for what is, after all,
 * a display-only read: gating/mutation decisions never go through this path (see CreditHelper).
 */
@WebServlet("/svc/wanda/credits/balance")
public class UserCreditsBalance extends SimpleServlet
  {
    private static final long serialVersionUID = 4471943985398431811L;

    public UserCreditsBalance()
      {
        super(true, false, true);
      }

    @Override
    protected void justDo(RequestUtil req, ResponseUtil res, Connection C, User_Data U)
    throws Exception
      {
        String productId = req.getParamString("productId", true);

        req.throwIfErrors();

        CreditSnapshot S = CreditHelper.getSnapshot(C, U, productId);

        JSONPrinter j = new JSONPrinter();
        j.addElement("productId", productId);
        j.addElement("balance", S._balance, 0);
        // No wallet at all vs. an exhausted wallet: the UI may want to say "buy credits" vs. "top up".
        j.addElement("hasWallet", S._hasWallet);
        j.addElement("creditsPurchased", S._creditsPurchased, 0);
        // The wallet's balance right after its last top-up finished (leftover balance before it PLUS what was
        // added): the credit-meter gauge uses this, when non-zero, as its "100%" scale instead of a fixed tier
        // -- see CreditHelper.getLastTopUpAmount.
        j.addElement("lastTopUpAmount", S._lastTopUpAmount, 0);
        // Whether the user is still riding an auto-assigned free/trial plan (Plan.autoPlan=true, e.g. a promo's
        // free credit pack) rather than one they actually purchased -- lets the gauge show a one-time "you're on
        // a free trial" welcome message. Display-only, same as everything else on this endpoint.
        j.addElement("onTrialPlan", PlanHelper.isOnAutoPlan(C, U, productId));
        res.successJson(j);
      }
  }

