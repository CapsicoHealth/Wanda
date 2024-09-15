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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.annotation.WebServlet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;
import wanda.data.AppUser_Factory;
import wanda.data.App_Data;
import wanda.data.App_Factory;
import wanda.data.User_Data;
import wanda.servlets.helpers.RoleHelper;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;

@WebServlet("/svc/admin/app/users/update")
/**
 *
 * @author ldhasson
 *         API to return list of users with access to an app.
 */
public class AppUserUpdate extends SimpleServlet
  {
    private static final long     serialVersionUID = 627146409221225570L;
    protected static final Logger LOG              = LogManager.getLogger(AppUserUpdate.class.getName());

    public AppUserUpdate()
      {
        super(true, true);
      }

    protected static long[] toLongArray(List<Long> L)
      {
        long[] ret = new long[L.size()];
        for (int i = 0; i < L.size(); i++)
          {
            ret[i] = L.get(i).longValue();
          }
        return ret;
      }

    @Override
    protected void justDo(RequestUtil req, ResponseUtil res, Connection C, User_Data U)
    throws Exception
      {
        throwIfUserInvalidRole(U, RoleHelper.ADMINROLES);

        long appRefnum = req.getParamLong("appRefnum", true);
        String[] userInfo = req.getParamsString("userInfo", true);

        // First get the app;
        App_Data A = App_Factory.lookupByPrimaryKey(appRefnum);
        if (A.read(C) == false)
          req.addError("appRefnum", "Invalid value");

        req.throwIfErrors();

        List<Long> usersEnabled = new ArrayList<Long>();
        List<Long> usersDisabled = new ArrayList<Long>();
        for (String ui : userInfo)
          {
            String[] parts = ui.split("\\|");
            if (parts[1].equals("1") == true)
              usersEnabled.add(Long.parseLong(parts[0]));
            else
              usersDisabled.add(Long.parseLong(parts[0]));
          }

        if (usersEnabled.isEmpty() == false)
          AppUser_Factory.addUserAccess(C, appRefnum, toLongArray(usersEnabled));
        if (usersDisabled.isEmpty() == false)
          AppUser_Factory.removeUserAccess(C, appRefnum, toLongArray(usersDisabled));

        res.success();
      }

  }
