package wanda.servlets;

import java.io.PrintWriter;

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import tilda.utils.json.JSONUtil;
import wanda.data.User_Data;
import wanda.servlets.helpers.PayPalHelper;
import wanda.servlets.helpers.PayPalOrderDetails;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.config.PaymentSystem;
import wanda.web.config.Wanda;
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

        PaymentSystem PS = Wanda.getPaymentSystem(paymentProvider, true);
        if (PS == null)
          throw new NotFoundException("Payment provider", paymentProvider);

        PayPalOrderDetails ppod = PayPalHelper.captureOrder(PS, orderId);
        if (ppod == null)
          throw new NotFoundException("Payment provider", paymentProvider);
        
        PrintWriter out = res.setContentType(ResponseUtil.ContentType.JSON);
        JSONUtil.startOK(out, '{');
        JSONUtil.print(out, "orderId", true, orderId);
        JSONUtil.print(out, "status", true, ppod.getFirstCaptureStatus());
        JSONUtil.end(out, '}');
        
      }
  }
