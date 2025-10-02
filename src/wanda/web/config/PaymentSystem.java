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

package wanda.web.config;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.annotations.SerializedName;

import tilda.utils.TextUtil;

public class PaymentSystem
  {
    /*@formatter:off*/
    @SerializedName("id"      ) public String  _id        = null;
    @SerializedName("clientId") public String  _clientId  = null;
    @SerializedName("secret"  ) public String  _secret    = null;
    @SerializedName("sandbox" ) public boolean _sandbox   = true;
    /*@formatter:on*/

    protected static boolean validate(List<PaymentSystem> paymentSystems, boolean OK)
      {
        if (paymentSystems == null)
          return OK;

        Set<String> paymentSystemsIds = new HashSet<String>();
        for (int i = 0; i < paymentSystems.size(); ++i)
          {
            PaymentSystem PS = paymentSystems.get(i);
            if (PS == null) // cleanup nulls, which can come from dangling commas in the source json.
              {
                paymentSystems.remove(i);
                --i;
                continue;
              }
            if (TextUtil.isNullOrEmpty(PS._id) == true)
              {
                Wanda.LOG.error("The PaymentSystem #" + (++i) + " is missing a value for the attribute 'id'.");
                OK = false;
                continue;
              }
            if (TextUtil.isNullOrEmpty(PS._clientId) == true)
              {
                Wanda.LOG.error("The PaymentSystem #" + (++i) + " is missing a value for the attribute 'clientId'.");
                OK = false;
                continue;
              }
            if (TextUtil.isNullOrEmpty(PS._secret) == true)
              {
                Wanda.LOG.error("The PaymentSystem #" + (++i) + " is missing a value for the attribute 'secret'.");
                OK = false;
                continue;
              }

            if (paymentSystemsIds.add(PS._id+"/"+PS._sandbox) == false)
              {
                Wanda.LOG.error("There are multiple PaymentSystem with the same id/sandbox flag '" + PS._id+"/"+PS._sandbox + "'.");
                OK = false;
              }
          }
        return OK;
      }
  }
