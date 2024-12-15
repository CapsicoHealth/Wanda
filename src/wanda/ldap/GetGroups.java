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


package wanda.ldap;

import java.io.PrintWriter;
import java.util.List;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import wanda.data.User_Data;

import tilda.db.Connection;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;

@WebServlet("/svc/ldap/groups")
public class GetGroups extends SimpleServlet
  {
    private static final long     serialVersionUID = 1018123535563202342L;
    protected static final Logger LOG              = LogManager.getLogger(GetGroups.class.getName());

    /*
     * Sample test urls
     * https://localhost:8443/reports/svc/ldap/groups?domain=dev.generaldevelopers.com
     */
    public GetGroups()
      {
        super(true);
      }

    @Override
    public void init(ServletConfig Conf)
      {
        // TODO: do a dry run. validate the config and basic login tests
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {

        String domain = Req.getParamString("domain", true);

        Req.throwIfErrors();

        List<LdapGroup> groups = new LdapUtils().getGroups(C, domain);

        PrintWriter Out = Res.setContentType(ResponseUtil.ContentType.JSON);
        LdapGroup.printGroups(groups, Out);
      }

  }
