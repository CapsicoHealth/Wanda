package wanda.servlets.datasource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import wanda.data.DataSourceRoleView_Factory;
import wanda.data.DataSource_Data;
import wanda.data.DataSource_Factory;

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import tilda.utils.DateTimeUtil;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.exceptions.NotFoundException;

@WebServlet("/svc/wanda/datasources/datasource/delete")
public class DataSourceDelete extends SimpleServlet
  {
    private static final long     serialVersionUID = 1L;
    protected static final Logger LOG              = LogManager.getLogger(DataSourceDelete.class.getName());

    public DataSourceDelete()
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
        DataSourceRoleView_Factory.checkDataSourceAcl(C, U, refnum, DataSourceRoleView_Factory.DataSourceRole.OWNER);

        DataSource_Data d = DataSource_Factory.lookupByPrimaryKey(refnum);
        if (d.read(C) == false)
          throw new NotFoundException("DataSource", "" + refnum, "Data source " + refnum + " cannot be found.");

        if (deleteMode.equalsIgnoreCase("hard") == true)
          {
            if (d.isNullDeleted() == true)
              throw new Exception("Cannot hard-delete data source " + refnum + ": it must be soft-deleted first.");
            d.setHardDeletedNow();
            d.setTitle(d.getTitle() + " [del " + DateTimeUtil.printDateTimeSuperCompact(DateTimeUtil.nowLocal()) + "]");
          }
        else
          {
            DataSourceRoleView_Factory.checkDataSourceAcl(C, U, refnum, DataSourceRoleView_Factory.DataSourceRole.ADMIN);
            if (deleteMode.equalsIgnoreCase("undelete") == true)
              d.setNullDeleted();
            else
              d.setDeletedNow();
          }

        if (d.write(C) == false)
          throw new Exception("Cannot '" + deleteMode + "' data source " + refnum + " for unknown reasons.");

        Res.successJson("", d);
      }
  }
