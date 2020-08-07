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

import wanda.data.TenantView_Data;
import wanda.data.TenantView_Factory;
import wanda.data.User_Data;

import tilda.db.Connection;
import tilda.utils.json.JSONUtil;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SessionUtil;
import wanda.web.SimpleServlet;

@WebServlet("/svc/SelectedTenant")
public class GetSelectedTenantServlet extends SimpleServlet
  {
    private static final long serialVersionUID = 4586735867602207005L;

    public GetSelectedTenantServlet()
      {
        super(true);
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        PrintWriter Out = Res.setContentType(ResponseUtil.ContentType.JSON);
        TenantView_Data TV = TenantView_Factory.getTenantByTenantUserRefnum(C, U.getRefnum(), Req.getSessionLong(SessionUtil.Attributes.TENANTUSERREFNUM.toString()));
        if(TV == null)
          {
            Res.Success();
            return;
          }
        JSONUtil.response(Out, "tenantUserJson", TV);
      }

  }
