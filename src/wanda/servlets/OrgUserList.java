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
import wanda.data.UserView_Data;
import wanda.data.UserView_Factory;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;

@WebServlet("/svc/org/user-list")
public class OrgUserList extends SimpleServlet
  {
    private static final long serialVersionUID = 2358369573367773870L;

    public OrgUserList()
      {
        super(true, false, false);
      }

    @Override
    public void init(ServletConfig Conf)
      {
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        int i = U.getEmail().indexOf("@");
        if (i < 0)
          throw new Exception("Invalid email for current user");
        
        String domain = "%"+U.getEmail().substring(i);
        List<UserView_Data> L = UserView_Factory.lookupWhereActive(C, domain, U.getRefnum(), 0, 200);
        Res.successJson("Mini", L);
      }

  }
