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

import jakarta.servlet.annotation.WebServlet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;
import tilda.db.ListResults;
import tilda.utils.SystemValues;
import tilda.utils.TextUtil;
import wanda.data.AppUserView_Data;
import wanda.data.AppUserView_Factory;
import wanda.data.App_Data;
import wanda.data.App_Factory;
import wanda.data.User_Data;
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
    protected void justDo(RequestUtil req, ResponseUtil res, Connection C, User_Data U)
    throws Exception
      {
        long appRefnum = req.getParamLong("appRefnum", false);
        String appId = req.getParamString("appId", false);
        String searchQuery = req.getParamString("searchQuery", false, "");
        String promoCode = req.getParamString("promoCode", false, "");

        // First get the app;
        App_Data A = null;
        if (TextUtil.isNullOrEmpty(appId) == false && appRefnum != SystemValues.EVIL_VALUE)
          req.addError("appRefnum", "This service cannot be called with both appId and appRefnum. Pick one.");
        else if (TextUtil.isNullOrEmpty(appId) == false)
          {
            A = App_Factory.lookupById(appId);
            if (A.read(C) == false)
             req.addError("appId", "Invalid value");
          }
        else
          {
            A = App_Factory.lookupByPrimaryKey(appRefnum);
            if (A.read(C) == false)
             req.addError("appRefnum", "Invalid value");
          }
        
        req.throwIfErrors();
        
        boolean superAdmin = throwIfUserNotSuperOrAppAdmin(U, A.getId()); // The user must be a super admin or an app admin to see the list of users

        ListResults<AppUserView_Data> L = AppUserView_Factory.getAppUsers(C, U, A, superAdmin == false, promoCode, searchQuery, 0, 100);
        res.successJson("Simple", L);
      }

  }
