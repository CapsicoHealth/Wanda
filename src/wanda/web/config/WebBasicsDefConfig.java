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

import tilda.db.Connection;
import tilda.utils.TextUtil;
import wanda.data.App_Data;
import wanda.data.App_Factory;
import wanda.data.Tenant_Data;
import wanda.data.Tenant_Factory;
import wanda.web.BeaconBit;

public class WebBasicsDefConfig
  {
    /*@formatter:off*/
    @SerializedName("email"           )         EmailConfigDetails                  _emailSettingsUsr       = null;
    @SerializedName("emailSys"        )         EmailConfigDetails                  _emailSettingsSys       = null;
    @SerializedName("sessionConfig"   )         SessionConfig                       _sessionConfig          = null;
    @SerializedName("passwordRules"   )         List<PasswordRule>                  _passwordRules          = null;

    @SerializedName("hostName"        )         String                              _hostName               = null;
    @SerializedName("appName"         )         String                              _appName                = null;
    @SerializedName("appPath"         )         String                              _appPath                = null;
    @SerializedName("homePagePath"    )         String                              _homePagePath           = null;
    @SerializedName("resetEmailText"  )         List<String>                        _resetEmailTexts        = null;
    @SerializedName("inviteEmailText" )         List<String>                        _inviteEmailTexts       = null;
    @SerializedName("guestRegistration")        GuestRegistration                   _guestRegistration      = null;
    @SerializedName("emailVerificationText")    List<String>                        _emailVerificationTexts = null;

    @SerializedName("lookAndFeel"     )         LookAndFeel                         _laf                    = null;
    @SerializedName("eulas"           )         List<Eula>                          _eulas                  = new ArrayList<Eula>();
    @SerializedName("twofishes-url"   )         String                              _twofishesUrl           = null;
    @SerializedName("tableauUrl"      )         String                              _tableauUrl             = null;
    @SerializedName("tableauUser"     )         String                              _tableauUser            = null;
    @SerializedName("jobCheckIntervalSeconds")  int                                 _jobCheckIntervalSeconds= 20;
    @SerializedName("beacon"          )         BeaconConfig                        _beacon                 = null;
    @SerializedName("extras"          )         Map<String, Map<String, String>>    _extras                 = null;
    /*@formatter:on*/

    public boolean validate(Connection C)
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

        if (_emailSettingsUsr != null)
          {
            if (TextUtil.isNullOrEmpty(_emailSettingsUsr._smtp) == true)
              {
                WebBasics.LOG.error("Invalid User Email Configuration in the WebBasics configuration file: the 'smtp' attribute is missing or empty.");
                OK = false;
              }
            if (TextUtil.isNullOrEmpty(_emailSettingsUsr._userId) == true)
              {
                WebBasics.LOG.error("Invalid User Email Configuration in the WebBasics configuration file: the 'userId' attribute is missing or empty.");
                OK = false;
              }
            if (TextUtil.isNullOrEmpty(_emailSettingsUsr._pswd) == true)
              {
                WebBasics.LOG.warn("The User Email Configuration in the WebBasics configuration file has a missing or empty 'pswd' attribute, which may be OK.");
              }
          }
        else
          WebBasics.LOG.warn("The User Email Configuration in the WebBasics configuration file is missing or not set. No end-user email services will be available, which will affect registration invitations, password reset capabilities and other features..");
          
        if (_emailSettingsSys != null)
          {
            if (TextUtil.isNullOrEmpty(_emailSettingsSys._smtp) == true)
              {
                WebBasics.LOG.error("Invalid System Email Configuration in the WebBasics configuration file: the 'smtp' attribute is missing or empty.");
                OK = false;
              }
            if (TextUtil.isNullOrEmpty(_emailSettingsSys._userId) == true)
              {
                WebBasics.LOG.error("Invalid System Email Configuration in the WebBasics configuration file: the 'userId' attribute is missing or empty.");
                OK = false;
              }
            if (TextUtil.isNullOrEmpty(_emailSettingsSys._pswd) == true)
              {
                WebBasics.LOG.warn("The System Email Configuration in the WebBasics configuration file has a missing or empty 'pswd' attribute, which may be OK.");
              }
            if (TextUtil.isNullOrEmpty(_emailSettingsSys._defaultAdmins) == true)
              {
                WebBasics.LOG.error("Invalid System Email Configuration in the WebBasics configuration file: the 'defaultAdmins' attribute is missing or empty.");
                OK = false;
              }
          }
        else
          {
            WebBasics.LOG.error("The System Email Configuration 'emailSys' in the WebBasics configuration file is missing or not set. This must be set.");
            OK = false;
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

        if (_guestRegistration != null && _guestRegistration._allowed == true)
          {
            if (_guestRegistration._appIds == null || _guestRegistration._appIds.length == 0)
              {
                WebBasics.LOG.error("The guestRegistration appIds is empty or unspecified. If allowed is true, there must be at least one aplication listed.");
                OK = false;
              }
            else
              {
                List<App_Data> AL = App_Factory.lookupWhereIds(C, _guestRegistration._appIds, 0, -1);
                if (AL.size() != _guestRegistration._appIds.length)
                  {
                    WebBasics.LOG.error("The guestRegistration appIds specifies application Ids which cannot be found in the database.");
                    OK = false;
                  }
                else
                  {
                    _guestRegistration._appRefnums = new long[AL.size()];
                    for (int i = 0; i < AL.size(); ++i)
                      _guestRegistration._appRefnums[i] = AL.get(i).getRefnum();
                  }
              }

            // Gotta check if the system is in multi-tenant mode or not.
            List<Tenant_Data> TL = Tenant_Factory.lookupWhereActive(C, 0, -1);
            if (TL.size() > 0 && (_guestRegistration._tenantIds == null || _guestRegistration._tenantIds.length == 0))
              {
                WebBasics.LOG.error("The guestRegistration tenantIds is empty or unspecified. If allowed is true, there must be at least one tenant listed.");
                OK = false;
              }
            else
              {
                TL = Tenant_Factory.lookupWhereNames(C, _guestRegistration._tenantIds, 0, -1);
                if (TL.size() != (_guestRegistration._tenantIds == null ? 0 : _guestRegistration._tenantIds.length))
                  {
                    WebBasics.LOG.error("The guestRegistration tenantIds specifies tenant ids which cannot be found in the database.");
                    OK = false;
                  }
                else
                  {
                    _guestRegistration._tenantRefnums = new long[TL.size()];
                    for (int i = 0; i < TL.size(); ++i)
                      _guestRegistration._tenantRefnums[i] = TL.get(i).getRefnum();
                  }
              }
          }

        return OK;
      }
  }
