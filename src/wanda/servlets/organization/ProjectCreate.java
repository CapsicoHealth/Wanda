package wanda.servlets.organization;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import wanda.data.Project_Data;
import wanda.data.Project_Factory;

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import tilda.utils.CollectionUtil;
import tilda.utils.SystemValues;
import tilda.utils.TextUtil;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.exceptions.NotFoundException;

@WebServlet("/svc/wanda/project/create")
public class ProjectCreate extends SimpleServlet
  {
    private static final long     serialVersionUID = 1L;
    protected static final Logger LOG              = LogManager.getLogger(ProjectCreate.class.getName());

    public ProjectCreate()
      {
        super(true, true, false);
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        long refnum = Req.getParamLong("refnum", false);
        long organizationRefnum = Req.getParamLong("organizationRefnum", false);
        String appScope = Req.getParamString("appScope", true);
        String title = Req.getParamString("title", true);
        String description = Req.getParamString("description", false);
        String area = Req.getParamString("area", false);
        String tag = Req.getParamString("tags", false);
        String status = Req.getParamString("status", false);
        Req.throwIfErrors();

        Project_Data p;
        if (refnum == SystemValues.EVIL_VALUE) // create
          {
            p = Project_Factory.create(appScope, title, U.getRefnum(), U.getId(), U.getRefnum(), U.getId());
            if (organizationRefnum!=SystemValues.EVIL_VALUE)
             p.setOrganizationRefnum(organizationRefnum);
          }
        else // update
          {
            p = Project_Factory.lookupByPrimaryKey(refnum);
            if (p.read(C) == false)
              throw new NotFoundException("Project", "" + refnum, "Project " + refnum + " cannot be found.");
            // AppScope and orgRefnum must match. Because organizationRefnum is optional, we have to check for nulls. 
            // If organizationRefnum was provided, it must match the project's. If it wasn't provided, the project's must be null.
            if (p.getAppScope().equals(appScope) == false  || organizationRefnum == SystemValues.EVIL_VALUE && p.isNullOrganizationRefnum() == false
                                                           || organizationRefnum != SystemValues.EVIL_VALUE && p.getOrganizationRefnum() != organizationRefnum)
              throw new NotFoundException("Project", "" + refnum, "Project " + refnum + " is not found in the specified appScope and organization.");
            if (p.getCreatorRefnum() != U.getRefnum())
              throw new NotFoundException("Project", "" + refnum, "Project " + refnum + " is not updatable by this user.");
            p.setTitle(title);
            p.setLastUpdatorRefnum(U.getRefnum());
            p.setLastUpdatorId(U.getId());
          }

        p.setDescription(description);
        p.setArea(area);
        if (TextUtil.isNullOrEmpty(tag) == false)
          {
            String[] parts = tag.toUpperCase().split("[,;\\s]+");
            p.setTags(CollectionUtil.toSet(parts));
          }
        if (TextUtil.isNullOrEmpty(status) == false)
          p.setStatus(status);

        if (p.write(C) == false)
          {
            p = organizationRefnum==SystemValues.EVIL_VALUE ? Project_Factory.lookupByAppScopeTitleActive(appScope, title)
                                                            : Project_Factory.lookupByOrgAppScopeTitleActive(organizationRefnum, appScope, title);
            if (p.read(C) == true)
              {
                Req.addError("title", "A project with the title '" + title + "' already exists. Please choose a different title.");
                Req.throwIfErrors();
              }
            throw new Exception("Database error: cannot " + (refnum == SystemValues.EVIL_VALUE ? "create" : "update") + " project.");
          }

        Res.successJson("", p);
      }
  }
