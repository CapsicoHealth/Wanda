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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.annotation.WebServlet;

import tilda.db.Connection;
import tilda.utils.pairs.StringStringPair;
import wanda.data.Ticket_Data;
import wanda.data.Ticket_Factory;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;

@WebServlet("/svc/admin/ticket/create")
public class TicketCreate extends SimpleServlet
  {

    private static final long serialVersionUID = -1745307937763620646L;

    public TicketCreate()
      {
        super(true, true, true);
      }

    @Override
    protected void justDo(RequestUtil req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
          
        List<StringStringPair> errors = new ArrayList<StringStringPair>();
        Map<String, String[]> params = new HashMap<String, String[]>(req.getParameterMap());
        params.put("creatorRefnum", new String[] {""+U.getRefnum()});
        params.put("creatorId", new String[] {""+U.getId()});
        Ticket_Data t = Ticket_Factory.init(params, errors);

        req.throwIfErrors(errors);

        if (t.write(C) == false)
         throw new Error("There was an error writing the ticket to the database due to an unknown error.");

        Res.successJson("", t);
      }

  }
