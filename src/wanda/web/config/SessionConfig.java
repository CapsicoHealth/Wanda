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
    @SerializedName("forceReLoginMins"    ) public int _forceReLoginMins     = 240;
    /*@formatter:on*/

    public boolean validate(boolean OK)
      {
        if (_passwordExpiryDays < 60)
          {
            Wanda.LOG.error("The Wanda configuration file defines a 'sessionConfig' property with a 'passwordExpiryDays' value of " + _passwordExpiryDays + " < 60.");
            OK = false;
          }

        if (_loginOrResetAttempts < 3)
          {
            Wanda.LOG.error("The Wanda configuration file defines a 'sessionConfig' property with a '_loginOrResetAttempts' value of" + _loginOrResetAttempts + " < 3.");
            OK = false;
          }
        if (_withinMins < 3)
          {
            Wanda.LOG.error("The Wanda configuration file defines a 'sessionConfig' property with a 'withinMins' value of " + _withinMins + " < 3.");
            OK = false;
          }
        if (_lockForMins < 1)
          {
            Wanda.LOG.error("The Wanda configuration file defines a 'sessionConfig' property with a 'lockForMins' value of " + _lockForMins + " < 10.");
            OK = false;
          }
        if (_failedLoginCycle < 2)
          {
            Wanda.LOG.error("The Wanda configuration file defines a 'sessionConfig' property with a 'failedLoginCycle' value of " + _failedLoginCycle + " < 2.");
            OK = false;
          }
        if (_lockForeverDays < 1)
          {
            Wanda.LOG.error("The Wanda configuration file defines a 'sessionConfig' property with a 'lockForeverDays' value of " + _lockForeverDays + " < 360.");
            OK = false;
          }
        if (_resetCodeExpiryMins < 10)
          {
            Wanda.LOG.error("The Wanda configuration file defines a 'sessionConfig' property with a 'resetCodeExpiryMins' value of " + _resetCodeExpiryMins + " < 10.");
            OK = false;
          }
        if (_maxPswdHistory < 2)
          {
            Wanda.LOG.error("The Wanda configuration file defines a 'sessionConfig' property with a 'maxPswdHistory' value of " + _maxPswdHistory + " < 2.");
            OK = false;
          }
        if (_forceReLoginMins == 0)
          {
            Wanda.LOG.error("The Wanda configuration file defines a 'sessionConfig' property with a 'forceReLoginMins' value of " + _forceReLoginMins + " == 0.");
            OK = false;
          }
        
        return OK;
      }
  }
