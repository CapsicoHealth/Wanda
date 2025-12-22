/* ===========================================================================
 * Copyright (C) 2024 CapsicoHealth Inc.
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

import java.util.List;
import java.util.stream.Stream;

import com.google.gson.annotations.SerializedName;

import tilda.db.Connection;
import tilda.utils.TextUtil;
import wanda.data.AppView_Data;
import wanda.data.AppView_Factory;
import wanda.data.Promo_Data;
import wanda.data.Promo_Factory;
import wanda.data.Tenant_Data;
import wanda.data.Tenant_Factory;

public class GuestRegistration
  {
    public static enum GuestType
      {
      NONE, PLAIN, PROMO
      }

    /*@formatter:off*/
    @SerializedName("type"            ) public GuestType              _type   = GuestType.NONE;
    @SerializedName("buttonLabel"     ) public String                 _buttonLabel = "Free Trial";
    @SerializedName("defaultPromoCode") public String                 _defaultPromoCode = null;
    @SerializedName("defaultApps"     ) public GuestRegistrationApp[] _defaultApps = null;
    @SerializedName("tenantIds"       ) public String[]               _tenantIds = null;
    @SerializedName("excludedDomains" ) public String[]               _excludedDomains = null;
    
    /*@formatter:on*/

    public transient long[]       _appRefnums      = null;
    public transient long[]       _tenantRefnums   = null;


    protected static boolean validate(Connection C, GuestRegistration gr, boolean OK)
    throws Exception
      {
        if (gr != null && gr._type != GuestType.NONE)
          {
            if (gr._defaultApps == null || gr._defaultApps.length == 0)
              {
                Wanda.LOG.error("The guestRegistration appIds is empty or unspecified. If allowed is true, there must be at least one aplication listed.");
                OK = false;
              }
            else
              {
                String[] appIds = Stream.of(gr._defaultApps).map(e -> e._id).toArray(String[]::new);

                List<AppView_Data> AL = AppView_Factory.lookupWhereIds(C, Wanda.getHostName(), appIds, 0, -1);
                if (AL.size() != appIds.length)
                  {
                    Wanda.LOG.warn("Some guestRegistration appIds cannot be found in the database.");
                    for (String appId : appIds)
                      {
                        boolean found = false;
                        for (AppView_Data av : AL)
                          if (av.getAppId().equals(appId) == true)
                            {
                              found = true;
                              break;
                            }
                        if (found == false)
                          Wanda.LOG.warn("      - " + appId);
                      }
                  }
                else
                  {
                    gr._appRefnums = new long[AL.size()];
                    for (int i = 0; i < AL.size(); ++i)
                      gr._appRefnums[i] = AL.get(i).getAppRefnum();
                  }
              }

            // Gotta check if the system is in multi-tenant mode or not.
            List<Tenant_Data> TL = Tenant_Factory.lookupWhereActive(C, 0, -1);
            if (TL.size() > 0 && (gr._tenantIds == null || gr._tenantIds.length == 0))
              {
                Wanda.LOG.error("The guestRegistration tenantIds is empty or unspecified. If allowed is true, there must be at least one tenant listed.");
                OK = false;
              }
            else
              {
                TL = Tenant_Factory.lookupWhereNames(C, gr._tenantIds, 0, -1);
                if (TL.size() != (gr._tenantIds == null ? 0 : gr._tenantIds.length))
                  {
                    Wanda.LOG.error("The guestRegistration tenantIds specifies tenant ids which cannot be found in the database.");
                    OK = false;
                  }
                else
                  {
                    gr._tenantRefnums = new long[TL.size()];
                    for (int i = 0; i < TL.size(); ++i)
                      gr._tenantRefnums[i] = TL.get(i).getRefnum();
                  }
              }
          }

        if (gr != null && gr._excludedDomains != null)
          {
            for (int i = 0; i < gr._excludedDomains.length; ++i)
              gr._excludedDomains[i] = "@" + gr._excludedDomains[i].toLowerCase().trim();
          }
        
        if (gr != null && TextUtil.isNullOrEmpty(gr._defaultPromoCode) == false)
          {
            Promo_Data P = Promo_Factory.lookupByCode(gr._defaultPromoCode);
            if (P.read(C) == false)
              {
                Wanda.LOG.error("The guestRegistration defaultPromo '" + gr._defaultPromoCode + "' cannot be found in the database.");
                OK = false;
              }
          }

        return OK;
      }


    public boolean isAllowedDomain(String userEmail)
      {
        if (_excludedDomains == null || _excludedDomains.length == 0)
          return true;
        String email = userEmail.toLowerCase().trim();
        for (String domain : _excludedDomains)
          if (email.endsWith(domain) == true)
            return false;
        return true;
      }
  }
