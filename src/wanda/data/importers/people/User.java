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

package wanda.data.importers.people;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

import tilda.db.Connection;
import wanda.data.AppUser_Data;
import wanda.data.AppUser_Factory;
import wanda.data.App_Data;
import wanda.data.App_Factory;
import wanda.data.UserDetail_Data;
import wanda.data.User_Data;

public class User
  {
    /*@formatter:off*/
    @SerializedName("user"     ) public User_Data        _User   = null;
    @SerializedName("details"  ) public UserDetail_Data  _Detail = null;
    @SerializedName("apps"     ) public String[]         _Apps = null;
    /*@formatter:on*/

    public int write(Connection C)
    throws Exception
      {
        int Count = 0;

        if (_User == null)
          throw new Exception("The element 'person' cannot be null or missing.");
        
        _User.setPswdCreateNow();

        if (_User.upsert(C, true) == false)
          throw new Exception("Cannot write User record");
        _User.refresh(C); // to get the refnum if the row was updated from the DB.
        ++Count;

        if (_Detail != null)
          {
            _Detail.initUserRefnum(_User.getRefnum());
            if (_Detail.upsert(C, true) == false)
              throw new Exception("Cannot upsert UserDetail record");
            ++Count;
          }
        if (_Apps != null)
          {
            List<AppUser_Data> L = new ArrayList<AppUser_Data>();
            for (String a : _Apps)
              {
                App_Data A = App_Factory.lookupById(a);
                if (A.read(C) == true)
                  {
                    AppUser_Data AU = AppUser_Factory.create(A.getRefnum());
                    AU.setUserRefnum(_User.getRefnum());
                    L.add(AU);
                    AU.write(C);
                  }
              }
          }

        return Count;
      }
    /*@formatter:on*/
  }
