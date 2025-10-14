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
    public static class Credentials
      {
        /*@formatter:off*/
        @SerializedName("clientId") public String  _clientId  = null;
        @SerializedName("secret"  ) public String  _secret    = null;
        /*@formatter:on*/

        public boolean validate(int i, boolean OK)
          {
            if (TextUtil.isNullOrEmpty(_clientId) == true)
              {
                Wanda.LOG.error("The PaymentSystem #" + i + " is missing a value for the attribute 'clientId'.");
                OK = false;
              }
            if (TextUtil.isNullOrEmpty(_secret) == true)
              {
                Wanda.LOG.error("The PaymentSystem #" + i + " is missing a value for the attribute 'secret'.");
                OK = false;
              }
            return OK;
          }
      }

    /*@formatter:off*/
    @SerializedName("id"         ) public String      _id          = null;
    @SerializedName("sandbox"    ) public Credentials _sandbox     = null;
    @SerializedName("prod"       ) public Credentials _prod        = null;
    @SerializedName("sandboxMode") public boolean     _sandboxMode = true;
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
                Wanda.LOG.error("The PaymentSystem #" + i + " is missing a value for the attribute 'id'.");
                OK = false;
                continue;
              }
            if (PS._sandbox == null)
              {
                Wanda.LOG.error("The PaymentSystem #" + i + " is missing 'sandbox'.");
                OK = false;
                continue;
              }
            OK = PS._sandbox.validate(i, OK);

            if (PS._prod == null)
              {
                Wanda.LOG.error("The PaymentSystem #" + i + " is missing 'prod'.");
                OK = false;
                continue;
              }
            OK = PS._prod.validate(i, OK);

            if (paymentSystemsIds.add(PS._id) == false)
              {
                Wanda.LOG.error("The PaymentSystem #" + i + " is a duplicte of a prior PaymentSystem with the same id '" + PS._id + "'.");
                OK = false;
              }
          }
        return OK;
      }

    public Credentials getCredentials()
      {
        return _sandboxMode == true ? _sandbox : _prod;
      }
  }
