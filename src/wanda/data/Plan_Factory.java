/*
 Tilda V2.5 template application class.
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

    public static List<Plan> getPlans(String[] planCodes, short discountPct, short discountMonths, boolean autoRenew)
      {
        List<Plan> L = new ArrayList<Plan>();
        if (planCodes != null)
          for (String pc : planCodes)
            for (Plan P : _PLANS)
              if (P._Plan.getCode().equals(pc) == true && P._Plan.isCurrentlyActiveToday() == true)
                L.add(new Plan(P, discountPct, discountMonths, autoRenew));
        return L;
      }

  }
