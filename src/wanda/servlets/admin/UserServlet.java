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

import javax.servlet.annotation.WebServlet;

import wanda.data.User_Data;
import wanda.data.User_Factory;
import wanda.servlets.helpers.RoleHelper;

import tilda.db.Connection;
import tilda.utils.DateTimeUtil;
import tilda.utils.EncryptionUtil;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.exceptions.NotFoundException;

/**
*
* @author mohan
* API to
* reset password
* Cancel Invite
* Lock User account
*/


@WebServlet("/svc/admin/user")
public class UserServlet extends SimpleServlet
  {

    private static final long serialVersionUID = 5234430667440570139L;

    public UserServlet()
      {
        super(true);
      }

    @Override
    protected void justDo(RequestUtil req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        throwIfUserInvalidRole(U, RoleHelper.ADMINROLES);
        long userRefnum = req.getParamLong("refnum", true);

        int lock = req.getParamInt("lock", false);
        boolean resetPswd = req.getParamBoolean("resetPswd", false);
        boolean cancelInvite = req.getParamBoolean("cancelInvite", false);

        User_Data user = User_Factory.lookupByPrimaryKey(userRefnum);
        if (user.read(C) == false)
          {
            throw new NotFoundException("refnum", "User not found with refnum: " + userRefnum);
          }
        if (cancelInvite)
          {
            user.setPswdResetCode(EncryptionUtil.getToken(16, true));
            user.setInviteCancelled(true);
          }
        
        // 1 = lock, 0 = unlock
        if (lock == 1)
          user.setLocked(DateTimeUtil.nowUTC().plusYears(100));
        else if (lock == 0)
          user.setLockedNull();

        if (resetPswd)
          user.sendForgotPswdEmail(C);
        user.write(C);
        Res.success();        
      }

  }
