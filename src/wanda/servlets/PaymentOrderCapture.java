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
import java.math.BigDecimal;
import java.time.ZonedDateTime;

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import tilda.utils.DateTimeUtil;
import tilda.utils.json.JSONUtil;
import wanda.data.Promo_Data;
import wanda.data.UserPlanBilling_Data;
import wanda.data.UserPlanBilling_Factory;
import wanda.data.UserPlanPreOrder_Data;
import wanda.data.UserPlanPreOrder_Factory;
import wanda.data.UserPlanSubscription_Data;
import wanda.data.UserPlanSubscription_Factory;
import wanda.data.User_Data;
import wanda.data.importers.promos.Plan;
import wanda.servlets.helpers.CreditHelper;
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
        // Pre-orders are keyed by (user, product), so we can no longer find "the" pending order from the user alone.
        // The client echoes back the planCode it used at create time.
        String planCode = req.getParamString("planCode", true);

        req.throwIfErrors();

        Plan P = PlanHelper.getPlan(PlanHelper.getAvailablePlans(C, U), planCode);
        if (P == null)
          throw new NotFoundException("Plan", planCode);
        String productId = P._Plan.getPaymentSystemProductId();

        // The OrderCreate step must have been performed before this "capture" step. So we
        // load the pre-order and make sure the orderId and paymentProvider match.
        UserPlanPreOrder_Data UPPO = UserPlanPreOrder_Factory.lookupByUser(U.getRefnum(), productId);
        if (UPPO.read(C) == false)
          throw new NotFoundException("UserPlanPreOrder", "No pre-order found for user");
        if (UPPO.getOrderId().equals(orderId) == false || UPPO.getPaymentProvider().equalsIgnoreCase(paymentProvider) == false)
          throw new NotFoundException("UserPlanPreOrder", "No pre-order found for order and provider supplied");

        // Idempotency guard: if we have already recorded a PAID billing for this provider order, do NOT capture
        // again. Protects against double-submits and provider retries, which would otherwise charge twice.
        UserPlanBilling_Data UPB = UserPlanBilling_Factory.lookupByOrder(orderId);
        if (UPB.read(C) == true && UPB.isStatusPaid() == true)
          {
            printResponse(res, UPB);
            return;
          }

        // Next, we either create or update the user's plan subscription based on the pre-order
        UserPlanSubscription_Data UPS = getUserPlanSubscription(C, U, UPPO, P);

        UPB = UserPlanBilling_Factory.create(UPS.getRefnum(), U.getRefnum(), UPS.getPlanRefnum(), productId, paymentProvider, UPPO.getCustomId(), orderId, false);
        UPB.setStatus(UserPlanBilling_Data._statusPending);
        ZonedDateTime now = DateTimeUtil.nowUTC();
        UPB.setOrderDt(now);
        // One-time (credit) purchases never date-expire: getExpiryDtFrom() returns null for cycle 'O', and
        // UserBillingView.active treats a null expiry as "still active".
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
            // Only now that we know the payment went through do we retire the previous billing: doing it earlier
            // would revoke a user's still-valid access if the capture failed.
            deactivateCurrentBilling(C, U, productId);
            UPB.setActive(true); // Only paid orders are active, the rest are not.
            if (UPB.write(C) == false) // The billing must be persisted before the ledger can reference it.
              throw new Exception("Cannot activate the plan billing record for user " + U.getRefnum() + " and orderId " + orderId);

            // For pre-paid plans, the purchase grants credits into the user's wallet for that product. Because
            // getUserPlanSubscription() reuses the existing wallet, a top-up ADDS to whatever was left.
            if (P._Plan.isPlanTypeCredits() == true)
              {
                BigDecimal credits = PlanHelper.getCredits(P, UPPO.getCurrency());
                if (credits == null)
                  LOG.error("Plan '" + P._Plan.getCode() + "' is a credit plan but defines no oneTimeCredits for currency '" + UPPO.getCurrency() + "': order " + orderId + " was PAID but granted nothing. This needs a manual adjustment.");
                else
                  CreditHelper.grant(C, UPS, UPB, credits);

                // On top of the pack itself, a user's PROMO may define a one-time signup bonus (Promo.initialCredits).
                // grantSignupBonusIfEligible() is a no-op if the promo defines none, or if this wallet already
                // received its bonus on an earlier purchase -- that guard is what makes it first-purchase-only.
                Promo_Data promo = PlanHelper.getUserPromo(C, U);
                if (promo != null && promo.isNullInitialCredits() == false)
                  CreditHelper.grantSignupBonusIfEligible(C, UPS, UPB, BigDecimal.valueOf(promo.getInitialCredits()));
              }


            PlanHelper.clearUserForPlan(C, req, U, false);
          }

        // we can't fail here anymore. If we failed, the user would be charged but we wouldn't record it.
        if (UPB.write(C) == false)
          {
            LOG.error("Cannot update plan billing record with capture details for user " + U.getRefnum() + " and orderId " + orderId + ", customId " + UPPO.getCustomId() + ". Capture payload: " + ppod.toJsonString());
            EMailSender.sendMailSys(null, null, null, "Capsico Order #" + orderId + " recording failed!!!", "The order with ID " + orderId + " for user " + U.getRefnum() + " was successfully captured but we failed to record the capture details.\n\nYou should check the billing record and update it manually if needed.\n\nThanks.\n\n", true, true);
          }

        // The pre-order is only cleaned up once everything above is safely recorded: deleting it earlier would
        // strand the user mid-payment if the capture call threw.
        UserPlanPreOrder_Factory.delete(C, U.getRefnum(), productId);

        printResponse(res, UPB);
      }

    private static void printResponse(ResponseUtil res, UserPlanBilling_Data UPB)
    throws Exception
      {
        PrintWriter out = res.setContentType(ResponseUtil.ContentType.JSON);
        JSONUtil.startOK(out, '{');
        JSONUtil.print(out, "orderId", true, UPB.getOrderId());
        JSONUtil.print(out, "message", false, UPB.getMessage());
        JSONUtil.print(out, "completed", false, UPB.isStatusPaid());
        JSONUtil.end(out, '}');
      }

    /**
     * Retires the user's currently-active billing for a product, if any. At most one billing can be active per
     * user per product (see UserPlanBilling.UserActive), so the previous one must be stood down before a newly
     * paid one can take its place.
     */
    protected static void deactivateCurrentBilling(Connection C, User_Data U, String productId)
    throws Exception
      {
        UserPlanBilling_Data UPB = UserPlanBilling_Factory.lookupByUserActive(U.getRefnum(), productId);
        if (UPB.read(C) == false) // No active billing for that product: nothing to do.
          return;

        UPB.setActive(false);
        if (UPB.write(C) == false)
          throw new Exception("Cannot terminate the existing plan billing for user " + U.getRefnum() + " and product " + productId + ".");
      }

    private static UserPlanSubscription_Data getUserPlanSubscription(Connection C, User_Data U, UserPlanPreOrder_Data UPPO, Plan P)
    throws Exception
      {
        // Check if we already have an active subscription for this PRODUCT (not just this plan).
        UserPlanSubscription_Data UPS = UserPlanSubscription_Factory.lookupByUserActivePlan(U.getRefnum(), UPPO.getPaymentSystemProductId());
        if (UPS.read(C) == false)
          {
            UPS = createSubscription(C, U, UPPO);
          }
        else if (isStillValid(UPPO, UPS, P) == false)
          {
            // The existing subscription is not valid anymore, we need to end it and create a new one
            UPS.setEndDt(DateTimeUtil.nowLocalDate());
            UPS.setActive(false);
            if (UPS.write(C) == false)
              throw new Exception("Cannot update existing plan subscription for user " + U.getRefnum());

            // Create the new one
            UPS = createSubscription(C, U, UPPO);
          }

        return UPS;
      }

    private static UserPlanSubscription_Data createSubscription(Connection C, User_Data U, UserPlanPreOrder_Data UPPO)
    throws Exception
      {
        UserPlanSubscription_Data UPS = UserPlanSubscription_Factory.create(U.getRefnum(), true, UPPO.getPlanRefnum(), UPPO.getPaymentSystemProductId(), UPPO.getCurrency(), UPPO.getCycle(), DateTimeUtil.nowLocalDate());
        if (UPS.write(C) == false)
          throw new Exception("Cannot create plan subscription for user " + U.getRefnum());
        return UPS;
      }

    /**
     * Whether an existing active subscription can absorb this new order rather than being retired and replaced.
     * <UL>
     * <LI>For CREDIT plans, the subscription is the user's credit wallet for the product: buying ANY tier of the
     * same product (e.g., topping up a $10 wallet with the $50 pack) must reuse it so the remaining balance is
     * carried over rather than stranded. Only the currency has to match.</LI>
     * <LI>For SUBSCRIPTION plans, the tier/currency/cycle must all match, as before.</LI>
     * </UL>
     */
    private static boolean isStillValid(UserPlanPreOrder_Data UPPO, UserPlanSubscription_Data UPS, Plan P)
      {
        if (UPS.getActive() == false || UPS.isNullEndDt() == false)
          return false;

        if (UPS.getCurrency().equals(UPPO.getCurrency()) == false)
          return false;

        if (P._Plan.isPlanTypeCredits() == true)
          return UPS.isCycleOneTime() == true;

        return UPS.getPlanRefnum() == UPPO.getPlanRefnum()
            && UPS.getCycle() == UPPO.getCycle();
      }
  }
