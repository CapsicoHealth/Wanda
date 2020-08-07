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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.utils.FileUtil;
import tilda.utils.HttpStatus;
import tilda.utils.TextUtil;

@WebFilter("/*")
public class WhiteListFilter implements javax.servlet.Filter
  {
    protected static final Logger  LOG         = LogManager.getLogger(WhiteListFilter.class.getName());

    protected static List<Pattern> _WHITE_LIST = new ArrayList<Pattern>();
    protected static List<Pattern> _BLACK_LIST = new ArrayList<Pattern>();

    @Override
    public void init(FilterConfig arg0)
    throws ServletException
      {
        LOG.info("");
        LOG.info("");
        LOG.info("*************************************************************************************************************************************");
        LOG.info("*** Initializing the white-list configuration");
        _WHITE_LIST = ReadListFile(".white-list.txt");
        LOG.info("*** Initializing the black-list configuration");
        _BLACK_LIST = ReadListFile(".black-list.txt");
        LOG.info("*************************************************************************************************************************************");
        LOG.info("");
        LOG.info("");
      }

    private List<Pattern> ReadListFile(String ResourceName)
    throws ServletException
      {
        List<Pattern> L = new ArrayList<Pattern>();
        InputStream In = FileUtil.getResourceAsStream(ResourceName);
        if (In != null)
          try
            {
              BufferedReader R = new BufferedReader(new InputStreamReader(In));
              String Str;
              while ((Str = R.readLine()) != null)
                {
                  if (TextUtil.isNullOrEmpty(Str) == true)
                    continue;
                  Str = Str.trim();
                  if (Str.startsWith("//") == true)
                    continue;
                  L.add(Pattern.compile(Str));
                  LOG.info("***     - " + Str);
                }
              if (L.isEmpty() == true)
                LOG.info("*** A " + ResourceName + " file was found in the classpath, but it didn't contain any active entry.");
            }
          catch (IOException E)
            {
              throw new ServletException("Failed initializing the " + ResourceName + " configuration\n", E);
            }
        else
          {
            LOG.info("*** No " + ResourceName + " file in the classpath.");
          }
        return L;
      }

    @Override
    public void destroy()
      {
      }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
    throws IOException, ServletException
      {
        HttpServletRequest Request = (HttpServletRequest) req;
        HttpServletResponse Response = (HttpServletResponse) res;

        String X = Request.getContextPath() + Request.getServletPath();
        for (Pattern P : _BLACK_LIST)
          {
            Matcher M = P.matcher(X);
            if (M.find() == true)
              {
                LOG.error("The URL " + X + " was blocked by the black-list entry " + P.pattern());
                Response.sendError(HttpStatus.ResourceNotFound._Code, "Invalid URL");
                throw new ServletException("Invalid URL");
              }
          }
        for (Pattern P : _WHITE_LIST)
          {
            Matcher M = P.matcher(X);
            if (M.find() == true)
              {
                LOG.info("The URL " + X + " was allowed by the white-list entry " + P.pattern());
                chain.doFilter(req, res);
                return;
              }
          }
        LOG.error("The URL " + X + " was blocked by the white-list as no entry was found to match th eincoming URL.");
        Response.sendError(HttpStatus.ResourceNotFound._Code, "Invalid URL");
        throw new ServletException("Invalid URL");
      }
  }
