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
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;

import wanda.data.User_Data;

/**
 * Servlet implementation class Login
 */
@WebServlet("/svc/Logout")
public class Logout extends SimpleServlet
  {
    protected static final Logger LOG = LogManager.getLogger(Logout.class.getName());

    private static final long serialVersionUID = 7833614578489016882L;

    /**
     * Default constructor.
     */
    public Logout()
      {
        super(false);
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
      throws Exception
      {
        Req.removeSessionUser();
        Res.Success();        
      }

  }
