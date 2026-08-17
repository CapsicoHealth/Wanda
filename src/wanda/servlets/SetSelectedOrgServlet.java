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
import wanda.data.OrganizationRoleView_Factory;
import wanda.data.Organization_Data;
import wanda.data.Organization_Factory;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SessionUtil;
import wanda.web.SimpleServlet;
import wanda.web.exceptions.NotFoundException;

/**
 * Sets (or clears) the Organization currently active/selected for this session -- i.e., the organization context
 * that org-scoped resources (Projects, DataSources...) should be resolved against. This is analogous to the
 * multi-tenant "Selected Tenant" mechanism, but for the Organizations feature.<BR>
 * <BR>
 * Calling this servlet without an "organizationRefnum" parameter, or with a value of 0 or less, clears the active
 * organization from the session.
 */
@WebServlet("/svc/SetSelectedOrg")
public class SetSelectedOrgServlet extends SimpleServlet
  {
    private static final long     serialVersionUID = 1L;
    protected static final Logger LOG              = LogManager.getLogger(SetSelectedOrgServlet.class.getName());

    public SetSelectedOrgServlet()
      {
        super(true, true, false);
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        long organizationRefnum = Req.getParamLong("organizationRefnum", -1L);
        if (organizationRefnum <= 0)
          {
            Req.removeSessionAttribute(SessionUtil.Attributes.ACTIVE_ORG_REFNUM.toString());
            Res.success();
            return;
          }

        // Will throw if the user doesn't have at least READER access to this organization.
        OrganizationRoleView_Factory.checkOrganizationAcl(C, U, organizationRefnum, OrganizationRoleView_Factory.OrganizationRole.READER);

        Organization_Data org = Organization_Factory.lookupByPrimaryKey(organizationRefnum);
        if (org.read(C) == false)
          throw new NotFoundException("Organization", "" + organizationRefnum, "Organization " + organizationRefnum + " cannot be found.");

        Req.setSessionLong(SessionUtil.Attributes.ACTIVE_ORG_REFNUM.toString(), organizationRefnum);

        Res.successJson("", org);
      }
  }
