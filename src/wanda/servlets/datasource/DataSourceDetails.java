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

import wanda.data.DataSourceRoleView_Factory;
import wanda.data.DataSource_Data;
import wanda.data.DataSource_Factory;

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.exceptions.NotFoundException;

@WebServlet("/svc/wanda/datasources/datasource/details")
public class DataSourceDetails extends SimpleServlet
  {
    private static final long     serialVersionUID = 1L;
    protected static final Logger LOG              = LogManager.getLogger(DataSourceDetails.class.getName());

    public DataSourceDetails()
      {
        super(true, false, false);
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        long refnum             = Req.getParamLong("refnum",             true);
        long organizationRefnum = Req.getParamLong("organizationRefnum", true);
        Req.throwIfErrors();
        DataSourceRoleView_Factory.checkDataSourceAcl(C, U, refnum, DataSourceRoleView_Factory.DataSourceRole.READER);

        DataSource_Data d = DataSource_Factory.lookupByPrimaryKey(refnum);
        if (d.read(C) == false || d.getOrganizationRefnum() != organizationRefnum)
          throw new NotFoundException("DataSource", "" + refnum, "Data source " + refnum + " cannot be found.");

        Res.successJson("", d);
      }
  }
