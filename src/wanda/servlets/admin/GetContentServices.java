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
import java.util.List;

import javax.servlet.annotation.WebServlet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;
import tilda.utils.CollectionUtil;
import wanda.data.User_Data;
import wanda.servlets.helpers.RoleHelper;
import wanda.web.ContentDefinitionService;
import wanda.web.ContentDefinitionService.ContentDefinition;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.config.WebBasics;

/**
 * Servlet implementation class Login
 */
@WebServlet("/svc/admin/contentServices")
public class GetContentServices extends SimpleServlet
  {
    protected static final Logger LOG              = LogManager.getLogger(GetContentServices.class.getName());
    private static final long     serialVersionUID = 7833614571489016882L;

    /**
     * Default constructor.
     */
    public GetContentServices()
      {
        super(true);
      }

    protected static List<ContentDefinition> getContentDefinitions(Connection C, User_Data U)
      {
        List<ContentDefinition> contents = new ArrayList<ContentDefinition>();
        if (WebBasics.getLoginSystem() != null && WebBasics.getLoginSystem()._contentDefinitionServices != null)
          {
            String[] contentDefinitionServices = WebBasics.getLoginSystem()._contentDefinitionServices;
            for (String cds : contentDefinitionServices)
              try
                {
                  Class<ContentDefinitionService> c = (Class<ContentDefinitionService>) Class.forName(cds);
                  ContentDefinition[] arr = c.getConstructor().newInstance().getContents(C, U);
                  if (arr != null && arr.length > 0)
                   CollectionUtil.append(contents, arr);
                }
              catch (Throwable T)
                {
                  LOG.warn("Cannot follow user sync process off of '" + cds + "'.");
                }
          }
        return contents;
      }
    

    @Override
    protected void justDo(RequestUtil req, ResponseUtil res, Connection C, User_Data U)
    throws Exception
      {
        throwIfUserInvalidRole(U, RoleHelper.ADMINROLES);
        
        res.successJson(ContentDefinition.toJsonPrinter(getContentDefinitions(C, U)));
      }
  }
