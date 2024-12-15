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

import jakarta.servlet.annotation.WebServlet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;
import tilda.db.ListResults;
import tilda.utils.json.JSONUtil;
import wanda.data.AppUserView_Data;
import wanda.data.AppUserView_Factory;
import wanda.data.AppView_Data;
import wanda.data.AppView_Factory;
import wanda.data.User_Data;
import wanda.data.User_Factory;
import wanda.servlets.helpers.RoleHelper;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.config.WebBasics;
import wanda.web.exceptions.NotFoundException;

@WebServlet("/svc/admin/users/apps")
/**
 *
 * @author mohan
 * API to return list of Tenants to which logged in user has access.
 */
public class UserAppList extends SimpleServlet
  {
    /**
   * 
   */
    private static final long serialVersionUID = -628664735749111388L;
    protected static final Logger LOG = LogManager.getLogger(UserAppList.class.getName());


    /**
     * Default constructor.
     */
    public UserAppList()
      {
        super(true);
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
      throws Exception
      {
        throwIfUserInvalidRole(U, RoleHelper.ADMINROLES);

        long UserRefnum = Req.getParamLong("userRefnum", true);
        Req.throwIfErrors();
        User_Data User = User_Factory.lookupByPrimaryKey(UserRefnum);
        if(User.read(C) == false)
          {
            throw new NotFoundException("User", UserRefnum);
          }

        PrintWriter Out = Res.setContentType(ResponseUtil.ContentType.JSON);
        if(User.hasRoles(RoleHelper.SUPERADMIN) == true)
          {
            ListResults<AppView_Data> apps = AppView_Factory.lookupWhereAll(C, WebBasics.getHostName(), 0, 1000);
            JSONUtil.response(Out, "", apps);
          }
        else
          {
            ListResults<AppUserView_Data> list = AppUserView_Factory.lookupWhereUserByActiveApp(C, WebBasics.getHostName(), UserRefnum, 0, 1000);
            JSONUtil.response(Out, "", list);            
          }
      }

  }
