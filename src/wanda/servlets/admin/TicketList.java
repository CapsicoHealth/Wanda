/* ===========================================================================
 * Copyright (C) 2017 CapsicoHealth Inc.
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

package wanda.servlets.admin;

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import tilda.db.ListResults;
import tilda.utils.CollectionUtil;
import wanda.data.TicketWaitingView_Data;
import wanda.data.TicketWaitingView_Factory;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.config.Wanda;

@WebServlet("/svc/admin/ticket/list")
public class TicketList extends SimpleServlet
  {

    private static final long serialVersionUID = -1745307937763620646L;

    public TicketList()
      {
        super(true, false, true);
      }

    protected static long[] _ADMIN_REFNUMS = Wanda.getTicketAccountRefnums();
    protected static int    _ALERT_TIMING  = Wanda.getTicketAlertMinutes();

    @Override
    protected void justDo(RequestUtil req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        boolean unanswered = req.getParamBoolean("unanswered", false);

        ListResults<TicketWaitingView_Data> L = null;

        // Is the user a super or ticket admin?
        if (U.isSuperAdmin() == true || CollectionUtil.in(U.getRefnum(), _ADMIN_REFNUMS) == true)
          {
            if (unanswered == true)
              L = TicketWaitingView_Factory.lookupWhereUnanswered(C, _ADMIN_REFNUMS, 0, 100);
            else
              L = TicketWaitingView_Factory.lookupWhereAll(C, 0, 100);
          }
        else // regular users can only see their tickets
          L = TicketWaitingView_Factory.lookupWhereUser(C, U.getRefnum(), 0, 100);

        Res.successJson("", L);
      }

  }
