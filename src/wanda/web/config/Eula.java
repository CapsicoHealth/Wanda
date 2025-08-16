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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.annotations.SerializedName;

import tilda.utils.TextUtil;

public class Eula
  {
    /*@formatter:off*/
    @SerializedName("name" ) public String _name   = null;
    @SerializedName("descr") public String _descr  = null;
    @SerializedName("url"  ) public String _url    = null;
    @SerializedName("activations"  ) public EulaActivation[] _activations    = new EulaActivation[] { };
    /*@formatter:on*/


    protected static boolean validate(List<Eula> eulas, boolean OK)
      {
        if (eulas == null)
          return OK;

        Set<String> eulaActivationTenantNames = new HashSet<String>();
        Set<String> eulaNames = new HashSet<String>();
        Set<String> eulaUrls = new HashSet<String>();
        for (int i = 0; i < eulas.size(); ++i)
          {
            Eula E = eulas.get(i);
            if (E == null) // cleanup nulls, which can come from dangling commas in the source json.
              {
                eulas.remove(i);
                --i;
                continue;
              }
            if (TextUtil.isNullOrEmpty(E._name) == true)
              {
                Wanda.LOG.error("The EULA #" + (++i) + " is missing a value for the attribute 'name'.");
                OK = false;
                continue;
              }
            if (TextUtil.isNullOrEmpty(E._url) == true)
              {
                Wanda.LOG.error("The EULA #" + (++i) + " is missing a value for the attribute 'url'.");
                OK = false;
                continue;
              }
            if (eulaNames.add(E._name.toLowerCase()) == false)
              {
                Wanda.LOG.error("There are multiple EULAs with the same name '" + E._name + "'.");
                OK = false;
              }
            if (eulaUrls.add(E._url.toLowerCase()) == false)
              {
                Wanda.LOG.error("There are multiple EULAs with the same URL '" + E._url + "'.");
                OK = false;
              }

            for (EulaActivation EA : E._activations)
              if (EA != null)
                {
                  if (EA._tenantName == null)
                    {
                      Wanda.LOG.error("An activation for the EULA '" + E._name + "' is missing a value for the attribute 'tenantName'.");
                      OK = false;
                      continue;
                    }
                  if (EA._renewalDays > 0 && eulaActivationTenantNames.add(EA._tenantName) == false)
                    {
                      Wanda.LOG.error("There are duplicate active EULAs for the tenant '" + EA._tenantName + "'.");
                      OK = false;
                    }
                  EA._eulaUrl = E._url;
                }
          }
        return OK;
      }
  }
