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

import wanda.data.ProjectACL_Data;
import wanda.data.ProjectACL_Factory;
import wanda.data.ProjectRoleView_Factory;

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;

@WebServlet("/svc/wanda/project/acl/delete")
public class ProjectACLDelete extends SimpleServlet
  {
    private static final long     serialVersionUID = 1L;
    protected static final Logger LOG              = LogManager.getLogger(ProjectACLDelete.class.getName());

    public ProjectACLDelete()
      {
        super(true, true, true);
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        long projectRefnum = Req.getParamLong("projectRefnum", true);
        long refnum = Req.getParamLong("refnum", true);
        Req.throwIfErrors();
        ProjectRoleView_Factory.checkProjectAcl(C, U, projectRefnum, ProjectRoleView_Factory.ProjectRole.ADMIN);

        ProjectACL_Data acl = ProjectACL_Factory.lookupByPrimaryKey(refnum);
        acl.setDeletedNow();
        if (acl.write(C) == false)
          throw new Exception("Cannot revoke MSL Project ACL entry " + refnum + ".");

        Res.successJson("", acl);
      }
  }
