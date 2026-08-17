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

package wanda.servlets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import tilda.utils.SystemValues;
import wanda.data.Organization_Data;
import wanda.data.Organization_Factory;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SessionUtil;
import wanda.web.SimpleServlet;

/**
 * Returns the Organization currently active/selected for this session, if any -- mirrors {@link GetSelectedTenantServlet}
 * but for the Organizations feature. Returns an empty success payload if no organization is currently selected, or
 * if the selected organization is no longer accessible.
 */
@WebServlet("/svc/GetSelectedOrg")
public class GetSelectedOrgServlet extends SimpleServlet
  {
    private static final long     serialVersionUID = 1L;
    protected static final Logger LOG              = LogManager.getLogger(GetSelectedOrgServlet.class.getName());

    public GetSelectedOrgServlet()
      {
        super(true);
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        long organizationRefnum = Req.getSessionLong(SessionUtil.Attributes.ACTIVE_ORG_REFNUM.toString());
        if (organizationRefnum == SystemValues.EVIL_VALUE)
          {
            Res.success();
            return;
          }

        Organization_Data org = Organization_Factory.lookupByPrimaryKey(organizationRefnum);
        if (org.read(C) == false)
          {
            Res.success();
            return;
          }

        Res.successJson("", org);
      }
  }
