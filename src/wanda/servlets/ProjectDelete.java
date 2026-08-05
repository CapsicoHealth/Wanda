package wanda.servlets;

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

@WebServlet("/svc/projects/project/delete")
public class ProjectDelete extends SimpleServlet
  {
    private static final long     serialVersionUID = 1L;
    protected static final Logger LOG              = LogManager.getLogger(ProjectDelete.class.getName());

    public ProjectDelete()
      {
        super(true, true, false);
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
