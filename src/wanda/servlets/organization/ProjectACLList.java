/* ===========================================================================
 * Copyright (C) 2026 CapsicoHealth Inc.
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

package wanda.servlets.organization;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import wanda.data.ProjectACL_Data;
import wanda.data.ProjectACL_Factory;
import wanda.data.ProjectRoleView_Factory;

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;

@WebServlet("/svc/wanda/project/acl/list")
public class ProjectACLList extends SimpleServlet
  {
    private static final long     serialVersionUID = 1L;
    protected static final Logger LOG              = LogManager.getLogger(ProjectACLList.class.getName());

    public ProjectACLList()
      {
        super(true, false, true);
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        long   projectRefnum = Req.getParamLong  ("projectRefnum", true);
        String orderBy = Req.getParamString("orderBy", false);
        if ("id".equals(orderBy) == false && "recent".equals(orderBy) == false)
         orderBy = "recent";
          
        Req.throwIfErrors();
        ProjectRoleView_Factory.checkProjectAcl(C, U, projectRefnum, ProjectRoleView_Factory.ProjectRole.ADMIN);

        List<ProjectACL_Data> L;
        if ("id".equals(orderBy) == true)
          L = ProjectACL_Factory.lookupWhereProjectActiveByUser(C, projectRefnum, 0, 250);
        else
          L = ProjectACL_Factory.lookupWhereProjectActiveByRecent(C, projectRefnum, 0, 250);

        Res.successJson("", L);
      }
  }
