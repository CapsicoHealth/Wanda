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

import jakarta.servlet.annotation.WebServlet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;
import tilda.db.ListResults;
import tilda.utils.json.JSONUtil;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;

import wanda.data.TenantView_Data;
import wanda.data.TenantView_Factory;
import wanda.data.User_Data;

@WebServlet("/svc/MyTenantList")
/**
 *
 * @author mohan
 * API to return list of TenantUser List to which logged in user has access.
 */
public class MyTenantList extends SimpleServlet
  {
    /**
   * 
   */
  private static final long serialVersionUID = -628664735749000388L;
    protected static final Logger LOG = LogManager.getLogger(MyTenantList.class.getName());


    /**
     * Default constructor.
     */
    public MyTenantList()
      {
        super(false);
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
      throws Exception
      {
        ListResults<TenantView_Data> list = TenantView_Factory.getAllActiveByUserRefnum(C, U.getRefnum(), 0, 1000);
        PrintWriter Out = Res.setContentType(ResponseUtil.ContentType.JSON);
        JSONUtil.response(Out, "tenantUserJson", list);
      }

  }
