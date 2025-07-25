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

package wanda.web;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.ConnectionPool;
import tilda.db.QueryDetails;
import tilda.utils.AnsiUtil;
import tilda.utils.DurationUtil;
import tilda.utils.SystemValues;
import wanda.web.config.Wanda;


public class SessionFilterNonTransactional implements jakarta.servlet.Filter
  {
    protected static final Logger LOG = LogManager.getLogger(SessionFilterNonTransactional.class.getName());

    @Override
    public void init(FilterConfig arg0)
    throws ServletException
      {
        LOG.info("\n");
        LOG.info("*************************************************************************************************************************************");
        Wanda.autoInit();
        ConnectionPool.autoInit();
        LOG.info("*************************************************************************************************************************************\n\n");
      }

    @Override
    public void destroy()
      {
      }

    @Override
    public void doFilter(ServletRequest servletReq, ServletResponse servletRes, FilterChain chain)
    throws IOException, ServletException
      {
        long T0 = System.nanoTime();
        HttpServletRequest request = (HttpServletRequest) servletReq;
        HttpServletResponse response = (HttpServletResponse) servletRes;

        try
          {
            HttpSession S = SessionUtil.getSession(request);
            Boolean maskedMode = (Boolean) S.getAttribute(SessionUtil.Attributes.MASKING_MODE.name());
            if (maskedMode == null)
              maskedMode = false;
            QueryDetails.setThreadMaskMode_DO_NOT_USE_IN_GENERAL_APP_CODE(maskedMode);
            AuthApiToken apiToken = AuthApiToken.getAuthToken(request);
            
            LOG.info(SessionFilter.getRequestHeaderLogStr(request, null, true, maskedMode, apiToken));
            if (request.getScheme().equals("https") == false)
              {
                LOG.error("The server only accepts HTTPS requests.");
                throw new ServletException("The server only accepts HTTPS requests.");
              }

            // LOG.info("********************************************************************************************************************************************\n");
            response.setHeader("X-Frame-Options", "SAMEORIGIN");
            chain.doFilter(servletReq, servletRes);
            if (response.getStatus() != 200 && response.getStatus() != 302)
              throw new Exception("Servlet error " + response.getStatus());
            // Clear flag from Session, after Success Request
            // So that subsequent requests are not affected
            LOG.info("\n"
            + "   ********************************************************************************************************************************************\n"
            + "   ** " + AnsiUtil.NEGATIVE + "R E Q U E S T   S U C C E E D E D  I N  " + DurationUtil.printDurationMilliSeconds(System.nanoTime() - T0) + AnsiUtil.NEGATIVE_OFF + ": " + request.getRequestURL() + "\n"
            + "   ********************************************************************************************************************************************");
          }
        catch (Throwable T)
          {
            LOG.error(AnsiUtil.NEGATIVE + ">>>>>>>>>>>>>>>" + AnsiUtil.NEGATIVE_OFF + "  R E Q U E S T   F A I L E D  " + AnsiUtil.NEGATIVE + "<<<<<<<<<<<<<<<" + AnsiUtil.NEGATIVE_OFF);
            LOG.error("**    in " + DurationUtil.printDurationMilliSeconds(System.nanoTime() - T0) + ".");
            LOG.catching(T);
            if (T.getCause() != null)
              LOG.catching(T.getCause());
          }
        finally
          {
            LOG.info(SystemValues.NEWLINEx4);
          }
      }



  }
