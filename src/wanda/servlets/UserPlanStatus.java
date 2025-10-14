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
import tilda.utils.DateTimeUtil;
import tilda.utils.json.JSONPrinter;
import wanda.data.UserBillingView_Data;
import wanda.data.UserBillingView_Factory;
import wanda.data.User_Data;
import wanda.data.importers.promos.Plan;
import wanda.servlets.helpers.PlanHelper;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;

@WebServlet("/svc/user/plan/status")
public class UserPlanStatus extends SimpleServlet
  {
    private static final long serialVersionUID = 2358369573367773870L;

    public UserPlanStatus()
      {
        super(true, false, true);
      }

    @Override
    public void init(ServletConfig Conf)
      {
      }

    @Override
    protected void justDo(RequestUtil req, ResponseUtil res, Connection C, User_Data U)
    throws Exception
      {
        List<Plan> plans = PlanHelper.getAvailablePlans(C, U);
        List<UserBillingView_Data> UBVL = U == null ? null : UserBillingView_Factory.lookupWhereUserRefnum(C, U.getRefnum(), 0, 24);
        boolean current = isCurrent(UBVL); 
        JSONPrinter j = new JSONPrinter();
        j.addElement("plans", plans, "");
        j.addElement("billingHistory", UBVL, "");
        j.addElement("billingCurrent", current);
        res.successJson(j);
      }

    /**
     * Check if the user's billing status is current.
     * <UL><LI>Subscription is active</LI>
     *  <LI>Billing is active</LI>
     *  <LI>Billing status is "Paid"</LI>
     *  <LI>Billing expiry date is in the future</LI>
     *  </UL>
     * @param UBVL
     * @return
     */
    private static boolean isCurrent(List<UserBillingView_Data> UBVL)
      {
        if (UBVL==null || UBVL.isEmpty() == true)
          return false;
        
        return UBVL.get(0).getActive();
      }

  }
