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

import java.io.PrintWriter;

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import tilda.utils.json.JSONUtil;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.config.PaymentSystem;
import wanda.web.config.Wanda;
import wanda.web.exceptions.NotFoundException;

@WebServlet("/svc/payments/provider/details")
public class PaymentProviderDetails extends SimpleServlet
  {
    private static final long serialVersionUID = 7833614578489016882L;

    public PaymentProviderDetails()
      {
        super(true, false, true);
      }

    @Override
    protected void justDo(RequestUtil req, ResponseUtil res, Connection C, User_Data U)
    throws Exception
      {
        String paymentProvider = req.getParamString("paymentProvider", true);
        
        req.throwIfErrors();

        PaymentSystem PS = Wanda.getPaymentSystem(paymentProvider);
        if (PS == null)
          throw new NotFoundException("Payment provider", paymentProvider);

        PrintWriter out = res.setContentType(ResponseUtil.ContentType.JSON);
        JSONUtil.startOK(out, '{');
        JSONUtil.print(out, "id", true, PS._id);
        JSONUtil.print(out, "clientId", false, PS.getCredentials()._clientId);
        JSONUtil.print(out, "sandbox", false, PS._sandboxMode);
        JSONUtil.end(out, '}');
      }
  }
