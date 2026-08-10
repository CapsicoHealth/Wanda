package wanda.servlets.organization;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import wanda.data.OrganizationRoleView_Factory;
import wanda.data.Organization_Data;
import wanda.data.Organization_Factory;

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import tilda.utils.DateTimeUtil;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.exceptions.NotFoundException;

@WebServlet("/svc/wanda/organizations/organization/delete")
public class OrganizationDelete extends SimpleServlet
  {
    private static final long     serialVersionUID = 1L;
    protected static final Logger LOG              = LogManager.getLogger(OrganizationDelete.class.getName());

    public OrganizationDelete()
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
        OrganizationRoleView_Factory.checkOrganizationAcl(C, U, refnum, OrganizationRoleView_Factory.OrganizationRole.OWNER);

        Organization_Data o = Organization_Factory.lookupByPrimaryKey(refnum);
        if (o.read(C) == false)
          throw new NotFoundException("Organization", "" + refnum, "Organization " + refnum + " cannot be found.");

        if (deleteMode.equalsIgnoreCase("hard") == true)
          {
            if (o.isNullDeleted() == true)
              throw new Exception("Cannot hard-delete organization " + refnum + ": it must be soft-deleted first.");
            o.setHardDeletedNow();
            o.setTitle(o.getTitle() + " [del " + DateTimeUtil.printDateTimeSuperCompact(DateTimeUtil.nowLocal()) + "]");
          }
        else
          {
            OrganizationRoleView_Factory.checkOrganizationAcl(C, U, refnum, OrganizationRoleView_Factory.OrganizationRole.ADMIN);
            if (deleteMode.equalsIgnoreCase("undelete") == true)
              o.setNullDeleted();
            else
              o.setDeletedNow();
          }

        if (o.write(C) == false)
          throw new Exception("Cannot '" + deleteMode + "' organization " + refnum + " for unknown reasons.");

        Res.successJson("", o);
      }
  }
