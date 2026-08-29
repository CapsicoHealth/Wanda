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

import wanda.data.ProjectRoleView_Factory;
import wanda.data.Project_Data;
import wanda.data.Project_Factory;

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import tilda.utils.DateTimeUtil;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.exceptions.NotFoundException;

@WebServlet("/svc/wanda/project/delete")
public class ProjectDelete extends SimpleServlet
  {
    private static final long     serialVersionUID = 1L;
    protected static final Logger LOG              = LogManager.getLogger(ProjectDelete.class.getName());

    public ProjectDelete()
      {
        super(true, true, true);
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        long refnum = Req.getParamLong("refnum", true);
        String deleteMode = Req.getParamString("deleteMode", false);
        if (deleteMode == null)
          deleteMode = "soft";
        if (!deleteMode.equalsIgnoreCase("soft") && !deleteMode.equalsIgnoreCase("hard") && !deleteMode.equalsIgnoreCase("undelete"))
          Req.addError("deleteMode", "Invalid deleteMode. Allowed values: soft, hard, undelete.");
        Req.throwIfErrors();
        ProjectRoleView_Factory.checkProjectAcl(C, U, refnum, ProjectRoleView_Factory.ProjectRole.OWNER);

        Project_Data p = Project_Factory.lookupByPrimaryKey(refnum);
        if (p.read(C) == false)
          throw new NotFoundException("Project", "" + refnum, "Project " + refnum + " cannot be found.");

        if (deleteMode.equalsIgnoreCase("hard") == true)
          {
            if (p.isNullDeleted() == true)
              throw new Exception("Cannot hard-delete project " + refnum + ": it must be soft-deleted first.");
            p.setHardDeletedNow();
            p.setTitle(p.getTitle() + " [del " + DateTimeUtil.printDateTimeSuperCompact(DateTimeUtil.nowLocal()) + "]");
          }
        else
          {
            ProjectRoleView_Factory.checkProjectAcl(C, U, refnum, ProjectRoleView_Factory.ProjectRole.ADMIN);
            if (deleteMode.equalsIgnoreCase("undelete") == true)
              p.setNullDeleted();
            else
              p.setDeletedNow();
          }

        if (p.write(C) == false)
          throw new Exception("Cannot '" + deleteMode + "' project " + refnum + " for unknown reasons.");

        Res.successJson("", p);
      }
  }
