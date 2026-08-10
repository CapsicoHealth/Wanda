package wanda.servlets.organization;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import wanda.data.OrganizationRoleView_Factory;
import wanda.data.Organization_Data;
import wanda.data.Organization_Factory;

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.exceptions.NotFoundException;

@WebServlet("/svc/wanda/organizations/organization/details")
public class OrganizationDetails extends SimpleServlet
  {
    private static final long     serialVersionUID = 1L;
    protected static final Logger LOG              = LogManager.getLogger(OrganizationDetails.class.getName());

    public OrganizationDetails()
      {
        super(true, false, false);
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        long refnum = Req.getParamLong("refnum", true);
        Req.throwIfErrors();
        OrganizationRoleView_Factory.checkOrganizationAcl(C, U, refnum, OrganizationRoleView_Factory.OrganizationRole.READER);

        Organization_Data o = Organization_Factory.lookupByPrimaryKey(refnum);
        if (o.read(C) == false)
          throw new NotFoundException("Organization", "" + refnum, "Organization " + refnum + " cannot be found.");

        Res.successJson("", o);
      }
  }
