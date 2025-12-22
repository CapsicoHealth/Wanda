package wanda.servlets;

import java.io.PrintWriter;
import java.util.List;

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import tilda.utils.DateTimeUtil;
import tilda.utils.EncryptionUtil;
import tilda.utils.TextUtil;
import tilda.utils.json.JSONUtil;
import wanda.data.UserPlanPreOrder_Data;
import wanda.data.UserPlanPreOrder_Factory;
import wanda.data.User_Data;
import wanda.data.importers.promos.Plan;
import wanda.servlets.helpers.PayPalHelper;
import wanda.servlets.helpers.PayPalPreOrder;
import wanda.servlets.helpers.PlanHelper;
import wanda.servlets.helpers.PlanHelper.SelectedPlan;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.exceptions.BadRequestException;

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

        PlanHelper.getAvailablePlans(C, U);
        List<Plan> plans = PlanHelper.getAvailablePlans(C, U);
        SelectedPlan p = PlanHelper.getPlanPrice(plans, planCode, currency, cycle);
        if (p == null)
          {
            req.addError("planCode", "No plan found matching planCode = '" + planCode + "' for the currency '" + currency + "'.");
            req.throwIfErrors();
          }
        
        UserPlanPreOrder_Data UPD = getPreOrder(req, C, U, paymentProvider, p);
        
        PrintWriter out = res.setContentType(ResponseUtil.ContentType.JSON);
        JSONUtil.startOK(out, '{');
        JSONUtil.print(out, "orderId", true, UPD.getOrderId());
        JSONUtil.print(out, "customId", false, UPD.getCustomId());
        JSONUtil.end(out, '}');
      }

    /**
     * Check that the existing pre-order is still valid:
     * - was created less than 2 hours ago
     * - is for the same user
     * - is for the same payment provider
     * - is for the same plan
     * - is for the same amount
     * - is for the same currency
     * 
     * @param UPD
     * @param U
     * @param paymentProvider
     * @param p
     * @return
     */
    private static boolean isPreOrderStillValid(UserPlanPreOrder_Data UPD, User_Data U, String paymentProvider, SelectedPlan p)
      {
        return DateTimeUtil.hoursBetween(UPD.getCreated(), DateTimeUtil.nowUTC()) < 2
            && UPD.getUserRefnum() == U.getRefnum()
            && UPD.getPaymentProvider().equals(paymentProvider) == true
            && UPD.getPlanRefnum() == p.getPlanRefnum()
            && UPD.getTotal().compareTo(p.getBillingPrice()) == 0
            && UPD.getCurrency().equals(p.getBillingCurrency()) == true
            && UPD.getCycle() == p.getBillingCycle()
        ;
      }

    /**
     * Get or create a pre-order for the user.
     * 
     * @param req
     * @param C
     * @param U
     * @param paymentProvider
     * @param p
     * @return
     * @throws Exception
     * @throws BadRequestException
     */
    private static UserPlanPreOrder_Data getPreOrder(RequestUtil req, Connection C, User_Data U, String paymentProvider, SelectedPlan p)
    throws Exception, BadRequestException
      {
        UserPlanPreOrder_Data UPD = UserPlanPreOrder_Factory.lookupByUser(U.getRefnum());
        if (UPD.read(C) == true && isPreOrderStillValid(UPD, U, paymentProvider, p) == true)
         return UPD;

        if (UPD.isSuccessfullyRead() == true) // If there was a prior Order (that was unprocessed), we need to clean.
         UserPlanPreOrder_Factory.delete(C, U.getRefnum());
        
        // Create a new Pre-Order
        String customId = EncryptionUtil.hash256Str(""+U.getRefnum(), EncryptionUtil.getToken(16, true));
        PayPalPreOrder PPPO = PayPalHelper.createOrder(paymentProvider, customId, p.getBillingCurrency(), p.getBillingPrice());
        if (PPPO == null || TextUtil.isNullOrEmpty(PPPO.id) == true)
         throw new Exception("Could not create order with payment provider " + paymentProvider + ", planCode='"+p.getPlanCode()+"' and currency '"+p.getBillingCurrency()+"'.");
       if (PPPO.isCreated() == false)
         throw new Exception("The pre-order from payment provider " + paymentProvider + ", planCode='"+p.getPlanCode()+"' and currency '"+p.getBillingCurrency()+"' was returned as incomplete.");

        UPD = UserPlanPreOrder_Factory.create(U.getRefnum(), p.getPlanRefnum(), paymentProvider, customId, DateTimeUtil.nowUTC(), p.getBillingPrice(), p.getBillingCurrency(), p.getBillingCycle());
        UPD.setOrderId(PPPO.id);
        UPD.setOrderDetails(PPPO.toJsonString());
        if (UPD.write(C) == false)
          {
            req.addError("UserPlanPreOrder", "Could not create pre-order record for user.");
            req.throwIfErrors();
          }
        
        return UPD;
      }
  }
