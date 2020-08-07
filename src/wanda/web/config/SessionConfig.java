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

public class SessionConfig
  {
    /*@formatter:off*/
    @SerializedName("loginOrResetAttempts") public int _loginOrResetAttempts = 3;
    @SerializedName("withinMins"          ) public int _withinMins           = 5;
    @SerializedName("lockForMins"         ) public int _lockForMins          = 10;
    @SerializedName("failedLoginCycle"    ) public int _failedLoginCycle     = 2;
    @SerializedName("lockForeverDays"     ) public int _lockForeverDays      = 720;
    @SerializedName("resetCodeExpiryMins" ) public int _resetCodeExpiryMins  = 30;
    @SerializedName("passwordExpiryDays"  ) public int _passwordExpiryDays   = 60;
    @SerializedName("maxPswdHistory"      ) public int _maxPswdHistory       = 3;
    @SerializedName("forceReLoginMins"    ) public int _forceReLoginMins     = 60;
    /*@formatter:on*/
  }
