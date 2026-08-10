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
        super(true, false, false);
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
