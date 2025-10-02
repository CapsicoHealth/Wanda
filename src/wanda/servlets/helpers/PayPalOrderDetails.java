package wanda.servlets.helpers;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PayPalOrderDetails
  {

    public String                id;
    public String                intent;
    public String                status;

    @SerializedName("purchase_units")
    public List<PurchaseUnit>    purchaseUnits;

    public Payer                 payer;
    public List<LinkDescription> links;

    // Convenience helpers
    public Capture getFirstCapture()
      {
        if (purchaseUnits == null || purchaseUnits.isEmpty())
          return null;
        PurchaseUnit pu = purchaseUnits.get(0);
        if (pu.payments == null || pu.payments.captures == null || pu.payments.captures.isEmpty())
          return null;
        return pu.payments.captures.get(0);
      }

    public String getFirstCaptureId()
      {
        Capture c = getFirstCapture();
        return c == null ? null : c.id;
      }

    public String getFirstCaptureStatus()
      {
        Capture c = getFirstCapture();
        return c == null ? null : c.status;
      }

    public Money getFirstCaptureAmount()
      {
        Capture c = getFirstCapture();
        return c == null ? null : c.amount;
      }

    public boolean isCompleted()
      {
        return "COMPLETED".equalsIgnoreCase(status);
      }

    // ==== Nested static classes ====

    public static class PurchaseUnit
      {
        @SerializedName("reference_id")
        public String   referenceId;

        @SerializedName("custom_id")
        public String   customId;

        public Money    amount;
        public Payments payments;
      }

    public static class Payments
      {
        public List<Capture> captures;
      }

    public static class Capture
      {
        public String                    id;
        public String                    status;
        public Money                     amount;

        @SerializedName("final_capture")
        public Boolean                   finalCapture;

        @SerializedName("seller_receivable_breakdown")
        public SellerReceivableBreakdown sellerReceivableBreakdown;

        @SerializedName("create_time")
        public String                    createTime;

        @SerializedName("update_time")
        public String                    updateTime;

        public List<LinkDescription>     links;
      }

    public static class SellerReceivableBreakdown
      {
        @SerializedName("gross_amount")
        public Money grossAmount;

        @SerializedName("paypal_fee")
        public Money paypalFee;

        @SerializedName("net_amount")
        public Money netAmount;
      }

    public static class Money
      {
        @SerializedName("currency_code")
        public String currencyCode;
        public String value;
      }

    public static class LinkDescription
      {
        public String href;
        public String rel;
        public String method;
      }

    public static class Payer
      {
        public PayerName name;

        @SerializedName("email_address")
        public String    emailAddress;

        @SerializedName("payer_id")
        public String    payerId;
      }

    public static class PayerName
      {
        @SerializedName("given_name")
        public String givenName;

        @SerializedName("surname")
        public String surname;
      }
  }
