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

import javax.servlet.ServletConfig;
import javax.servlet.annotation.WebServlet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import wanda.data.User_Data;

import tilda.db.Connection;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SessionFilter;
import wanda.web.SimpleServlet;
import wanda.web.exceptions.NotFoundException;

@WebServlet("/svc/ldap/login")
public class LdapLogin extends SimpleServlet
  {
    private static final long     serialVersionUID = 1018123535563202342L;
    protected static final Logger LOG              = LogManager.getLogger(LdapLogin.class.getName());

    /*
     * Sample test urls
     * https://localhost:8443/reports/svc/ad/login?username=gdad3@gendevs.com&password=Pa55word3&domain=dev.generaldevelopers.com
     * https://localhost:8443/reports/svc/ad/login?username=gdad2@gendevs.com&password=Pa55word&domain=dev.generaldevelopers.com
     * https://localhost:8443/reports/svc/ad/login?username=gdad2@gendevs.com&password=Pa55word&domain=dev2.generaldevelopers.com
     * https://localhost:8443/reports/svc/ad/login?username=demo@capsicohealth.com&password=demoCap!&domain=capsico
     */
    public LdapLogin()
      {
        super(false);
      }

    @Override
    public void init(ServletConfig Conf)
      {
        SessionFilter.addMaskedUrlNvp("password");
      }


    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {

        String username = Req.getParamString("username", true);
        String password = Req.getParamString("password", true);
        String domain = Req.getParamString("domain", false);

        Req.throwIfErrors();

        U = new LdapUtils().login(C, username, password, domain);

        if (U == null)
          throw new NotFoundException("User", username);

        Req.setSessionUser(U);
        
        Res.success();
      }

  }
