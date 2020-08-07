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

package wanda.servlets.admin.apps;

import java.io.PrintWriter;

import javax.servlet.annotation.WebServlet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import wanda.data.AppUserView_Data;
import wanda.data.AppUserView_Factory;
import wanda.data.User_Data;
import wanda.servlets.helpers.RoleHelper;

import tilda.db.Connection;
import tilda.db.ListResults;
import tilda.utils.json.JSONUtil;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;

@WebServlet("/svc/admin/app/users")
/**
 *
 * @author ldhasson
 *         API to return list of users with access to an app.
 */
public class AppUsersList extends SimpleServlet
  {
    private static final long     serialVersionUID = 627146409221225570L;
    protected static final Logger LOG              = LogManager.getLogger(AppUsersList.class.getName());

    public AppUsersList()
      {
        super(true);
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        throwIfUserInvalidRole(U, RoleHelper.ADMINROLES);

        long appRefnum = Req.getParamLong("appRefnum", true);
        String filter = Req.getParamString("filter", false);
        Req.throwIfErrors();

        PrintWriter Out = Res.setContentType(ResponseUtil.ContentType.JSON);
        ListResults<AppUserView_Data> L = AppUserView_Factory.lookupWhereAppByUser(C, appRefnum, 0, 100);
        JSONUtil.response(Out, "", L);
      }

  }
