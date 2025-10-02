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

        PaymentSystem PS = Wanda.getPaymentSystem(paymentProvider, true);
        if (PS == null)
          throw new NotFoundException("Payment provider", paymentProvider);

        PrintWriter out = res.setContentType(ResponseUtil.ContentType.JSON);
        JSONUtil.startOK(out, '{');
        JSONUtil.print(out, "id", true, PS._id);
        JSONUtil.print(out, "clientId", false, PS._clientId);
        JSONUtil.print(out, "sandbox", false, PS._sandbox);
        JSONUtil.end(out, '}');
      }
  }
