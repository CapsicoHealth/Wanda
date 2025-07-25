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

public class GuestRegistration
  {
    public static enum GuestType
      {
      NONE, PLAIN, PROMO
      }

    /*@formatter:off*/
    @SerializedName("type"       ) public GuestType              _type   = GuestType.NONE;
    @SerializedName("buttonLabel") public String                 _buttonLabel = "Free Trial";
    @SerializedName("defaultApps") public GuestRegistrationApp[] _defaultApps = null;
    @SerializedName("tenantIds"  ) public String[]               _tenantIds = null;
    /*@formatter:on*/

    public transient long[]       _appRefnums             = null;
    public transient long[]       _tenantRefnums          = null;

  }
