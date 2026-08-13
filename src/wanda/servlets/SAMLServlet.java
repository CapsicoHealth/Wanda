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

import org.pac4j.core.exception.http.HttpAction;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tilda.utils.TextUtil;
import wanda.saml.ConfigSAML;
import wanda.web.AuthApiToken;
import wanda.web.SessionFilter;

@WebServlet("/svcx/saml-sso")
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
            AuthApiToken apiToken = AuthApiToken.getAuthToken(req);
            ConfigSAML.LOG.info("\n\n\n");
            ConfigSAML.LOG.info(SessionFilter.getRequestHeaderLogStr(req, null, null, true, true, apiToken));
            String ssoId = req.getParameter("ssoId");
            String returnUrl = req.getParameter("returnUrl");
            if (TextUtil.isNullOrEmpty(ssoId) == true)
              throw new ServletException("Missing ssoId parameter");
            ConfigSAML.processRedirect(req, res, ssoId, returnUrl);
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
            ConfigSAML.LOG.info("\n\n");
          }
      }
  }
