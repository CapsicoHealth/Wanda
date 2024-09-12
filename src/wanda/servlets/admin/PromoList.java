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
import wanda.data.Promo_Data;
import wanda.data.Promo_Factory;
import wanda.data.User_Data;
import wanda.servlets.helpers.RoleHelper;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;

@WebServlet("/svc/admin/promo/list")
public class PromoList extends SimpleServlet
  {

    private static final long serialVersionUID = -1745307937763620646L;

    public PromoList()
    {
      super(true);
    }

    @Override
    protected void justDo(RequestUtil req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
          throwIfUserInvalidRole(U, RoleHelper.ADMINROLES);

          boolean activeOnly = req.getParamBoolean("activeOnly", false);

          ListResults<Promo_Data> L = activeOnly==true ? Promo_Factory.lookupWhereActive(C, 0, 250) : Promo_Factory.lookupWhereAll(C, 0, 250);
          Res.successJson("", L);
      }

  }
