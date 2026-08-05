package wanda.servlets;

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

@WebServlet("/svc/projects/project/acl/list")
public class ProjectACLList extends SimpleServlet
  {
    private static final long     serialVersionUID = 1L;
    protected static final Logger LOG              = LogManager.getLogger(ProjectACLList.class.getName());

    public ProjectACLList()
      {
        super(true, false, false);
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
