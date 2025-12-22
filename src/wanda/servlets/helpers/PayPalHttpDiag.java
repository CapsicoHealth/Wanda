package wanda.servlets.helpers;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class PayPalHttpDiag
  {

    // ProxySelector that always disables proxy usage
    private static final ProxySelector NO_PROXY = new ProxySelector()
      {
        @Override
        public List<Proxy> select(URI uri)
          {
            return List.of(Proxy.NO_PROXY);
          }

        @Override
        public void connectFailed(URI uri, SocketAddress sa, IOException ioe)
          {
          }
      };

    private static HttpClient buildClient()
      {
        return HttpClient.newBuilder()
        .proxy(NO_PROXY) // hard-disable proxies
        .connectTimeout(Duration.ofSeconds(15))
        .version(HttpClient.Version.HTTP_1_1) // keep simple
//        .sslParameters(new SSLParameters(new String[] { "TLSv1.2"
//        }))
        .build();
      }

    private static void logProxyDecision()
      {
        var uri = URI.create("https://api-m.sandbox.paypal.com/");
        var def = ProxySelector.getDefault();
        if (def != null)
          {
            System.out.println("Default ProxySelector decision(s):");
            for (var p : def.select(uri))
              System.out.println("  " + p);
          }
        else
          {
            System.out.println("Default ProxySelector is null.");
          }
      }

    public static void main(String[] args)
    throws Exception
      {
        String url = "https://api-m.sandbox.paypal.com/v1/oauth2/token";
        logProxyDecision();

        // Manual DNS timing (again)
        long dnsT0 = System.nanoTime();
        var addrs = InetAddress.getAllByName("api-m.sandbox.paypal.com");
        long dnsMs = (System.nanoTime() - dnsT0) / 1_000_000;
        System.out.println("DNS (manual) " + dnsMs + " ms ->");
        for (var a : addrs)
          System.out.println("  " + a.getHostAddress());

        HttpClient client = buildClient();

        String bogusBasic = "Basic ZHVtbXk6ZHVtbXk="; // dummy to provoke 401 quickly
        HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(Duration.ofSeconds(25))
        .header("Authorization", bogusBasic)
        .header("Accept", "application/json")
        .header("Accept-Language", "en_US")
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
        .build();

        long t0 = System.nanoTime();
        try
          {
            System.out.println("Sending request to " + url);
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            long totalMs = (System.nanoTime() - t0) / 1_000_000;
            System.out.println("Status=" + resp.statusCode() + " totalMs=" + totalMs);
            System.out.println("Body (truncated): " +
            (resp.body() == null ? "<null>" : resp.body().substring(0, Math.min(200, resp.body().length()))));
          }
        catch (HttpConnectTimeoutException cte)
          {
            long totalMs = (System.nanoTime() - t0) / 1_000_000;
            System.out.println("Connect timeout after " + totalMs + " ms: " + cte);
          }
      }
  }
