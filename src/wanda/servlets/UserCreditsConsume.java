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
import wanda.data.User_Data;
import wanda.servlets.helpers.CreditHelper;
import wanda.servlets.helpers.RoleHelper;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;

/**
 * Deducts credits from a user's wallet when a metered action takes place.
 * <P>
 * <B>This endpoint mutates money-equivalent state, so it is ADMIN-gated.</B> It is meant for internal,
 * service-to-service calls, not for an end user's browser session: an unauthenticated (or merely signed-in)
 * caller must never be able to name their own user/amount. Application code running in-process should prefer
 * calling {@link CreditHelper#consume} directly and skip this servlet entirely.
 * <P>
 * Note the deliberate asymmetry with a "spend" API: an insufficient balance is NOT an error. It returns
 * <CODE>sufficient:false</CODE> having written nothing, so the caller can prompt the user to top up.
 */
@WebServlet("/svc/user/credits/consume")
public class UserCreditsConsume extends SimpleServlet
  {
    private static final long serialVersionUID = 8825110455119364135L;

    public UserCreditsConsume()
      {
        super(true, false, true);
      }

    @Override
    protected void justDo(RequestUtil req, ResponseUtil res, Connection C, User_Data U)
    throws Exception
      {
        throwIfUserInvalidRole(U, RoleHelper.ADMINROLES);

        long userRefnum = req.getParamLong("userRefnum", true);
        String productId = req.getParamString("productId", true);
        BigDecimal credits = req.getParamBigDecimal("credits", true);
        String reference = req.getParamString("reference", false);

        req.throwIfErrors();

        if (credits.signum() <= 0)
          req.addError("credits", "The number of credits to charge must be strictly positive.");
        req.throwIfErrors();

        User_Data TargetU = wanda.data.User_Factory.lookupByPrimaryKey(userRefnum);
        if (TargetU.read(C) == false)
          throw new wanda.web.exceptions.NotFoundException("User", Long.toString(userRefnum));

        // "charge" semantics: the work has already been done, so the debit is always recorded even if it takes
        // the balance negative. The caller decides what to do with the returned code.
        CreditHelper.CreditStatus status = CreditHelper.charge(C, TargetU, productId, credits, reference);

        JSONPrinter j = new JSONPrinter();
        status.toJSON(j);
        res.successJson(j);
      }
  }
