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

package wanda.data.importers.promos;

import java.util.List;

import com.google.gson.annotations.SerializedName;

import tilda.db.Connection;
import wanda.data.PlanPricing_Data;
import wanda.data.Plan_Data;

public class Plan
  {
    /*@formatter:off*/
    @SerializedName("plan"    ) public Plan_Data              _Plan     = null;
    @SerializedName("pricings") public List<PlanPricing_Data> _Pricings = null;
    /*@formatter:on*/

    public int write(Connection C)
    throws Exception
      {
        int count = 0;

        if (_Plan == null)
          throw new Exception("The element 'plan' cannot be null.");

        if (_Plan.upsert(C) == false)
          throw new Exception("Cannot upsert Plan record");
        _Plan.refresh(C); // to get the refnum if the row was updated from the DB.
        ++count;

        if (_Pricings != null)
          for (PlanPricing_Data obj : _Pricings)
            {
              if (obj == null)
                continue;
              obj.setPlanRefnum(_Plan.getRefnum());
              if (obj.upsert(C) == false)
                throw new Exception("Cannot upsert PlanPricing record");
              ++count;
            }

        return count;
      }
  }
