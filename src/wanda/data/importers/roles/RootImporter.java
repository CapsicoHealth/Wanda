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

package wanda.data.importers.roles;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

import tilda.Importer;
import tilda.db.Connection;
import wanda.data.Role_Data;
import wanda.data.Role_Factory;

public class RootImporter implements Importer
  {
    /*@formatter:off*/
    @SerializedName("roles" ) public List<Role_Data> _Roles = new ArrayList<Role_Data>();
    /*@formatter:on*/

    @Override
    public int process(Connection C)
    throws Exception
      {
        int Count = 0;

        for (Role_Data obj : _Roles)
          {
            ++Count;
            // general upsert!
            if (obj.upsert(C) == false)
              throw new Exception("Cannot upsert Role record");
          }
        Role_Factory.initMappings(C);

        return Count;
      }
    /*@formatter:on*/
  }
