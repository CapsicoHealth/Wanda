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

package wanda.servlets.organization;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import wanda.data.OrganizationInvite_Data;
import wanda.data.OrganizationInvite_Factory;
import wanda.data.OrganizationRoleView_Factory;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;

/**
 * Lists the still-pending Organization invitations for a given organization. Only an org Admin/Owner may view the
 * pending invite list.
 */
@WebServlet("/svc/wanda/organizations/invite/list")
public class OrganizationInviteList extends SimpleServlet
  {
    private static final long     serialVersionUID = 1L;
    protected static final Logger LOG              = LogManager.getLogger(OrganizationInviteList.class.getName());

    public OrganizationInviteList()
      {
        super(true, false, false);
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        long organizationRefnum = Req.getParamLong("organizationRefnum", true);
        Req.throwIfErrors();
        OrganizationRoleView_Factory.checkOrganizationAcl(C, U, organizationRefnum, OrganizationRoleView_Factory.OrganizationRole.ADMIN);

        List<OrganizationInvite_Data> L = OrganizationInvite_Factory.lookupWhereByOrgPending(C, organizationRefnum, 0, 250);

        Res.successJson("", L);
      }
  }
