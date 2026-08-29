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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import wanda.data.Project_Data;
import wanda.data.Project_Factory;
import wanda.data.ProjectRoleView_Factory;

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.exceptions.NotFoundException;

@WebServlet("/svc/wanda/project/details")
public class ProjectDetails extends SimpleServlet
  {
    private static final long     serialVersionUID = 1L;
    protected static final Logger LOG              = LogManager.getLogger(ProjectDetails.class.getName());

    public ProjectDetails()
      {
        super(true, false, true);
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        long refnum = Req.getParamLong("refnum", true);
        String appScope = Req.getParamString("appScope", true);
        Req.throwIfErrors();
        ProjectRoleView_Factory.checkProjectAcl(C, U, refnum, ProjectRoleView_Factory.ProjectRole.READER);

        Project_Data p = Project_Factory.lookupByPrimaryKey(refnum);
        if (p.read(C) == false || p.getAppScope().equals(appScope) == false)
          throw new NotFoundException("Project", "" + refnum, "Project " + refnum + " cannot be found.");

        Res.successJson("", p);
      }
  }
