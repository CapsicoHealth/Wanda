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

import javax.servlet.annotation.WebServlet;

import tilda.db.Connection;
import tilda.db.ListResults;
import tilda.utils.DateTimeUtil;
import wanda.data.AccessLogDailyView_Data;
import wanda.data.AccessLogDailyView_Factory;
import wanda.data.User_Data;
import wanda.servlets.helpers.RoleHelper;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;

@WebServlet("/svc/admin/user/activity/list")
public class UserActivity extends SimpleServlet
  {

    private static final long serialVersionUID = -1745307937763620646L;

    public UserActivity()
      {
        super(true);
      }

    @Override
    protected void justDo(RequestUtil req, ResponseUtil res, Connection C, User_Data U)
    throws Exception
      {
        throwIfUserInvalidRole(U, RoleHelper.ADMINROLES);

        long daysBack = req.getParamLong("daysBack", false);
        if (daysBack < 30 || daysBack > 400)
         daysBack = 90;

        req.throwIfErrors();

        LocalDate tomorrow = DateTimeUtil.nowLocalDate().plusDays(1);
        ListResults<AccessLogDailyView_Data> L = AccessLogDailyView_Factory.lookupWhereDay(C, tomorrow.minusDays(daysBack), tomorrow, 0, 500);
        res.successJson("", L);
      }

  }
