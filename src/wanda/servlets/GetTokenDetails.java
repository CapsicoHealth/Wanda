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

import tilda.db.Connection;
import tilda.utils.json.JSONUtil;
import wanda.data.UserResetPasswordView_Data;
import wanda.data.UserResetPasswordView_Factory;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.exceptions.NotFoundException;


@WebServlet("/svc/user/token")
public class GetTokenDetails extends SimpleServlet
  {

    private static final long serialVersionUID = 988554219257979935L;

    public GetTokenDetails()
      {
        super(false);
      }

    @Override
    protected void justDo(RequestUtil req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        String token = req.getParamString("token", true);
        req.throwIfErrors();

        UserResetPasswordView_Data user = UserResetPasswordView_Factory.lookupByPswdResetCode(token);
        if (user.read(C) == false)
          throw new NotFoundException("token", "" + token);

        PrintWriter Out = Res.setContentType(ResponseUtil.ContentType.JSON);
        JSONUtil.response(Out, "Mini", user);
      }
  }
