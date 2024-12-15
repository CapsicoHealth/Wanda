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
import tilda.utils.SystemValues;
import tilda.utils.pairs.StringStringPair;
import wanda.data.Promo_Data;
import wanda.data.Promo_Factory;
import wanda.data.User_Data;
import wanda.servlets.helpers.RoleHelper;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.exceptions.DuplicateResourceException;
import wanda.web.exceptions.NotFoundException;

@WebServlet("/svc/admin/promo/create")
public class PromoCreate extends SimpleServlet
  {

    private static final long serialVersionUID = -1745307937763620646L;

    public PromoCreate()
      {
        super(true);
      }

    @Override
    protected void justDo(RequestUtil req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        throwIfUserInvalidRole(U, RoleHelper.ADMINROLES);
        
        long refnum = req.getParamLong("refnum", false);
        if (refnum != SystemValues.EVIL_VALUE) // update, so must check a system promotion is not being requested as this is not allowed.
          {
            Promo_Data p = Promo_Factory.lookupByPrimaryKey(refnum);
            if (p.read(C) == false || p.getSystem() == true)
             throw new NotFoundException("Promotion", refnum);
          }
          
        List<StringStringPair> errors = new ArrayList<StringStringPair>();
        Map<String, String[]> params = new HashMap<String, String[]>(req.getParameterMap());
        params.put("system", new String[] {"0"}); // overwrite any system value coming in: system promotions can only be side-loaded.
        Promo_Data p = Promo_Factory.init(params, errors);

        req.throwIfErrors(errors);

        if (p.write(C) == false)
          {
            p = Promo_Factory.lookupByCode(p.getCode());
            if (p.read(C) == true)
              throw new DuplicateResourceException("Promotion", p.getCode());
            throw new Error("Cannot write Promotion to the database due to an unknown error.");
          }

        Res.successJson("", p);
      }

  }
