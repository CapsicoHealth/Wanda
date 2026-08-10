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
        super(true, true, false);
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
