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

import java.io.PrintWriter;

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import tilda.utils.json.JSONUtil;
import wanda.data.User_Data;
import wanda.servlets.helpers.EulaHelper;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;

@WebServlet("/svc/user/eula")
public class EulaRefresher extends SimpleServlet
  {

    private static final long serialVersionUID = -646305078287230041L;

    public EulaRefresher()
      {
        super(false, false, true);
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        String eulaUrl = EulaHelper.getEulaUrl(C, U, null);
        PrintWriter Out = Res.setContentType(ResponseUtil.ContentType.JSON);
        JSONUtil.startOK(Out, '{');
        JSONUtil.print(Out, "eulaUrl", true, eulaUrl);
        JSONUtil.end(Out, '}');
      }

  }
