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

package wanda.servlets.admin;

import java.time.LocalDate;
import java.util.List;

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import tilda.utils.CollectionUtil;
import tilda.utils.DateTimeUtil;
import tilda.utils.json.JSONPrinter;
import wanda.data.AccessLog_Factory;
import wanda.data.User_Data;
import wanda.servlets.helpers.RoleHelper;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;

@WebServlet("/svc/admin/perf/servlet/list")
public class ServletList extends SimpleServlet
  {

    private static final long serialVersionUID = -1745307937763620646L;

    public ServletList()
      {
        super(true);
      }

    @Override
    protected void justDo(RequestUtil req, ResponseUtil res, Connection C, User_Data U)
    throws Exception
      {
        throwIfUserInvalidRole(U, RoleHelper.ADMINROLES);

        req.throwIfErrors();

        // Look back 90 days to get the set of active servlets
        LocalDate tomorrow = DateTimeUtil.nowLocalDate().plusDays(1);
        List<String> L = AccessLog_Factory.getDistinctServlets(C, tomorrow.minusDays(90), tomorrow, 0, 500);

        JSONPrinter j = new JSONPrinter();
        j.addElement("servlets", CollectionUtil.toStringArray(L));
        
        res.successJson(j);
      }

  }
