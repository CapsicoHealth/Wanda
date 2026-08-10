package wanda.servlets.datasource;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import wanda.data.DataSourceACLView_Data;
import wanda.data.DataSourceACLView_Factory;

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;

@WebServlet("/svc/wanda/datasources/datasource/list")
public class DataSourceList extends SimpleServlet
  {
    private static final long     serialVersionUID = 1L;
    protected static final Logger LOG              = LogManager.getLogger(DataSourceList.class.getName());

    public DataSourceList()
      {
        super(true, false, false);
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        long organizationRefnum = Req.getParamLong("organizationRefnum", true);
        Req.throwIfErrors();

        // Return all active data sources in this organization for which the current user is the creator.
        // Data sources shared with them via ACL are surfaced here too via the CreatorOrAccess view query.
        List<DataSourceACLView_Data> L = DataSourceACLView_Factory.lookupWhereOrganizationCreatorOrAccess(C, organizationRefnum, U.getRefnum(), U.getRefnum(), 0, 200);

        Res.successJson("", L);
      }
  }
