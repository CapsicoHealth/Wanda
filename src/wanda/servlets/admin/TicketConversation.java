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

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import tilda.utils.json.JSONPrinter;
import wanda.data.TicketAnswer_Data;
import wanda.data.TicketAnswer_Factory;
import wanda.data.Ticket_Data;
import wanda.data.Ticket_Factory;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.config.Wanda;

@WebServlet("/svc/admin/ticket/conversation")
public class TicketConversation extends SimpleServlet
  {
    private static final long serialVersionUID = -1745307937763620646L;

    public TicketConversation()
      {
        super(true, false, true);
      }

    @Override
    protected void justDo(RequestUtil req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        long ticketRefnum = req.getParamLong("ticketRefnum", true);
        
        Ticket_Data t = Ticket_Factory.lookupByPrimaryKey(ticketRefnum);
        if (U.isSuperAdmin() == false && Wanda.isUserTicketAdmin(U.getRefnum()) == false)
         req.addError("ticketRefnum", "Unknown ticket or you do not have access.");
        else if (t.read(C) == false)
         req.addError("ticketRefnum", "Unknown ticket Id");

        req.throwIfErrors();
        
        List<TicketAnswer_Data> L = TicketAnswer_Factory.lookupWhereTicket(C, ticketRefnum, 0, 50);
        
        JSONPrinter j = new JSONPrinter();
        j.addElement("ticket", t, "");
        j.addElement("answers", L, "");

        Res.successJson(j);
      }

  }
