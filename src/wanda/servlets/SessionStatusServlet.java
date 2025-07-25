/* ===========================================================================
 * Copyright (C) 2017 CapsicoHealth Inc.
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

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.QueryDetails;
import tilda.utils.json.JSONUtil;
import tilda.utils.pairs.StringIntPair;
import wanda.web.AuthApiToken;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SessionFilter;
import wanda.web.SessionStatusImpl;
import wanda.web.SessionUtil;

/**
 * This is a plain servlet to make session status checking as fact and low-overhead as possible, i.e.,
 * avoid transaction management, user checks and other things SimpleServlet does outside of normal acl.
 * 
 * @author Laurent Hasson
 *
 */
@WebServlet("/svcx/session/status")
public class SessionStatusServlet extends jakarta.servlet.http.HttpServlet implements jakarta.servlet.Servlet
  {
    private static final long     serialVersionUID = 1018123535563202342L;
    protected static final Logger LOG              = LogManager.getLogger(SessionStatusServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
      {
        RequestUtil req = new RequestUtil(request);
        ResponseUtil res = new ResponseUtil(response);
        String servletPath = req.getParamString("servletPath", true);

        req.throwIfErrors();

        StringIntPair SIP = req.getSessionStatus().getSessionStatus(servletPath);
        boolean clear = req.getSessionStatus().isSessionStatusMarkedForClear(servletPath);

        HttpSession S = SessionUtil.getSession(request);
        Boolean maskedMode = (Boolean) S.getAttribute(SessionUtil.Attributes.MASKING_MODE.name());
        if (maskedMode == null)
          maskedMode = false;
        QueryDetails.setThreadMaskMode_DO_NOT_USE_IN_GENERAL_APP_CODE(maskedMode);
        
        LOG.debug("Session Attributes: "+req.getSessionAttributes());
        
        try
          {
            AuthApiToken apiToken = AuthApiToken.getAuthToken(request);
            LOG.info("\n"
            + " ============================================================================================================================================================================\n"
            + SessionFilter.getRequestHeaderLogStr(request, null, false, maskedMode, apiToken) + "\n"
            + "   ***  Session status for '" + servletPath + "': " + SIP._V + "% -> " + SIP._N + "\n"
            + " ============================================================================================================================================================================\n"
            );
          }
        catch (Exception e)
          {
            LOG.error("An error occurred\n",e);
          }

        // Enumeration<String> SAN = Req.getSessionNames();
        // while (SAN.hasMoreElements() == true)
        // LOG.debug("SESSION NAME: "+SAN.nextElement());

        PrintWriter Out = res.setContentType(ResponseUtil.ContentType.JSON);
        JSONUtil.startOK(Out, '{');
        JSONUtil.print(Out, "message", true, SIP._N);
        JSONUtil.print(Out, "percent", false, SIP._V);
        if (clear == true)
          JSONUtil.print(Out, "clear", false, clear);
        JSONUtil.end(Out, '}');
        
        if (clear == true)
         SessionStatusImpl.clear(req, servletPath, true); // Clear session status
      }
  }
