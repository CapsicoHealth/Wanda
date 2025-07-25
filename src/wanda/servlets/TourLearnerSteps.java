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
import wanda.data.TourUserDetailsView_Data;
import wanda.data.TourUserDetailsView_Factory;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;

@WebServlet("/svc/tour/learner/steps")
public class TourLearnerSteps extends SimpleServlet
  {
    private static final long serialVersionUID = -1745307937763620646L;

    public TourLearnerSteps()
      {
        super(true, false, true);
      }

    @Override
    protected void justDo(RequestUtil req, ResponseUtil res, Connection C, User_Data U)
    throws Exception
      {
        String tourId = req.getParamString("tourId", true);
        String contextId = req.getParamString("contextId", true);
        String type = req.getParamString("type", true); // General type such as 'Cohorts', 'LLMs'...
        String topic = req.getParamString("topic", true); // Topic such as CARDIOLOGY, ONGOLOGY etc...
        
        req.throwIfErrors();

        TourUserDetailsView_Data tourDetails = TourUserDetailsView_Factory.lookupByUserContextTypeTopicTour(U.getRefnum(), contextId, type, topic, tourId);
        if (tourDetails.read(C) == false)
         res.success();
        else
         res.successJson("", tourDetails);

      }
  }
