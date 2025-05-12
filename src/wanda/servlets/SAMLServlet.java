package wanda.servlets;

import java.util.List;
import java.util.Optional;

import org.opensaml.core.config.InitializationService;
import org.pac4j.core.client.Clients;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.context.FrameworkParameters;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.engine.CallbackLogic;
import org.pac4j.core.engine.DefaultCallbackLogic;
import org.pac4j.core.engine.DefaultSecurityLogic;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.exception.http.RedirectionAction;
import org.pac4j.core.util.Pac4jConstants;
import org.pac4j.jee.context.JEEContext;
import org.pac4j.jee.context.session.JEESessionStore;
import org.pac4j.jee.http.adapter.JEEHttpActionAdapter;
import org.pac4j.saml.client.SAML2Client;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import wanda.saml.ConfigSAML;
import wanda.web.config.Wanda;

@WebServlet(urlPatterns = { "/saml-sso", "/saml-callback"
})
public class SAMLServlet extends HttpServlet
  {

    private Config      samlConfig;
    private SAML2Client samlClient;

    @Override
    public void init()
    throws ServletException
      {
        try
          {
            InitializationService.initialize();
            String callbackUrl = Wanda.getHostName() + Wanda.getAppPath() + "/saml-callback";
            samlClient = ConfigSAML.buildSaml2Client("ALV", callbackUrl);
            Clients clients = new Clients(callbackUrl, samlClient);
            samlConfig = new Config(clients);
          }
        catch (Exception e)
          {
            throw new ServletException("Failed to initialize SAML2 client", e);
          }
      }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
    throws ServletException
      {
        try
          {
            if (req.getRequestURI().endsWith("/saml-sso"))
              {

                JEEContext context = new JEEContext(req, res);
                SessionStore sessionStore = new JEESessionStore();

                IndirectClient client = (IndirectClient) samlConfig.getClients().findClient("SAML2Client").get();

                CallContext callContext = new CallContext(context, sessionStore);
                Optional<RedirectionAction> action = client.getRedirectionAction(callContext);

                if (action.isPresent())
                  {
                    JEEHttpActionAdapter.INSTANCE.adapt(action.get(), context);
                  }
                else
                  {
                    res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No redirection action available.");
                  }
              }
            else if (req.getRequestURI().endsWith("/saml-callback"))
              {
                JEEContext context = new JEEContext(req, res);
                SessionStore sessionStore = new JEESessionStore();
                CallContext callContext = new CallContext(context, sessionStore);

                IndirectClient client = (IndirectClient) samlConfig.getClients().findClient("SAML2Client").get();

                try
                  {
                    // Get credentials from the response
                    var credentials = client.getCredentials(callContext).get();

                    // Get user profile from the IdP using those credentials
                    var profile = client.getUserProfile(callContext, credentials).get();

                    // Save profile in session
                    sessionStore.set(context, Pac4jConstants.USER_PROFILES, List.of(profile));

                    // Redirect to post-login page
                    res.sendRedirect("/profile");

                  }
                catch (Exception e)
                  {
                    e.printStackTrace();
                    res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "SSO authentication failed.");
                  }
              }
            else
              {
                res.sendError(HttpServletResponse.SC_NOT_FOUND);
              }
          }
        catch (HttpAction action)
          {
            action.getCode(); // e.g., 302 for redirect — pac4j has already handled the response
          }
        catch (Exception e)
          {
            throw new ServletException("Error during SAML processing", e);
          }
      }
  }
