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

import tilda.db.Connection;
import tilda.db.ListResults;
import tilda.utils.SystemValues;
import tilda.utils.json.JSONUtil;
import wanda.data.AppConfig_Data;
import wanda.data.AppConfig_Factory;
import wanda.data.AppUserView_Data;
import wanda.data.AppUserView_Factory;
import wanda.data.AppView_Data;
import wanda.data.AppView_Factory;
import wanda.data.User_Data;
import wanda.servlets.helpers.RoleHelper;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.config.WebBasics;

/**
*
* @author mohan
* API to return list of Tenants to which logged in user has access.
*/

@WebServlet("/svc/admin/apps")
public class AppListServlet extends SimpleServlet
  {
    private static final long serialVersionUID = 5942786964807123071L;

    public AppListServlet()
      {
        super(true);
      }

    @Override
    protected void justDo(RequestUtil req, ResponseUtil res, Connection C, User_Data U)
    throws Exception
      {
        throwIfUserInvalidRole(U, RoleHelper.ADMINROLES);
        
        long refnum=req.getParamLong("refnum", false);
        int active=req.getParamInt("active", false);
        long userRefnum=req.getParamLong("userRefnum", false);

        if (active != 0 && active != 1 && active != SystemValues.EVIL_VALUE)
          req.addError("active", "Invalid API call: active must be either 0 or 1 if passed.");
        if (refnum != SystemValues.EVIL_VALUE && userRefnum != SystemValues.EVIL_VALUE)
          req.addError("userRefnum", "Invalid API call: both userRefnum and refnum were passed. Cannot handle user-level and app-level request at the same time.");
        if (refnum != SystemValues.EVIL_VALUE && active == SystemValues.EVIL_VALUE)
          req.addError("refnum", "Invalid API call: refnum was passed without the active flag.");          

        req.throwIfErrors();

        if (refnum != SystemValues.EVIL_VALUE)
          {
            AppConfig_Data AC = AppConfig_Factory.lookupByAppHost(refnum, WebBasics.getHostName());
            AC.setActive(active==1); // active must be 1 or 0 at this point.
            if (AC.write(C) == false)
             req.addError("refnum", "App '"+refnum+"' cannot be found.");
          }
        
        PrintWriter Out = res.setContentType(ResponseUtil.ContentType.JSON);

        // This can work for an front end that manages apps and sets them active/inactive, or a UI that 
        // sets app ACL for specific users, or gets a list of all apps. In the first case below, we have
        // a user, so we assume we are doing user-level management and return the results from the appuser
        // view. for the second case, we have no user, so assume a "blank" (i.e., no ACL) list of all apps.
        if (userRefnum != SystemValues.EVIL_VALUE)
          {
            ListResults<AppUserView_Data> apps = AppUserView_Factory.getUserApps(C, U, userRefnum, 0, 250);
            JSONUtil.response(Out, "", apps);
          }
        else
          {
            ListResults<AppView_Data> apps = active == SystemValues.EVIL_VALUE ? AppView_Factory.lookupWhereAll(C, WebBasics.getHostName(), 0, 250) 
                                                                               : AppView_Factory.lookupWhereActive(C, WebBasics.getHostName(), active==1, 0, 250);
            JSONUtil.response(Out, "", apps);
          }
      }
  }
