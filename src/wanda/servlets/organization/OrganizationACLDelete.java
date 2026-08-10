package wanda.servlets.organization;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import wanda.data.OrganizationACL_Data;
import wanda.data.OrganizationACL_Factory;
import wanda.data.OrganizationRoleView_Factory;

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;

@WebServlet("/svc/wanda/organizations/organization/acl/delete")
public class OrganizationACLDelete extends SimpleServlet
  {
    private static final long     serialVersionUID = 1L;
    protected static final Logger LOG              = LogManager.getLogger(OrganizationACLDelete.class.getName());

    public OrganizationACLDelete()
      {
        super(true, true, false);
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        long organizationRefnum = Req.getParamLong("organizationRefnum", true);
        long refnum = Req.getParamLong("refnum", true);
        Req.throwIfErrors();
        OrganizationRoleView_Factory.checkOrganizationAcl(C, U, organizationRefnum, OrganizationRoleView_Factory.OrganizationRole.ADMIN);

        OrganizationACL_Data acl = OrganizationACL_Factory.lookupByPrimaryKey(refnum);
        acl.setDeletedNow();
        if (acl.write(C) == false)
          throw new Exception("Cannot revoke Organization ACL entry " + refnum + ".");

        Res.successJson("", acl);
      }
  }
