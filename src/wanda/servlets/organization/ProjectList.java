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

import wanda.data.ProjectACLView_Data;
import wanda.data.ProjectACLView_Factory;

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import tilda.utils.SystemValues;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;

@WebServlet("/svc/wanda/project/list")
public class ProjectList extends SimpleServlet
  {
    private static final long     serialVersionUID = 1L;
    protected static final Logger LOG              = LogManager.getLogger(ProjectList.class.getName());

    public ProjectList()
      {
        super(true, false, false);
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        long organizationRefnum = Req.getParamLong("organizationRefnum", false);
        String appScope = Req.getParamString("appScope", true);
        Req.throwIfErrors();

        // Return all active projects for which the current user is the creator.
        // Projects shared with them via ACL are surfaced here too via the CreatorOrAccess view query.
        List<ProjectACLView_Data> L = organizationRefnum == SystemValues.EVIL_VALUE
            ? ProjectACLView_Factory.lookupWhereAppCreatorOrAccess(C, appScope, U.getRefnum(), U.getRefnum(), 0, 200)
            : ProjectACLView_Factory.lookupWhereOrgAppCreatorOrAccess(C, organizationRefnum, appScope, U.getRefnum(), U.getRefnum(), 0, 200)
            ;
        Res.successJson("", L);
      }
  }
