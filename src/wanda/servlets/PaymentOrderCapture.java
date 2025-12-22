package wanda.servlets;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import tilda.utils.DateTimeUtil;
import tilda.utils.json.JSONUtil;
import wanda.data.UserPlanBilling_Data;
import wanda.data.UserPlanBilling_Factory;
import wanda.data.UserPlanPreOrder_Data;
import wanda.data.UserPlanPreOrder_Factory;
import wanda.data.UserPlanSubscription_Data;
import wanda.data.UserPlanSubscription_Factory;
import wanda.data.User_Data;
import wanda.servlets.helpers.PayPalHelper;
import wanda.servlets.helpers.PayPalOrderDetails;
import wanda.servlets.helpers.PlanHelper;
import wanda.web.EMailSender;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.exceptions.NotFoundException;

@WebServlet("/svc/payments/order/capture")
public class PaymentOrderCapture extends SimpleServlet
  {
    private static final long serialVersionUID = 7833614578489016882L;

    public PaymentOrderCapture()
      {
        super(true, false, true);
      }

    @Override
    protected void justDo(RequestUtil req, ResponseUtil res, Connection C, User_Data U)
    throws Exception
      {
        String paymentProvider = req.getParamString("paymentProvider", true);
        String orderId = req.getParamString("orderId", true);

        req.throwIfErrors();

        // The OrderCreate step must have been performed before this "capture" step. So we
        // load the pre-order and make sure the orderId and paymentProvider match.
        UserPlanPreOrder_Data UPPO = UserPlanPreOrder_Factory.lookupByUser(U.getRefnum());
        if (UPPO.read(C) == false)
          throw new NotFoundException("UserPlanPreOrder", "No pre-order found for user");
        if (UPPO.getOrderId().equals(orderId) == false || UPPO.getPaymentProvider().equalsIgnoreCase(paymentProvider) == false)
          throw new NotFoundException("UserPlanPreOrder", "No pre-order found for order and provider supplied");
        // Then we delete the pre-order.
        UserPlanPreOrder_Factory.delete(C, U.getRefnum());

        // Next, we either create or update the user's plan subscription based on the pre-order
        UserPlanSubscription_Data UPS = getUserPlanSubscription(C, U, UPPO);
        UserPlanBilling_Data UPB = checkBilling(C, U, UPS);
        if (UPB == null) // need a new billing.
          {
            UPB = UserPlanBilling_Factory.create(UPS.getRefnum(), U.getRefnum(), UPS.getPlanRefnum(), paymentProvider, UPPO.getCustomId(), orderId, false);
            UPB.setStatus(UserPlanBilling_Data._statusPending);
            ZonedDateTime now = DateTimeUtil.nowUTC();
            UPB.setOrderDt(now);
            UPB.setExpiryDt(UPS.getExpiryDtFrom(now.toLocalDate()));
            UPB.setTotal(UPPO.getTotal());
            UPB.setCurrency(UPPO.getCurrency());
            if (UPB.write(C) == false)
              throw new Exception("Cannot create plan billing record for user " + U.getRefnum());

            PayPalOrderDetails ppod = PayPalHelper.captureOrder(paymentProvider, orderId);
            if (ppod == null)
              throw new NotFoundException("Payment provider", paymentProvider);

            UPB.setMessage("Order: " + ppod.status + "  /  Capture: " + ppod.getFirstCaptureStatus());
            UPB.setOrderCapture(ppod.toJsonString());
            UPB.setStatus(switch (ppod.getOrderStatusEnum())
              {
                case COMPLETED -> UserPlanBilling_Data._statusPaid;
                case APPROVED, CREATED -> UserPlanBilling_Data._statusCreated;
                case PAYER_ACTION_REQUIRED, SAVED -> UserPlanBilling_Data._statusPending;
                case VOIDED -> UserPlanBilling_Data._statusVoided;
                case UNKNOWN -> UserPlanBilling_Data._statusFailed;
                default -> UserPlanBilling_Data._statusFailed;
              });
            if (UPB.isStatusPaid() == true)
              {
                UPB.setActive(true); // Only paid orders are active, the rest are not.
                PlanHelper.clearUserForPlan(C, req, U, false);
              }

            // we can't fail here anymore. If we failed, the user would be charged but we wouldn't record it.
            if (UPB.write(C) == false)
              {
                LOG.error("Cannot update plan billing record with capture details for user " + U.getRefnum() + " and orderId " + orderId);
                EMailSender.sendMailSys(null, null, null, "Capsico Order #" + orderId + " recording failed!!!", "The order with ID " + orderId + " for user " + U.getRefnum() + " was successfully captured but we failed to record the capture details.\n\nYou should check the billing record and update it manually if needed.\n\nThanks.\n\n", true, true);
              }
          }

        PrintWriter out = res.setContentType(ResponseUtil.ContentType.JSON);
        JSONUtil.startOK(out, '{');
        JSONUtil.print(out, "orderId", true, UPB.getOrderId());
        JSONUtil.print(out, "message", false, UPB.getMessage());
        JSONUtil.print(out, "completed", false, UPB.isStatusPaid());
        JSONUtil.end(out, '}');
      }

    protected static UserPlanBilling_Data checkBilling(Connection C, User_Data U, UserPlanSubscription_Data UPS)
    throws Exception
      {
        UserPlanBilling_Data UPB = UserPlanBilling_Factory.lookupByUserActive(U.getRefnum());
        if (UPB.read(C) == false) // there is currently an active plan.
          return null;

        if (UPB.getSubscriptionRefnum() != UPS.getRefnum()) // Is the current billing for the current subscription?
          {
            // If no, the billing has expired
            UPB.setActive(false);
            if (UPB.write(C) == false)
              throw new Exception("Cannot terminate existing plan billing for user " + U.getRefnum() + " due to a new subscription.");
            return null;
          }
        
        LocalDate today = DateTimeUtil.nowLocalDate();
        if (UPB.getExpiryDt().isAfter(today) == true) // is the current billing still active?
          {
            // If no, the billing has expired
            UPB.setActive(false);
            if (UPB.write(C) == false)
              throw new Exception("Cannot terminate existing plan billing for user " + U.getRefnum() + " due to the billing plan expiring.");
            return null;
          }

        return UPB;
      }

    private static UserPlanSubscription_Data getUserPlanSubscription(Connection C, User_Data U, UserPlanPreOrder_Data UPPO)
    throws Exception
      {
        // Check if we already have an active subscription matching the pre-order
        UserPlanSubscription_Data UPS = UserPlanSubscription_Factory.lookupByUserActivePlan(U.getRefnum());
        if (UPS.read(C) == false)
          {
            UPS = UserPlanSubscription_Factory.create(U.getRefnum(), true, UPPO.getPlanRefnum(), UPPO.getCurrency(), UPPO.getCycle(), DateTimeUtil.nowLocalDate());
            if (UPS.write(C) == false)
              throw new Exception("Cannot create plan subscription for user " + U.getRefnum());
          }
        else if (isStillValid(C, U, UPPO, UPS) == false)
          {
            // The existing subscription is not valid anymore, we need to end it and create a new one
            UPS.setEndDt(DateTimeUtil.nowLocalDate());
            UPS.setActive(false);
            if (UPS.write(C) == false)
              throw new Exception("Cannot update existing plan subscription for user " + U.getRefnum());

            // Create the new one
            UPS = UserPlanSubscription_Factory.create(U.getRefnum(), true, UPPO.getPlanRefnum(), UPPO.getCurrency(), UPPO.getCycle(), DateTimeUtil.nowLocalDate());
            if (UPS.write(C) == false)
              throw new Exception("Cannot create plan subscription for user " + U.getRefnum());
          }

        return UPS;
      }

    private static boolean isStillValid(Connection C, User_Data U, UserPlanPreOrder_Data UPPO, UserPlanSubscription_Data UPS)
      {
        return UPS.getActive() == true
        && UPS.getPlanRefnum() == UPPO.getPlanRefnum()
        && UPS.getCurrency().equals(UPPO.getCurrency()) == true
        && UPS.getCycle() == UPPO.getCycle()
        && UPS.isNullEndDt() == true;
      }
  }
