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

import java.util.List;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import wanda.data.User_Data;
import wanda.data.importers.promos.Plan;
import wanda.servlets.helpers.PlanHelper;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;

@WebServlet("/svc/user/plan/list")
public class UserPlanList extends SimpleServlet
  {
    private static final long serialVersionUID = 2358369573367773870L;

    public UserPlanList()
      {
        super(true, false, true);
      }

    @Override
    public void init(ServletConfig Conf)
      {
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
//        if (U == null)
//          {
//            String email = Req.getParamString("email", true);
//            String token = Req.getParamString("token", true);
//            Req.throwIfErrors();
//            U = User_Factory.lookupByEmail(email);
//            if (U.read(C) == false)
//              throw new NotFoundException("User", "User not found.");
//            if (U.checkTokenValidity(token) == false)
//              throw new NotFoundException("User", "User not found.");
//          }

        List<Plan> plans = PlanHelper.getAvailablePlans(C, U);
        Res.successJson("", plans);
      }

  }
