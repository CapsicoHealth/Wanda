package wanda.servlets.organization;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import wanda.data.OrganizationACLView_Data;
import wanda.data.OrganizationACLView_Factory;

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;

@WebServlet("/svc/wanda/organizations/organization/list")
public class OrganizationList extends SimpleServlet
  {
    private static final long     serialVersionUID = 1L;
    protected static final Logger LOG              = LogManager.getLogger(OrganizationList.class.getName());

    public OrganizationList()
      {
        super(true, false, false);
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        // Return all active organizations for which the current user is the creator.
        // Organizations shared with them via ACL are surfaced here too via the CreatorOrAccess view query.
        List<OrganizationACLView_Data> L = OrganizationACLView_Factory.lookupWhereCreatorOrAccess(C, U.getRefnum(), U.getRefnum(), 0, 200);

        Res.successJson("", L);
      }
  }
