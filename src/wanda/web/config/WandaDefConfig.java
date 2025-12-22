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
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

import tilda.db.Connection;
import tilda.utils.TextUtil;

public class WandaDefConfig
  {
    /*@formatter:off*/
    @SerializedName("email"           )         EmailConfigDetails                  _emailSettingsUsr       = null;
    @SerializedName("emailSys"        )         EmailConfigDetails                  _emailSettingsSys       = null;
    @SerializedName("sessionConfig"   )         SessionConfig                       _sessionConfig          = null;
    @SerializedName("passwordRules"   )         List<PasswordRule>                  _passwordRules          = null;

    @SerializedName("hostName"        )         String                              _hostName               = null;
    @SerializedName("appName"         )         String                              _appName                = null;
    @SerializedName("appUUID"         )         String                              _appUUID                = null;
    @SerializedName("appPath"         )         String                              _appPath                = null;
    @SerializedName("homePagePath"    )         String                              _homePagePath           = null;
    @SerializedName("resetEmailText"  )         List<String>                        _resetEmailTexts        = null;
    @SerializedName("inviteEmailText" )         List<String>                        _inviteEmailTexts       = null;
    @SerializedName("guestRegistration")        GuestRegistration                   _guestRegistration      = null;
    @SerializedName("emailVerificationText")    List<String>                        _emailVerificationTexts = null;

    @SerializedName("lookAndFeel"     )         LookAndFeel                         _laf                    = null;
    @SerializedName("loginSystem"     )         LoginSystem                         _loginSystem            = null;
    @SerializedName("ticketSystem"    )         TicketSystem                        _ticketSystem           = new TicketSystem();
    @SerializedName("paymentSystems"  )         List<PaymentSystem>                 _paymentSystems         = new ArrayList<PaymentSystem>();
    @SerializedName("eulas"           )         List<Eula>                          _eulas                  = new ArrayList<Eula>();
    @SerializedName("beacon"          )         BeaconConfig                        _beacon                 = null;
    @SerializedName("extras"          )         Map<String, Map<String, String>>    _extras                 = null;
    /*@formatter:on*/

    public boolean validate(Connection C)
    throws Exception
      {
        boolean OK = true;

        if (_appName == null)
          {
            Wanda.LOG.error("The Wanda configuration file didn't define any 'appName' property");
            OK = false;
          }
        if (_appUUID == null)
          {
            Wanda.LOG.error("The Wanda configuration file didn't define any 'appUUID' property. ");
            OK = false;
          }
        if (_hostName == null)
          {
            Wanda.LOG.error("The Wanda configuration file didn't define any 'hostName' property");
            OK = false;
          }
        if (_appPath == null)
          {
            Wanda.LOG.error("The Wanda configuration file didn't define any 'appPath' property");
            OK = false;
          }
        if (_homePagePath == null)
          {
            Wanda.LOG.error("The Wanda configuration file didn't define any 'homePagePath' property");
            OK = false;
          }

        if (_sessionConfig == null)
          {
            Wanda.LOG.error("The Wanda configuration file didn't define any 'sessionConfig' property");
            OK = false;
          }
        else
          OK = _sessionConfig.validate(OK);

        if (_passwordRules == null || _passwordRules.size() == 0)
          {
            Wanda.LOG.error("No Password rules in the wanda.config.json");
            OK = false;
          }

        if (_emailSettingsUsr != null)
          {
            if (TextUtil.isNullOrEmpty(_emailSettingsUsr._smtp) == true)
              {
                Wanda.LOG.error("Invalid User Email Configuration in the Wanda configuration file: the 'smtp' attribute is missing or empty.");
                OK = false;
              }
            if (TextUtil.isNullOrEmpty(_emailSettingsUsr._userId) == true)
              {
                Wanda.LOG.error("Invalid User Email Configuration in the Wanda configuration file: the 'userId' attribute is missing or empty.");
                OK = false;
              }
            if (TextUtil.isNullOrEmpty(_emailSettingsUsr._pswd) == true)
              {
                Wanda.LOG.warn("The User Email Configuration in the Wanda configuration file has a missing or empty 'pswd' attribute, which may be OK.");
              }
          }
        else
          Wanda.LOG.warn("The User Email Configuration in the Wanda configuration file is missing or not set. No end-user email services will be available, which will affect registration invitations, password reset capabilities and other features..");

        if (_emailSettingsSys != null)
          {
            if (TextUtil.isNullOrEmpty(_emailSettingsSys._smtp) == true)
              {
                Wanda.LOG.error("Invalid System Email Configuration in the Wanda configuration file: the 'smtp' attribute is missing or empty.");
                OK = false;
              }
            if (TextUtil.isNullOrEmpty(_emailSettingsSys._userId) == true)
              {
                Wanda.LOG.error("Invalid System Email Configuration in the Wanda configuration file: the 'userId' attribute is missing or empty.");
                OK = false;
              }
            if (TextUtil.isNullOrEmpty(_emailSettingsSys._pswd) == true)
              {
                Wanda.LOG.warn("The System Email Configuration in the Wanda configuration file has a missing or empty 'pswd' attribute, which may be OK.");
              }
            if (TextUtil.isNullOrEmpty(_emailSettingsSys._defaultAdmins) == true)
              {
                Wanda.LOG.error("Invalid System Email Configuration in the Wanda configuration file: the 'defaultAdmins' attribute is missing or empty.");
                OK = false;
              }
          }
        else
          {
            Wanda.LOG.error("The System Email Configuration 'emailSys' in the Wanda configuration file is missing or not set. This must be set.");
            OK = false;
          }

        if (_laf == null)
          {
            Wanda.LOG.error("No 'lookAndFeel' Configuration was defined in the Wanda configuration file");
            OK = false;
          }
        else
          {
            if (TextUtil.isNullOrEmpty(_laf._logoBig) == true)
              {
                Wanda.LOG.error("The property 'lookAndFeel.logoBig' in the Wanda configuration file is missing");
                OK = false;
              }
            if (TextUtil.isNullOrEmpty(_laf._logoSmall) == true)
              _laf._logoSmall = "";
            if (TextUtil.isNullOrEmpty(_laf._pageTitle) == true)
              _laf._pageTitle = "";

            if (_laf._urlRedirectPostLogin == null)
              _laf._urlRedirectPostLogin = _appPath + _homePagePath;
          }

        OK = Eula.validate(_eulas, OK);
        OK = PaymentSystem.validate(_paymentSystems, OK);
        OK = BeaconConfig.validate(_beacon, OK);
        OK = GuestRegistration.validate(C, _guestRegistration, OK);

        _ticketSystem.validate(C);
        _ticketSystem.launch();

        if (_loginSystem == null)
          {
            Wanda.LOG.error("No 'loginSystem' Configuration was defined in the Wanda configuration file");
            OK = false;
          }
        else if (_loginSystem.validate() == false)
          OK = false;

        return OK;
      }

  }
