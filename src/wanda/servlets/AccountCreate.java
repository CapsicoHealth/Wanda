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

import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import tilda.utils.CollectionUtil;
import tilda.utils.EncryptionUtil;
import wanda.data.UserDetail_Data;
import wanda.data.UserDetail_Factory;
import wanda.data.User_Data;
import wanda.data.User_Factory;
import wanda.servlets.helpers.RoleHelper;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.config.SSOConfig;
import wanda.web.config.Wanda;

@WebServlet("/svc/user/account/create")
public class AccountCreate extends SimpleServlet
  {

    private static final long serialVersionUID = 988554219257979935L;

    public AccountCreate()
      {
        super(true, true, false, APIKeyEnum.EXCLUSIVELY);
      }

    @Override
    public void init(ServletConfig Conf)
      {
      }

    protected static Set<String> _GUEST_ROLE = new HashSet<String>(CollectionUtil.toList(RoleHelper.GUEST));

    @Override
    protected void justDo(RequestUtil req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        String id = req.getParamString("id", true);
        String email = req.getParamString("email", true);
        String nameFirst = req.getParamString("nameFirst", true);
        String nameLast = req.getParamString("nameLast", true);
        String nameMiddle = req.getParamString("nameMiddle", false);
        String title = req.getParamString("title", true);
        String company = req.getParamString("company", true);
        String fullAddress = req.getParamString("fullAddress", false);
        String country = req.getParamString("country", false);
        String stateProv = req.getParamString("stateProv", false);
        String city = req.getParamString("city", false);
        String zipCode = req.getParamString("zipCode", false);
        String industry = req.getParamString("industry", false);
        String tel = req.getParamString("tel", false);
        String rolesStr = req.getParamString("roles", true);
        String orgId = req.getParamString("orgId", false, company);

        Set<String> roles = rolesStr == null ? null : new Gson().fromJson(rolesStr, new TypeToken<Set<String>>()
          {
          }.getType());
        if (roles == null || roles.isEmpty() == true)
          req.addError("roles", "Roles cannot be empty");

        req.throwIfErrors();

        User_Data newUser = User_Factory.lookupByEmail(email);
        UserDetail_Data newPerson;
        if (newUser.read(C) == false)
          {
            newUser = User_Factory.create(email, id, _GUEST_ROLE, EncryptionUtil.getToken(24, true), EncryptionUtil.getToken(12, true));
            newPerson = UserDetail_Factory.create(newUser.getRefnum(), nameLast, nameFirst);
          }
        else
          {
            newPerson = newUser.getUserDetails();
            if (newPerson == null)
              {
                newPerson = UserDetail_Factory.create(newUser.getRefnum(), nameLast, nameFirst);
              }
          }

        newUser.setRoles(_GUEST_ROLE);
        newUser.setProfRoles(roles);
        newUser.setLockedNow();
        newUser.setLoginType(User_Data._loginTypeSSO);
        newUser.setLoginDomain(req.getApiCallSsoId());
        newUser.setLastipaddress(req.getRemoteAddr());
        
        SSOConfig ssoConfig = Wanda.getSsoConfig(req.getApiCallSsoId());
        if (ssoConfig != null)
          newUser.setPromoCode(ssoConfig._defaultPromoCode);

        newPerson.setNameFirst(nameFirst);
        newPerson.setNameLast(nameLast);
        newPerson.setNameMiddle(nameMiddle);
        newPerson.setProfTitle(title);
        newPerson.setCompany(company);
        newPerson.setOrgId(orgId);
        newPerson.setIndustry(industry);
        newPerson.setAddress1(fullAddress);
        newPerson.setCity(city);
        newPerson.setStateProv(stateProv);
        newPerson.setZipPostal(zipCode);
        newPerson.setCountry(country);
        newPerson.setTelMobile(tel);

        if (newUser.upsert(C) == false || newPerson.upsert(C) == false)
          throw new Exception("Database error: cannot create/update user record.");
        
        

        Res.success();
      }
  }
