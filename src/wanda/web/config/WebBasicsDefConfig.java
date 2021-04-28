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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.annotations.SerializedName;

import tilda.utils.TextUtil;
import wanda.web.BeaconBit;

public class WebBasicsDefConfig
  {
    /*@formatter:off*/
    @SerializedName("appName"         )         String                              _appName                = null;
    @SerializedName("hostName"        )         String                              _hostName               = null;
    @SerializedName("appPath"         )         String                              _appPath                = null;
    @SerializedName("homePagePath"    )         String                              _homePagePath           = null;
    @SerializedName("email"           )         EmailConfigDetails                  _emailSettings          = null;
    @SerializedName("sessionConfig"   )         SessionConfig                       _sessionConfig          = null;
    @SerializedName("passwordRules"   )         List<PasswordRule>                  _passwordRules          = null;
    @SerializedName("resetEmailText"  )         List<String>                        _resetEmailTexts        = null;
    @SerializedName("inviteEmailText" )         List<String>                        _inviteEmailTexts       = null;
    @SerializedName("emailVerificationText")    List<String>                        _emailVerificationTexts = null;
    @SerializedName("lookAndFeel"     )         LookAndFeel                         _laf                    = null;
    @SerializedName("eulas"           )         List<Eula>                          _eulas                  = new ArrayList<Eula>();
    @SerializedName("twofishes-url"   )         String                              _twofishesUrl           = null;
    @SerializedName("tableauUrl"      )         String                               _tableauUrl             = null;
    @SerializedName("tableauUser"     )         String                               _tableauUser            = null;
    @SerializedName("jobCheckIntervalSeconds")  int                                 _jobCheckIntervalSeconds= 20;
    @SerializedName("beacon"          )         BeaconConfig                        _beacon                 = null;
    @SerializedName("extras"          )         Map<String, Map<String, String>>    _extras                 = null;
    /*@formatter:on*/

    public boolean validate()
    throws Exception
      {
        boolean OK = true;

        if (_appName == null)
          {
            WebBasics.LOG.error("The WebBasics configuration file didn't define any 'appName' property");
            OK = false;
          }
        if (_hostName == null)
          {
            WebBasics.LOG.error("The WebBasics configuration file didn't define any 'hostName' property");
            OK = false;
          }
        if (_appPath == null)
          {
            WebBasics.LOG.error("The WebBasics configuration file didn't define any 'appPath' property");
            OK = false;
          }
        if (_homePagePath == null)
          {
            WebBasics.LOG.error("The WebBasics configuration file didn't define any 'homePagePath' property");
            OK = false;
          }
        if (_twofishesUrl == null)
          {
            WebBasics.LOG.error("The WebBasics configuration file didn't define any 'twofishes-url' property");
            // OK = false;
          }

        if (_sessionConfig == null)
          {
            WebBasics.LOG.error("The WebBasics configuration file didn't define any 'sessionConfig' property");
            OK = false;
          }

        if (_sessionConfig._passwordExpiryDays < 60)
          {
            WebBasics.LOG.error("The WebBasics configuration file defines a 'sessionConfig' property with a 'passwordExpiryDays' value of " + _sessionConfig._passwordExpiryDays + " < 60.");
            OK = false;
          }

        if (_sessionConfig._loginOrResetAttempts < 3)
          {
            WebBasics.LOG.error("The WebBasics configuration file defines a 'sessionConfig' property with a '_loginOrResetAttempts' value of" + _sessionConfig._loginOrResetAttempts + " < 3.");
            OK = false;
          }
        if (_sessionConfig._withinMins < 3)
          {
            WebBasics.LOG.error("The WebBasics configuration file defines a 'sessionConfig' property with a 'withinMins' value of " + _sessionConfig._withinMins + " < 3.");
            OK = false;
          }
        if (_sessionConfig._lockForMins < 1)
          {
            WebBasics.LOG.error("The WebBasics configuration file defines a 'sessionConfig' property with a 'lockForMins' value of " + _sessionConfig._lockForMins + " < 10.");
            OK = false;
          }
        if (_sessionConfig._failedLoginCycle < 2)
          {
            WebBasics.LOG.error("The WebBasics configuration file defines a 'sessionConfig' property with a 'failedLoginCycle' value of " + _sessionConfig._failedLoginCycle + " < 2.");
            OK = false;
          }
        if (_sessionConfig._lockForeverDays < 1)
          {
            WebBasics.LOG.error("The WebBasics configuration file defines a 'sessionConfig' property with a 'lockForeverDays' value of " + _sessionConfig._lockForeverDays + " < 360.");
            OK = false;
          }
        if (_sessionConfig._resetCodeExpiryMins < 10)
          {
            WebBasics.LOG.error("The WebBasics configuration file defines a 'sessionConfig' property with a 'resetCodeExpiryMins' value of " + _sessionConfig._resetCodeExpiryMins + " < 10.");
            OK = false;
          }
        if (_sessionConfig._maxPswdHistory < 2)
          {
            WebBasics.LOG.error("The WebBasics configuration file defines a 'sessionConfig' property with a 'maxPswdHistory' value of " + _sessionConfig._maxPswdHistory + " < 2.");
            OK = false;
          }
        if (_sessionConfig._forceReLoginMins == 0)
          {
            WebBasics.LOG.error("The WebBasics configuration file defines a 'sessionConfig' property with a 'forceReLoginMins' value of " + _sessionConfig._forceReLoginMins + " == 0.");
            OK = false;
          }

        if (_passwordRules == null || _passwordRules.size() == 0)
          {
            WebBasics.LOG.error("No Password rules in the WebBasics.config.json");
            OK = false;
          }

        if (_emailSettings != null)
          {
            if (TextUtil.isNullOrEmpty(_emailSettings._smtp) == true)
              {
                WebBasics.LOG.error("Invalid Email Configuration in the WebBasics configuration file: the 'smtp' attribute is missing or empty.");
                OK = false;
              }
            if (TextUtil.isNullOrEmpty(_emailSettings._userId) == true)
              {
                WebBasics.LOG.error("Invalid Email Configuration in the WebBasics configuration file: the 'userId' attribute is missing or empty.");
                OK = false;
              }
            if (TextUtil.isNullOrEmpty(_emailSettings._pswd) == true)
              {
                WebBasics.LOG.warn("The Email Configuration in the WebBasics configuration file has a missing or empty 'pswd' attribute, which may be OK.");
              }
          }

        if (_laf == null)
          {
            WebBasics.LOG.error("No 'lookAndFeel' Configuration was defined in the WebBasics configuration file");
            OK = false;
          }
        else
          {
            if (TextUtil.isNullOrEmpty(_laf._logoBig) == true)
              {
                WebBasics.LOG.error("The property 'lookAndFeel.logoBig' in the WebBasics configuration file is missing");
                OK = false;
              }
            if (TextUtil.isNullOrEmpty(_laf._logoSmall) == true)
              _laf._logoSmall = "";
            if (TextUtil.isNullOrEmpty(_laf._pageTitle) == true)
              _laf._pageTitle = "";
          }
        Set<String> Names = new HashSet<String>();
        for (Eula E : _eulas)
          if (E != null)
            {
              if (Names.add(E._tenantName) == false)
                {
                  WebBasics.LOG.error("There are duplicate EULAs for the tenant '" + E._tenantName + "'.");
                  OK = false;
                }
            }

        if (_beacon != null && _beacon._bits != null)
          for (BeaconBitConfig b : _beacon._bits)
            {
              if (b == null)
                continue;

              if (TextUtil.isNullOrEmpty(b._className) == true)
                {
                  WebBasics.LOG.error("A beacon definition is missing a value for the attribute 'className'.");
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
                      WebBasics.LOG.error("The class '" + b._className + " could not be found or does not implement the wanda.web.BeaconBit interface.\n", T);
                      OK = false;
                    }
                }

              if (b._timing == null)
                {
                  WebBasics.LOG.error("The beacon definition '" + b._className + "' doesn't defines a timing (should be either DAY, HOUR or MINUTE).");
                  OK = false;
                }
            }

        return OK;
      }
  }
