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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jakarta.servlet.annotation.WebServlet;

import wanda.data.AppUserView_Data;
import wanda.data.AppUserView_Factory;
import wanda.data.User_Data;
import wanda.web.config.PasswordRule;
import wanda.web.config.Wanda;

import tilda.db.Connection;
import tilda.db.ConnectionPool;
import tilda.utils.json.JSONUtil;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;

@WebServlet("/svc/config")
public class ConfigServlet extends SimpleServlet
  {

    private static final long serialVersionUID = -646305078287230041L;

    public ConfigServlet()
      {
        super(false);
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        PrintWriter Out = Res.setContentType(ResponseUtil.ContentType.JSON);
        List<String> authPassthroughs = new ArrayList<String>();
        Iterator<String> IAuthPassthroughs = Wanda.getAuthPassthroughs();
        while (IAuthPassthroughs.hasNext())
          {
            authPassthroughs.add(IAuthPassthroughs.next());
          }
        String[] authPassthroughsArr = new String[authPassthroughs.size()];
        authPassthroughs.toArray(authPassthroughsArr);
        List<PasswordRule> passwordRules = Wanda.getPasswordRules();
        List<AppUserView_Data> AUVL = U == null ? null : AppUserView_Factory.getUserApps(C, U, U.getRefnum(), 0, -1);

        JSONUtil.startOK(Out, '{');
        JSONUtil.print(Out, "passwordRules", "", true, passwordRules, " ");
        JSONUtil.print(Out, "authPassthroughs", false, authPassthroughsArr);
        Out.println();
        JSONUtil.print(Out, "isMultiTenant", false, ConnectionPool.isMultiTenant());
        Out.println();
        JSONUtil.print(Out, "defaultSsoId", false, Wanda.getDefaultSsoConfigId());
        Out.println();
        JSONUtil.print(Out, "appPath", false, Wanda.getAppPath());
        Out.println();
        JSONUtil.print(Out, "homePagePath", false, Wanda.getHomePagePath());
        Out.println();
        JSONUtil.print(Out, "apps", "", false, AUVL, " ");
        Out.println();
        if (U != null)
          {
            Out.println(", \"currentUser\": { ");
            JSONUtil.print(Out, "person", "", true, U.getUserDetails(), "     ");
            Out.println();
            JSONUtil.print(Out, "user", "", false, U, "     ");
            Out.println();
            Out.println("}");
          }
        JSONUtil.end(Out, '}');
      }

  }
