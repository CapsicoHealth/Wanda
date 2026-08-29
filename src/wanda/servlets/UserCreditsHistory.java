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
 * The signed-in user's credit statement for a product: every grant, use and adjustment, most recent first.
 * Reads straight off the WORM ledger, which is the source of truth for balances.
 */
@WebServlet("/svc/wanda/credits/history")
public class UserCreditsHistory extends SimpleServlet
  {
    private static final long serialVersionUID = 3300950265120885544L;

    public UserCreditsHistory()
      {
        super(true, false, true);
      }

    @Override
    protected void justDo(RequestUtil req, ResponseUtil res, Connection C, User_Data U)
    throws Exception
      {
        String productId = req.getParamString("productId", true);
        int start = req.getParamInt("start", 0);
        int size = req.getParamInt("size", 50);

        req.throwIfErrors();

        if (size <= 0 || size > 500) // Keep a caller from asking for an unbounded statement.
          size = 50;

        // Scoped to the signed-in user by construction: a user can never read someone else's statement.
        List<UserPlanCreditLedger_Data> L = UserPlanCreditLedger_Factory.lookupWhereUser(C, U.getRefnum(), productId, start, size);

        JSONPrinter j = new JSONPrinter();
        j.addElement("productId", productId);
        j.addElement("history", L, "");
        res.successJson(j);
      }
  }
