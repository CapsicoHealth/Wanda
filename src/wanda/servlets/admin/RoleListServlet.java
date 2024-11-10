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

import javax.servlet.annotation.WebServlet;

import tilda.db.Connection;
import tilda.db.ListResults;
import tilda.utils.SystemValues;
import wanda.data.RoleView_Data;
import wanda.data.RoleView_Factory;
import wanda.data.User_Data;
import wanda.servlets.helpers.RoleHelper;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;

@WebServlet("/svc/admin/roles")
public class RoleListServlet extends SimpleServlet
  {

    private static final long serialVersionUID = -1745307937763620646L;

    public RoleListServlet()
      {
        super(true);
      }

    @Override
    protected void justDo(RequestUtil req, ResponseUtil res, Connection C, User_Data U)
    throws Exception
      {
        throwIfUserInvalidRole(U, RoleHelper.ADMINROLES);

        int admin = req.getParamInt("admin", false);

        if (admin != 0 && admin != 1 && admin != SystemValues.EVIL_VALUE)
          req.addError("excludeAdmin", "Invalid API call: 'excludeAdmin' must be either 0 or 1 if passed.");

        req.throwIfErrors();
        
        ListResults<RoleView_Data> L = admin == SystemValues.EVIL_VALUE ? RoleView_Factory.lookupWhereAll(C, 0, 1000)
                                                                        : RoleView_Factory.lookupWhereAdmin(C, admin==1, 0, 1000);;
        res.successJson("", L);
      }

  }
