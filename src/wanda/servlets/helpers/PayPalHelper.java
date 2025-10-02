
package wanda.servlets.helpers;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import tilda.utils.json.JSONPrinter;
import tilda.utils.json.JSONUtil;
import wanda.web.config.PaymentSystem;

public class PayPalHelper
  {
    static final Logger             LOG               = LogManager.getLogger(PayPalHelper.class.getName());

    private static final HttpClient _HTTP_CLIENT_POOL = HttpClient.newBuilder()
    // .executor(Executors.newFixedThreadPool(25))
    .connectTimeout(Duration.ofSeconds(10))
    .version(HttpClient.Version.HTTP_2)
    .build();

    private static String base(PaymentSystem PS)
      {
        return PS._sandbox == true ? "https://api-m.sandbox.paypal.com" : "https://api-m.paypal.com";
      }

    private static final Object TOKEN_LOCK            = new Object();
    private static String       _cachedToken          = null;
    private static long         _cachedExpiryEpochMs;                // absolute time in ms

    private static final long   _EXPIRY_LIMIT_SECONDS = 60 * 15;     // 15 minutes

    /*
     * private static void logSecretDiagnostics(String clientId, String secret)
     * {
     * LOG.debug("clientId length={} secret length={}", clientId.length(), secret.length());
     * StringBuilder sb = new StringBuilder();
     * for (int i = 0; i < secret.length(); i++)
     * {
     * char c = secret.charAt(i);
     * if (c < 32 || c > 126)
     * {
     * sb.append(String.format("\\u%04x(at %d) ", (int) c, i));
     * }
     * }
     * if (sb.length() > 0)
     * LOG.warn("Secret contains non-printable chars: {}", sb.toString());
     * }
     */

    public static String getAccessToken(PaymentSystem PS)
    throws IOException, InterruptedException
      {
        long now = System.currentTimeMillis();
        // Reuse if valid for at least another 15 minutes
        if (_cachedToken != null && now < _cachedExpiryEpochMs)
          return _cachedToken;

        // logSecretDiagnostics(PS._clientId, PS._secret);

        synchronized (TOKEN_LOCK)
          {
            // Retry within the synchronize to avoid overlaps
            // Reuse if valid for at least another 15 minutes
            if (_cachedToken != null && now < _cachedExpiryEpochMs)
              return _cachedToken;

            String basicRaw = PS._clientId + ":" + PS._secret;
            String basic = Base64.getEncoder().encodeToString(basicRaw.getBytes(StandardCharsets.UTF_8));
//            LOG.debug("Auth header Base64 length={}", basic.length());

            var req = HttpRequest.newBuilder()
            .uri(URI.create(base(PS) + "/v1/oauth2/token"))
            .timeout(Duration.ofSeconds(15))
            .header("Authorization", "Basic " + basic)
            .header("Accept", "application/json")
            .header("Accept-Language", "en_US")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .version(HttpClient.Version.HTTP_1_1)
            .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
            .build();
            PlanHelper.LOG.debug("Calling Paypal Token url: " + req.uri());
            HttpResponse<String> resp = _HTTP_CLIENT_POOL.send(req, HttpResponse.BodyHandlers.ofString());
            LOG.debug("PayPal token (status " + resp.statusCode() + "): " + resp.body());
            if (resp.statusCode() != 200)
              throw new IOException("Cannot get a PayPal token: status " + resp.statusCode());
            JsonObject obj = JSONUtil.fromJSONObj(resp.body());
            JsonElement accessToken = obj.get("access_token");
            JsonElement expiresIn = obj.get("expires_in");
            int expiresInSeconds = expiresIn.isJsonNull() == true ? 0 : expiresIn.getAsInt();
            if (expiresInSeconds >= _EXPIRY_LIMIT_SECONDS) // at least 15mn before actual expiry
              expiresInSeconds -= _EXPIRY_LIMIT_SECONDS;
            _cachedExpiryEpochMs = now + expiresInSeconds * 1000L;
            _cachedToken = accessToken.isJsonNull() == true ? null : accessToken.getAsString();
            return _cachedToken;
          }
      }

    /**
     * Returns an Order ID
     * 
     * @param PS
     * @param customId
     * @param currency
     * @param value
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static String createOrder(PaymentSystem PS, String customId, String currency, BigDecimal value)
    throws Exception
      {
        JSONPrinter json = new JSONPrinter();
        json.addElement("intent", "CAPTURE");
        json.addArrayStart("purchase_units");
        json.addArrayElementStart();
        json.addElement("reference_id", "PU1");
        json.addElement("custom_id", customId);
        json.addElementStart("amount");
        json.addElement("currency_code", currency);
        json.addElement("value", value, 2);
        json.addElementClose("amount");
        json.addArrayElementClose();
        json.addArrayClose("purchase_units");
        json.addElementStart("application_context");
        json.addElement("shipping_preference", "NO_SHIPPING");
        json.addElement("user_action", "PAY_NOW");
        json.addElementClose("application_context");
        
        String jsonStr = json.printRaw();
        LOG.debug(jsonStr);

        var req = HttpRequest.newBuilder()
        .uri(URI.create(base(PS) + "/v2/checkout/orders"))
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer " + getAccessToken(PS))
        .POST(HttpRequest.BodyPublishers.ofString(jsonStr))
        .build();
        PlanHelper.LOG.debug("Calling Paypal checkout url: " + req.uri());
        HttpResponse<String> resp = _HTTP_CLIENT_POOL.send(req, HttpResponse.BodyHandlers.ofString());
        LOG.debug("PayPal Create Order (status " + resp.statusCode() + "): " + resp.body());
        if (resp.statusCode() != 201) // 201 Created
          throw new IOException("Create order failed with status " + resp.statusCode());
        JsonObject obj = JSONUtil.fromJSONObj(resp.body());
        JsonElement e = obj.get("id");
        return e.isJsonNull() == true ? null : e.getAsString();
      }

    public static PayPalOrderDetails captureOrder(PaymentSystem PS, String orderId)
    throws IOException, InterruptedException
      {
        var req = HttpRequest.newBuilder()
        .uri(URI.create(base(PS) + "/v2/checkout/orders/" + orderId + "/capture"))
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer " + getAccessToken(PS))
        .POST(HttpRequest.BodyPublishers.ofString("{}"))
        .build();
        PlanHelper.LOG.debug("Calling Paypal capture url: " + req.uri());
        HttpResponse<String> resp = _HTTP_CLIENT_POOL.send(req, HttpResponse.BodyHandlers.ofString());
        LOG.debug("PayPal Capture Order (status " + resp.statusCode() + "):\n" + resp.body() + "\n");
        if (resp.statusCode() != 201) // 201 Created
          throw new IOException("PayPal Capture Order failed with status " + resp.statusCode());

        return new Gson().fromJson(resp.body(), PayPalOrderDetails.class);
      }

/*
    public static void main(String[] args)
    throws Exception
      {
        try
          {
            PaymentSystem PS = new PaymentSystem();
            PS._sandbox = true;
            PS._clientId = "xxx"; // PUT YOURS HERE
            PS._secret = "xxx";
            String token = getAccessToken(PS);
            System.out.println("Token: " + token);
          }
        catch (Exception E)
          {
            E.printStackTrace();
          }
        LOG.info("Done");
      }
*/

    /*
     * public static void main(String[] args)
     * throws Exception
     * {
     * long dnsT0 = System.nanoTime();
     * var addrs = java.net.InetAddress.getAllByName("api-m.sandbox.paypal.com");
     * System.out.println("DNS resolved in " + (System.nanoTime() - dnsT0) / 1_000_000 + " ms:");
     * for (var a : addrs)
     * System.out.println("  " + a);
     * try (var s = javax.net.ssl.SSLSocketFactory.getDefault().createSocket())
     * {
     * long t0 = System.nanoTime();
     * s.connect(new java.net.InetSocketAddress("api-m.sandbox.paypal.com", 443), 15000);
     * System.out.println("TCP+TLS connected in " + (System.nanoTime() - t0) / 1_000_000 + " ms");
     * }
     * }
     */

  }
