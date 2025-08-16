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

package wanda.web.config;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

import tilda.utils.TextUtil;
import wanda.web.BeaconBit;

public class BeaconConfig
  {
    /*@formatter:off*/
    @SerializedName("emails") public String[]                 _emails= new String[] { };
    @SerializedName("bits"  ) public List<BeaconBitConfig>    _bits  = new ArrayList<BeaconBitConfig>();
    /*@formatter:on*/

    protected static boolean validate(BeaconConfig beacon, boolean OK)
      {
        if (beacon == null || beacon._bits != null)
          return OK;

        for (BeaconBitConfig b : beacon._bits)
          {
            if (b == null)
              continue;

            if (TextUtil.isNullOrEmpty(b._className) == true)
              {
                Wanda.LOG.error("A beacon definition is missing a value for the attribute 'className'.");
                OK = false;
              }
            else
              {
                try
                  {
                    b._bitObj = (BeaconBit) Class.forName(b._className).newInstance();
                  }
                catch (Throwable T)
                  {
                    Wanda.LOG.error("The class '" + b._className + " could not be found or does not implement the wanda.web.BeaconBit interface.\n", T);
                    OK = false;
                  }
              }

            if (b._timing == null)
              {
                Wanda.LOG.error("The beacon definition '" + b._className + "' doesn't defines a timing (should be either DAY, HOUR or MINUTE).");
                OK = false;
              }
          }
        return OK;
      }
  }
