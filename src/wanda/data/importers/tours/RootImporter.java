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

package wanda.data.importers.tours;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.annotations.SerializedName;

import tilda.Importer;
import tilda.db.Connection;
import wanda.data.TourPart_Data;
import wanda.data.TourStep_Data;
import wanda.data.TourStep_Factory;
import wanda.data.Tour_Data;
import wanda.data.Tour_Factory;

public class RootImporter implements Importer
  {
    /*@formatter:off*/
    @SerializedName("id"    ) public String _id;
    @SerializedName("parts" ) public List<TourPart_Data> _parts = new ArrayList<TourPart_Data>();
    /*@formatter:on*/

    @Override
    public int process(Connection C)
    throws Exception
      {
        int Count = 0;

        Tour_Data t = Tour_Factory.create(_id);
        if (t.upsert(C) == false)
          throw new Exception("Cannot upsert Tour record '" + _id + "'.");

        short partPos = 0;
        Set<String> partIds = new HashSet<String>();
        for (TourPart_Data p : _parts)
          {
            if (p == null)
              continue;
            if (partIds.add(p.getId()) == false)
             throw new Exception("Duplicate part Id '" + p.getId() + "' for Tour '" + _id + "'.");
            ++Count;
            p.setTourRefnum(t.getRefnum());
            p.setPos(++partPos);
            if (p.upsert(C) == false)
              throw new Exception("Cannot upsert Part '" + p.getId() + "' for Tour '" + _id + "'.");
            short stepPos = 0;
            Set<String> stepIds = new HashSet<String>();
            TourStep_Factory.shiftOutSteps(C, p.getRefnum());
            for (TourStep_Data s : p._steps)
              {
                if (s == null)
                  continue;
                if (stepIds.add(s.getId()) == false)
                  throw new Exception("Duplicate Step '"+s.getId()+"' for Part '" + p.getId() + "', for Tour '" + _id + "'.");
                ++Count;
                s.setTourPartRefnum(p.getRefnum());
                s.setPos(++stepPos);
                // It is possible for parts to be moved around, so we might need to shuffle things around.
                if (s.upsert(C) == false)
                  throw new Exception("Cannot upsert Step '"+s.getId()+"' for Part '" + p.getId() + "', for Tour '" + _id + "'.");
              }
             TourStep_Factory.cleanOldSteps(C, p.getRefnum());
          }

        return Count;
      }
  }
