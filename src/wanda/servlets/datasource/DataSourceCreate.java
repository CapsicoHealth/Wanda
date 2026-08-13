/* ===========================================================================
 * Copyright (C) 2026 CapsicoHealth Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wanda.servlets.datasource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import wanda.data.DataSource_Data;
import wanda.data.DataSource_Factory;

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import tilda.utils.SystemValues;
import tilda.utils.TextUtil;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.exceptions.NotFoundException;

@WebServlet("/svc/wanda/datasources/datasource/create")
public class DataSourceCreate extends SimpleServlet
  {
    private static final long     serialVersionUID = 1L;
    protected static final Logger LOG              = LogManager.getLogger(DataSourceCreate.class.getName());

    public DataSourceCreate()
      {
        super(true, true, false);
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        long   refnum             = Req.getParamLong  ("refnum",             false);
        long   organizationRefnum = Req.getParamLong  ("organizationRefnum", true );
        String title              = Req.getParamString("title",              true );
        String description        = Req.getParamString("description",        false);
        String type               = Req.getParamString("type",                true );
        String configJson         = Req.getParamString("configJson",          true );
        String status             = Req.getParamString("status",              false);
        if (DataSource_Data.checkType(type) == false)
          Req.addError("type", "Invalid type specified: allowed are " + TextUtil.print(DataSource_Data._type_Values, 0) + ".");
        Req.throwIfErrors();

        DataSource_Data d;
        if (refnum == SystemValues.EVIL_VALUE) // create
          {
            d = DataSource_Factory.create(organizationRefnum, title, type, configJson, U.getRefnum(), U.getId(), U.getRefnum(), U.getId());
          }
        else // update
          {
            d = DataSource_Factory.lookupByPrimaryKey(refnum);
            if (d.read(C) == false || d.getOrganizationRefnum() != organizationRefnum)
              throw new NotFoundException("DataSource", "" + refnum, "Data source " + refnum + " cannot be found.");
            if (d.getCreatorRefnum() != U.getRefnum())
              throw new NotFoundException("DataSource", "" + refnum, "Data source " + refnum + " is not updatable by this user.");
            d.setTitle(title);
            d.setType(type);
            d.setLastUpdatorRefnum(U.getRefnum());
            d.setLastUpdatorId(U.getId());
          }

        d.setDescription(description);
        d.setConfigJson(configJson);
        if (TextUtil.isNullOrEmpty(status) == false)
          d.setStatus(status);

        if (d.write(C) == false)
          {
            d = DataSource_Factory.lookupByOrganizationTitleActive(organizationRefnum, title);
            if (d.read(C) == true)
              {
                Req.addError("title", "A data source with the title '" + title + "' already exists in this organization. Please choose a different title.");
                Req.throwIfErrors();
              }
            throw new Exception("Database error: cannot " + (refnum == SystemValues.EVIL_VALUE ? "create" : "update") + " data source.");
          }

        Res.successJson("", d);
      }
  }
