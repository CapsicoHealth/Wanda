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

import java.util.List;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import tilda.utils.EncryptionUtil;
import tilda.utils.SystemValues;
import tilda.utils.TextUtil;
import wanda.data.UserDetail_Data;
import wanda.data.User_Data;
import wanda.servlets.helpers.UserTenantSync;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SessionFilter;
import wanda.web.SessionUtil;
import wanda.web.SimpleServlet;
import wanda.web.config.Wanda;
import wanda.web.exceptions.NotFoundException;

@WebServlet("/svc/user/account/update")
public class AccountUpdate extends SimpleServlet
  {

    private static final long serialVersionUID = 988554219257979935L;

    public AccountUpdate()
      {
        super(true, false, true);
      }
    
    @Override
    public void init(ServletConfig Conf)
      {
        SessionFilter.addMaskedUrlNvp("password");
      }

    @Override
    protected void justDo(RequestUtil req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        String currentPassword = req.getParamString("currentPassword", true);
        req.throwIfErrors();

        if (U.isLoginTypeLocal() == false)
          {
            LOG.error("User '"+U.getId()+"' is not a local user and cannot be updated via this service.");
            throw new NotFoundException("User", U.getEmail(), "This account cannot be updated");
          }

        long TenantUserRefnum = req.getSessionLong(SessionUtil.Attributes.TENANTUSERREFNUM.toString());
        if (EncryptionUtil.hash(currentPassword, U.getPswdSalt()).equals(U.getPswd()) == false)
          {
            req.addError("currentPassword", "please enter correct current password");
          }
        req.throwIfErrors();
        UserDetail_Data person = U.getUserDetails();
                
        String nameTitle = req.getParamString("nameTitle", false);
        String nameFirst = req.getParamString("nameFirst", false);
        String nameLast  = req.getParamString("nameLast", false);
        String email     = req.getParamString("email", false);
        String password  = req.getParamString("password", false);
        
        person.updateDetails(C, nameTitle, nameFirst, nameLast);
        U.updateEmail(C, email);
       
        if (TextUtil.isNullOrEmpty(password) == false)
          {
            String hashedPassword = null;
            List<String> errors = Wanda.validatePassword(password);
            if (!errors.isEmpty())
              {
                for (String error : errors)
                  {
                    req.addError("password", error);
                  }
              }
            req.throwIfErrors();

            String salt = U.getOrCreatePswdSalt();
            hashedPassword = EncryptionUtil.hash(password, salt);
            if (U.hasPswdHist(hashedPassword))
              {
                throw new Exception("Password Already used");
              }
            U.setPswd(hashedPassword);
            U.setPswdSalt(salt);
            U.setPswdCreateNow();
            U.pushToPswdHistory(hashedPassword);
          } // TODO ADD FOR CONTACT / PERSON

        U.write(C);
        person.write(C);
        // Commit happens in Session filter.
        if (TenantUserRefnum != SystemValues.EVIL_VALUE && U != null)
          {
            UserTenantSync.sync(C, U, TenantUserRefnum);
          }
         
        Res.success();
      }
  }
