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

import com.google.gson.annotations.SerializedName;

public class SSOConfig implements Cloneable
  {
    /*@formatter:off*/
    @SerializedName("id"                        ) public String  _id                         = null ;
    @SerializedName("identityProviderConfigFile") public String  _identityProviderConfigFile = null ;
    @SerializedName("serviceProviderConfigFile" ) public String  _serviceProviderConfigFile  = null ;
    @SerializedName("identityProviderEntityId"  ) public String  _identityProviderEntityId   = null ;
    @SerializedName("redirectUrl"               ) public String  _redirectUrl                = null ;
    @SerializedName("keyStorePath"              ) public String  _keyStorePath               = null ;
    @SerializedName("keyStorePswd"              ) public String  _keyStorePswd               = null ;
    @SerializedName("defaultPromoCode"          ) public String  _defaultPromoCode           = null ;
    @SerializedName("eula"                      ) public boolean _eula                       = true ;
    
    /*@formatter:on*/


    // Override clone() method
    @Override
    public SSOConfig clone()
    throws CloneNotSupportedException
      {
        return (SSOConfig) super.clone();
      }

  }
