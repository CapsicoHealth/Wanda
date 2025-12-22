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
import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

import tilda.Importer;
import tilda.db.Connection;
import wanda.data.Promo_Data;

public class RootImporter implements Importer
  {
    /*@formatter:off*/
    @SerializedName("plans"  ) public List<Plan> _Plans = new ArrayList<Plan>();
    @SerializedName("promos" ) public List<Promo_Data> _Promos = new ArrayList<Promo_Data>();
    /*@formatter:on*/

    @Override
    public int process(Connection C)
    throws Exception
      {
        int count = 0;

        for (Plan obj : _Plans)
          count+= obj.write(C);

        for (Promo_Data obj : _Promos)
          {
            ++count;
            if (obj.upsert(C) == false)
              throw new Exception("Cannot upsert Promo record");
          }

        return count;
      }
    /*@formatter:on*/
  }
