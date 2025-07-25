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

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import wanda.data.TourUserClick_Data;
import wanda.data.TourUserClick_Factory;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;

@WebServlet("/svc/tour/learner/click")
public class TourLearnerClick extends SimpleServlet
  {
    private static final long serialVersionUID = -1745307937763620646L;

    public TourLearnerClick()
      {
        super(true, true, true);
      }

    @Override
    protected void justDo(RequestUtil req, ResponseUtil res, Connection C, User_Data U)
    throws Exception
      {
        String contextId = req.getParamString("contextId", true);
        String type = req.getParamString("type", true);
        String topic = req.getParamString("topic", true);
        String tourId = req.getParamString("tourId", true);
        String partId = req.getParamString("partId", true);
        String stepId = req.getParamString("stepId", true);
        
        req.throwIfErrors();
        
        TourUserClick_Data utc = TourUserClick_Factory.create(U.getRefnum(), contextId, type, topic, tourId, partId, stepId);
        if (utc.write(C) == false)
         throw new Exception("Database error writing a udser tour click record.");
        
      }
  }
