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

import java.time.ZonedDateTime;
import java.util.List;

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import tilda.utils.TextUtil;
import wanda.data.TourUserStatusView_Data;
import wanda.data.TourUserStatusView_Factory;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.exceptions.AccessForbiddenException;

@WebServlet("/svc/tour/learner/status")
public class TourLearnerStatus extends SimpleServlet
  {
    private static final long serialVersionUID = -1745307937763620646L;

    public TourLearnerStatus()
      {
        super(true, false, true, APIKeyEnum.ALLOWED);
      }

    @Override
    protected void justDo(RequestUtil req, ResponseUtil res, Connection C, User_Data U)
    throws Exception
      {
        processStatusRequest(req, res, C, U);
      }

    public static void processStatusRequest(RequestUtil req, ResponseUtil res, Connection C, User_Data U)
    throws Exception
      {
        ZonedDateTime lastDateTime = req.getParamZonedDateTime("lastDateTime", true);
        String userId = req.getParamString("userId", false);

        req.throwIfErrors();

        List<TourUserStatusView_Data> L;

        // API Call
        if (req.getApiCallSsoId() != null)
          L = TourUserStatusView_Factory.lookupWhereLastAccessed(C, lastDateTime, 0, 10_000);
        else // Regular user call
          {
            if (U.isSuperAdmin() == false) // regular users must only call for themselves
              {
                if (TextUtil.isNullOrEmpty(userId) == true)
                  userId = U.getId();
                else if (U.getId().equalsIgnoreCase(userId) == false)
                  throw new AccessForbiddenException("User", userId);
              }

            L = TourUserStatusView_Factory.lookupWhereUserLastAccessed(C, userId, lastDateTime, 0, 10_000);
          }

        res.successJson("export", L);

      }
  }
