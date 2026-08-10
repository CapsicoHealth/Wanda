package wanda.servlets.organization;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import wanda.data.Organization_Data;
import wanda.data.Organization_Factory;

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import tilda.utils.SystemValues;
import tilda.utils.TextUtil;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.exceptions.NotFoundException;

@WebServlet("/svc/wanda/organizations/organization/create")
public class OrganizationCreate extends SimpleServlet
  {
    private static final long     serialVersionUID = 1L;
    protected static final Logger LOG              = LogManager.getLogger(OrganizationCreate.class.getName());

    public OrganizationCreate()
      {
        super(true, true, false);
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        long   refnum      = Req.getParamLong  ("refnum",      false);
        String title       = Req.getParamString("title",       true);
        String description = Req.getParamString("description", false);
        String status      = Req.getParamString("status",      false);
        Req.throwIfErrors();

        Organization_Data o;
        if (refnum == SystemValues.EVIL_VALUE) // create
          {
            o = Organization_Factory.create(title, U.getRefnum(), U.getId(), U.getRefnum(), U.getId());
          }
        else // update
          {
            o = Organization_Factory.lookupByPrimaryKey(refnum);
            if (o.read(C) == false)
              throw new NotFoundException("Organization", "" + refnum, "Organization " + refnum + " cannot be found.");
            if (o.getCreatorRefnum() != U.getRefnum())
              throw new NotFoundException("Organization", "" + refnum, "Organization " + refnum + " is not updatable by this user.");
            o.setTitle(title);
            o.setLastUpdatorRefnum(U.getRefnum());
            o.setLastUpdatorId(U.getId());
          }

        o.setDescription(description);
        if (TextUtil.isNullOrEmpty(status) == false)
          o.setStatus(status);

        if (o.write(C) == false)
          {
            o = Organization_Factory.lookupByTitleActive(title);
            if (o.read(C) == true)
              {
                Req.addError("title", "An organization with the title '" + title + "' already exists. Please choose a different title.");
                Req.throwIfErrors();
              }
            throw new Exception("Database error: cannot " + (refnum == SystemValues.EVIL_VALUE ? "create" : "update") + " organization.");
          }

        Res.successJson("", o);
      }
  }
