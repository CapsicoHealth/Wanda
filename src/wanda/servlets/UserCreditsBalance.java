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

import java.math.BigDecimal;

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import tilda.utils.json.JSONPrinter;
import wanda.data.UserPlanSubscription_Data;
import wanda.data.User_Data;
import wanda.servlets.helpers.CreditHelper;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;

/**
 * Returns the signed-in user's credit balance for a product, for the credit-meter widget. Cheap enough to poll:
 * it's a single indexed read of the wallet (the user's active subscription for that product).
 */
@WebServlet("/svc/user/credits/balance")
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

        UserPlanSubscription_Data UPS = CreditHelper.getWallet(C, U, productId);
        BigDecimal balance = CreditHelper.getBalance(UPS);

        JSONPrinter j = new JSONPrinter();
        j.addElement("productId", productId);
        j.addElement("balance", balance, 0);
        // No wallet at all vs. an exhausted wallet: the UI may want to say "buy credits" vs. "top up".
        j.addElement("hasWallet", UPS != null);
        j.addElement("creditsPurchased", UPS == null || UPS.isNullCreditsPurchased() == true ? BigDecimal.ZERO : UPS.getCreditsPurchased(), 0);
        res.successJson(j);
      }
  }
