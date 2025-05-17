package wanda.servlets;

import org.pac4j.core.exception.http.HttpAction;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tilda.utils.TextUtil;
import wanda.saml.ConfigSAML;
import wanda.web.SessionFilter;

@WebServlet("/saml-sso")
public class SAMLServlet extends HttpServlet
  {
    @Override
    public void init()
      {
        SessionFilter.addMaskedUrlNvp("SAMLResponse");
      }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
    throws ServletException
      {
        try
          {
            ConfigSAML.LOG.info("\n\n\n");
            ConfigSAML.LOG.info(SessionFilter.getRequestHeaderLogStr(req, null, true, true));
            String ssoId = req.getParameter("ssoId");
            if (TextUtil.isNullOrEmpty(ssoId) == true)
              throw new ServletException("Missing ssoId or idp parameter");
            ConfigSAML.processRedirect(req, res, ssoId);
          }
        catch (HttpAction action)
          {
            action.getCode(); // e.g., 302 for redirect — pac4j has already handled the response
            ConfigSAML.LOG.debug("Error during SAML authentication with action code: " + action.getCode());
          }
        catch (Exception e)
          {
            ConfigSAML.LOG.debug("Error during SAML processing: " + e.getMessage() + "\n", e);
            throw new ServletException("Error during SAML processing", e);
          }
        finally
          {
            ConfigSAML.LOG.info("\n\n\n");
          }
      }
  }
