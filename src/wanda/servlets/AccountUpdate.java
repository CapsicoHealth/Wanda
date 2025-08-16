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
        super(true, true, true);
      }

    @Override
    public void init(ServletConfig Conf)
      {
        SessionFilter.addMaskedUrlNvp("password");
        SessionFilter.addMaskedUrlNvp("pswd");
      }

    @Override
    protected void justDo(RequestUtil req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        // Local accounts cannot be updated
        if (U.isLoginTypeLocal() == false)
          {
            LOG.error("User '" + U.getId() + "' is not a local user and cannot be updated via this service.");
            throw new NotFoundException("User", U.getEmail(), "This account is not a local account and cannot be updated");
          }

        /*@formatter:off*/
        String currentPassword = req.getParamString("currentPassword", true );
        String newPassword     = req.getParamString("newPassword"    , false);
        String email           = req.getParamString("email"          , true );
        String nameTitle       = req.getParamString("nameTitle"      , false);
        String nameFirst       = req.getParamString("nameFirst"      , true );
        String nameLast        = req.getParamString("nameLast"       , true );
        String company         = req.getParamString("company"        , false);
        String profTitle       = req.getParamString("profTitle"      , false);
        String phone           = req.getParamString("phone"          , false);
        String address1        = req.getParamString("address1"       , false);
        String city            = req.getParamString("city"           , false);
        String stateProv       = req.getParamString("stateProv"      , false);
        String zipPostal       = req.getParamString("zipPostal"      , false);
        String country         = req.getParamString("country"        , false);
        /*@formatter:on*/

        req.throwIfErrors();

        // If the current password is invalid, throw error
        if (EncryptionUtil.hash(currentPassword, U.getPswdSalt()).equals(U.getPswd()) == false)
          req.addError("currentPassword", "please enter correct current password");

        // Check if newPassword is valid (if present)
        if (TextUtil.isNullOrEmpty(newPassword) == false)
          {
            List<String> errors = Wanda.validatePassword(newPassword);
            if (errors.isEmpty() == false)
              for (String error : errors)
                req.addError("password", error);
          }

        req.throwIfErrors();

        // First, check if email has changed and make necessary updates
        U.updateEmailIfChanged(C, email);
        // Update the password if needed
        U.updatePassword(newPassword);
        U.write(C);


        UserDetail_Data UD = U.getUserDetails();
        /*@formatter:off*/
        if (TextUtil.isNullOrEmpty(nameTitle) == false)  UD.setNameTitle(nameTitle);
        if (TextUtil.isNullOrEmpty(nameFirst) == false)  UD.setNameFirst(nameFirst);
        if (TextUtil.isNullOrEmpty(nameLast ) == false)  UD.setNameLast (nameLast );
        if (TextUtil.isNullOrEmpty(company  ) == false)  UD.setCompany  (company  );
        if (TextUtil.isNullOrEmpty(profTitle) == false)  UD.setProfTitle(profTitle);
        if (TextUtil.isNullOrEmpty(phone    ) == false)  UD.setTelMobile(phone    );
        if (TextUtil.isNullOrEmpty(address1 ) == false)  UD.setAddress1 (address1 );
        if (TextUtil.isNullOrEmpty(city     ) == false)  UD.setCity     (city     );
        if (TextUtil.isNullOrEmpty(stateProv) == false)  UD.setStateProv(stateProv);
        if (TextUtil.isNullOrEmpty(zipPostal) == false)  UD.setZipPostal(stateProv);
        if (TextUtil.isNullOrEmpty(country  ) == false)  UD.setCountry  (country  );
        /*@formatter:on*/
        UD.write(C);

        // Commit happens in Session filter.
        long TenantUserRefnum = req.getSessionLong(SessionUtil.Attributes.TENANTUSERREFNUM.toString());
        if (TenantUserRefnum != SystemValues.EVIL_VALUE && U != null)
          UserTenantSync.sync(C, U, TenantUserRefnum);

        Res.success();
      }
  }
