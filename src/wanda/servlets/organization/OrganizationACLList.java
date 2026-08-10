package wanda.servlets.organization;

import java.util.List;

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

@WebServlet("/svc/wanda/organizations/organization/acl/list")
public class OrganizationACLList extends SimpleServlet
  {
    private static final long     serialVersionUID = 1L;
    protected static final Logger LOG              = LogManager.getLogger(OrganizationACLList.class.getName());

    public OrganizationACLList()
      {
        super(true, false, false);
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        long   organizationRefnum = Req.getParamLong  ("organizationRefnum", true);
        String orderBy = Req.getParamString("orderBy", false);
        if ("id".equals(orderBy) == false && "recent".equals(orderBy) == false)
         orderBy = "recent";

        Req.throwIfErrors();
        OrganizationRoleView_Factory.checkOrganizationAcl(C, U, organizationRefnum, OrganizationRoleView_Factory.OrganizationRole.ADMIN);

        List<OrganizationACL_Data> L;
        if ("id".equals(orderBy) == true)
          L = OrganizationACL_Factory.lookupWhereOrganizationActiveByUser(C, organizationRefnum, 0, 250);
        else
          L = OrganizationACL_Factory.lookupWhereOrganizationActiveByLastUpdated(C, organizationRefnum, 0, 250);

        Res.successJson("", L);
      }
  }
