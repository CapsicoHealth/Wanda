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

import java.util.List;

import javax.servlet.annotation.WebServlet;

import tilda.db.Connection;
import wanda.data.Ticket_Data;
import wanda.data.Ticket_Factory;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;

@WebServlet("/svc/admin/ticket/list")
public class TicketList extends SimpleServlet
  {

    private static final long serialVersionUID = -1745307937763620646L;

    public TicketList()
      {
        super(true, false, true);
      }

    @Override
    protected void justDo(RequestUtil req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        List<Ticket_Data> L = U.isSuperAdmin() == true ? Ticket_Factory.lookupWhereAll(C, 0, 200) : Ticket_Factory.lookupWhereCreator(C, U.getRefnum(), 0, 100);

        Res.successJson("", L);
      }

  }
