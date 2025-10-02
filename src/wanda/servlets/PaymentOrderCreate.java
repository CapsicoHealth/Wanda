package wanda.servlets;

import java.io.PrintWriter;
import java.util.List;

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import tilda.utils.EncryptionUtil;
import tilda.utils.TextUtil;
import tilda.utils.json.JSONUtil;
import wanda.data.User_Data;
import wanda.data.importers.promos.Plan;
import wanda.servlets.helpers.PayPalHelper;
import wanda.servlets.helpers.PlanHelper;
import wanda.servlets.helpers.PlanHelper.SelectedPlan;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.config.PaymentSystem;
import wanda.web.config.Wanda;
import wanda.web.exceptions.NotFoundException;

@WebServlet("/svc/payments/order/create")
public class PaymentOrderCreate extends SimpleServlet
  {
    private static final long serialVersionUID = 7833614578489016882L;

    public PaymentOrderCreate()
      {
        super(true, false, true);
      }

    @Override
    protected void justDo(RequestUtil req, ResponseUtil res, Connection C, User_Data U)
    throws Exception
      {
        String paymentProvider = req.getParamString("paymentProvider", true);
        String planCode = req.getParamString("planCode", true);
        char cycle = req.getParamChar("cycle", true);
        String currency = req.getParamString("currency", true);

        req.throwIfErrors();

        PaymentSystem PS = Wanda.getPaymentSystem(paymentProvider, true);
        if (PS == null)
          throw new NotFoundException("Payment provider", paymentProvider);

        PlanHelper.getAvailablePlans(C, U);

        List<Plan> plans = PlanHelper.getAvailablePlans(C, U);
        SelectedPlan p = PlanHelper.getPlanPrice(plans, planCode, currency, cycle);
        if (p == null)
          {
            req.addError("planCode", "No plan found matching planCode = '" + planCode + "' for the currency '" + currency + "'.");
            req.throwIfErrors();
          }
        
        String customId = EncryptionUtil.hash256Str(""+U.getRefnum(), EncryptionUtil.getToken(16, true));
        String orderId = PayPalHelper.createOrder(PS, customId, p.getBillingCurrency(), p.getBillingPrice());
        if (TextUtil.isNullOrEmpty(orderId) == true)
         throw new Exception("Could not create order with payment provider " + paymentProvider + ", planCode='"+planCode+"' and currency '"+currency+"'.");
        
        PrintWriter out = res.setContentType(ResponseUtil.ContentType.JSON);
        JSONUtil.startOK(out, '{');
        JSONUtil.print(out, "orderId", true, orderId);
        JSONUtil.print(out, "customId", false, orderId);
        JSONUtil.end(out, '}');
      }
  }
