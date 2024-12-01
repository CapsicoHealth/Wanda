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

import javax.servlet.annotation.WebServlet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tilda.db.Connection;
import tilda.utils.TextUtil;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.config.WebBasics;
import wanda.data.User_Data;

/**
 * Servlet implementation class Login
 */
@WebServlet("/svc/Logout")
public class Logout extends SimpleServlet
  {
    protected static final Logger LOG              = LogManager.getLogger(Logout.class.getName());

    private static final long     serialVersionUID = 7833614578489016882L;

    /**
     * Default constructor.
     */
    public Logout()
      {
        super(false);
      }

    protected static String _DEFAULT_REDIRECT_URL = WebBasics.getHostName() + WebBasics.getAppPath() + WebBasics.getHomePagePath();

    @Override
    protected void justDo(RequestUtil req, ResponseUtil res, Connection C, User_Data U)
    throws Exception
      {
        boolean redirect = req.getParamBoolean("redirect", false);
        String redirectUrl = req.getParamString("redirectUrl", false);
        
        if (redirect == false && TextUtil.isNullOrEmpty(redirectUrl) == false)
          req.addError("redirect", "Parameter redirect cannot be false if a redirectUrl is supplied.");
        
        req.throwIfErrors();

        req.removeSessionUser();

        if (redirect == true)
          res.sendRedirect(TextUtil.isNullOrEmpty(redirectUrl) == true ? _DEFAULT_REDIRECT_URL : redirectUrl);
        else
          res.success();
      }

  }
