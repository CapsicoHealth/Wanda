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

package wanda.servlets;

import java.io.PrintWriter;

import javax.servlet.annotation.WebServlet;

import tilda.db.Connection;
import tilda.utils.json.JSONUtil;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.tableau.TableauTicket;


@WebServlet("/svc/tableau/sso-ticket")
public class GetTableauSSOTicket extends SimpleServlet
  {

    private static final long serialVersionUID = 988554219257979935L;

    public GetTableauSSOTicket()
      {
        super(false);
      }

    @Override
    protected void justDo(RequestUtil req, ResponseUtil res, Connection C, User_Data U)
    throws Exception
      {
        String config = req.getParamString("config", true);
        req.throwIfErrors();

        String tableauBaseUrl = TableauTicket.getTrustedUrl(config);
        String tableauBaseSite = TableauTicket.getSite(config);
        LOG.debug("One-time use Tableau trusted url: '"+tableauBaseUrl+"'");

        PrintWriter Out = res.setContentType(ResponseUtil.ContentType.JSON);
        JSONUtil.startOK(Out, '{');
        JSONUtil.print(Out, "tableauBaseUrl", true, tableauBaseUrl);
        JSONUtil.print(Out, "tableauBaseSite", false, tableauBaseSite);
        JSONUtil.end(Out, '}');
      }
  }
