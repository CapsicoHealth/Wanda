/* ===========================================================================
 * Copyright (C) 2025 CapsicoHealth Inc.
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

package wanda.data;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.*;
import wanda.data.importers.promos.Plan;

/**
 * This is the application class <B>Data_Plan</B> mapped to the table <B>WANDA.Plan</B>.
 * 
 * @see wanda.data._Tilda.TILDA__PLAN
 */
public class Plan_Factory extends wanda.data._Tilda.TILDA__PLAN_Factory
  {
    protected static final Logger LOG = LogManager.getLogger(Plan_Factory.class.getName());

    protected Plan_Factory()
      {
      }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Implement your customizations, if any, below.
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static List<Plan> _PLANS = new ArrayList<Plan>();

    public static void init(Connection C)
    throws Exception
      {
        List<Plan_Data> PL = Plan_Factory.lookupWhereAllByPositions(C, 0, -1);
        List<PlanPricing_Data> PPL = PlanPricing_Factory.lookupWhereAllByCurrency(C, 0, -1);
        for (Plan_Data p : PL)
          {
            Plan P = new Plan(p);
            for (PlanPricing_Data pp : PPL)
              {
                if (pp.getPlanRefnum() == p.getRefnum())
                  P._Pricings.add(pp);
              }
            _PLANS.add(P);
          }
      }

    public static List<Plan> getPlans(String[] planCodes, short discountPct, short discountMonths, short discountPctYear, boolean autoRenew)
      {
        List<Plan> L = new ArrayList<Plan>();
        if (planCodes != null)
          for (String pc : planCodes)
            for (Plan P : _PLANS)
              if (P._Plan.getCode().equals(pc) == true && P._Plan.isCurrentlyActiveToday() == true)
                L.add(new Plan(P, discountPct, discountMonths, discountPctYear, autoRenew));
        return L;
      }

    public static List<Plan> getPlans()
      {
        List<Plan> L = new ArrayList<Plan>();
        for (Plan P : _PLANS)
          if (P._Plan.isCurrentlyActiveToday() == true)
            L.add(new Plan(P, (short) 0, (short) 0, (short) 0, false));
        return L;
      }

  }
