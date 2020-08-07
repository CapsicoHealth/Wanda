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

import javax.servlet.annotation.WebServlet;

import tilda.db.Connection;
import tilda.db.ListResults;
import wanda.data.UserDetail_Data;
import wanda.data.UserDetail_Factory;
import wanda.data.User_Data;
import wanda.data.User_Factory;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.exceptions.BadRequestException;

@WebServlet("/svc/Verifications")
public class Verifications extends SimpleServlet
  {
    private static final long serialVersionUID = -4932144487665406560L;

    public Verifications()
      {
        super(false, true);
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        String action = Req.getParamString("action", true);
        String token  = Req.getParamString("token", true);
        Req.throwIfErrors();
        
        if (action.equals("emailVerification"))
          {
            ListResults<User_Data> users = User_Factory.lookupWhereEmailVerificationCodeLike(C, token, 0, 1);
            if ( users.size() > 0 && users.get(0).read(C) == true) {
              User_Data user = users.get(0);
              user.setEmail(user.getEmailUnverified());
              user.setEmailUnverifiedNull();
              user.setEmailVerificationCodeNull();
              user.write(C);
              
              UserDetail_Data contact = UserDetail_Factory.lookupByUserRefnum(user.getRefnum());
              if (contact.read(C) == false)
                {
                  contact = UserDetail_Factory.create(user.getRefnum(), user.getId(), user.getId());
                }
              contact.setEmailHome(user.getEmail());
              contact.write(C);              
              
            } else {
              throw new BadRequestException("token", "is Invalid / Expired");
            }
          }
        Res.Success();
      }

  }
