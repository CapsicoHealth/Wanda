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

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.annotations.SerializedName;

import tilda.utils.EncryptionUtil;
import tilda.utils.TextUtil;
import wanda.web.LoginSyncService;

public class LoginSystem
  {
    /*@formatter:off*/
    @SerializedName("ssoConfigs"               ) public List<SSOConfig> _ssoConfigs  = new ArrayList<SSOConfig>();
    @SerializedName("apiKeys"                  ) public List<ApiKey>    _apiKeys  = new ArrayList<ApiKey>();
    @SerializedName("userSyncServices"         ) public String[]        _userSyncServices   = null  ;
    @SerializedName("contentDefinitionServices") public String[]        _contentDefinitionServices   = null  ;
    @SerializedName("notifications"            ) public Notification    _notifications      = null  ;
    /*@formatter:on*/

    protected transient List<LoginSyncService> _userSyncServiceClasses    = new ArrayList<LoginSyncService>();

    public boolean validate()
      {
        boolean OK = true;
        if (_ssoConfigs != null)
          OK = validateSSOConfigs(OK);

        if (_apiKeys != null)
          OK = validateApiKeys(OK);

        if (_userSyncServices != null)
          OK = validateUserSyncServices(OK);

        return OK;
      }

    private boolean validateApiKeys(boolean OK)
      {
        Set<String> ids = new HashSet<String>();
        Set<String> keys = new HashSet<String>();
        for (ApiKey apiKey : _apiKeys)
          if (apiKey != null)
            {
              if (TextUtil.isNullOrEmpty(apiKey._id) == true)
                {
                  Wanda.LOG.error("The loginSystem entry has an apiKey without an id value.");
                  OK = false;
                }
              else if (ids.add(apiKey._id) == false)
                {
                  Wanda.LOG.error("The loginSystem entry defines an apiKey '" + apiKey._id + "' multiple times.");
                  OK = false;
                }

              if (TextUtil.isNullOrEmpty(apiKey._key) == true)
                {
                  Wanda.LOG.error("The loginSystem entry has an apiKey '" + apiKey._id + "' without a key value.");
                  OK = false;
                }
              else if (keys.add(apiKey._key) == false)
                {
                  Wanda.LOG.error("The loginSystem entry defines an apiKey '" + apiKey._id + "' with the key '" + apiKey._key + "' which is being used multiple times.");
                  OK = false;
                }

              if (apiKey._sourceIps == null || apiKey._sourceIps.length == 0)
                {
                  Wanda.LOG.error("The loginSystem entry has an apiKey '" + apiKey._id + "' without a sourceIps value.");
                  OK = false;
                }
              else
                {
                  for (String ip : apiKey._sourceIps)
                    {
                      if (TextUtil.isNullOrEmpty(ip) == true)
                        {
                          Wanda.LOG.error("The loginSystem entry has an apiKey '" + apiKey._id + "' with a sourceIps value which is empty.");
                          OK = false;
                        }
                      if (ip.equals("*") == true && apiKey._sourceIps.length > 1)
                        {
                          Wanda.LOG.error("The loginSystem entry has an apiKey '" + apiKey._id + "' with a sourceIps value which is '*' and another value.");
                          OK = false;
                        }
                      int pos = ip.indexOf('*');
                      if (pos >= 0 && pos != ip.length() - 1)
                        {
                          Wanda.LOG.error("The loginSystem entry has an apiKey '" + apiKey._id + "' with a sourceIps value which contains a '*' not at the end.");
                          OK = false;
                        }
                    }
                }
            }
        return OK;
      }

    protected boolean validateUserSyncServices(boolean OK)
      {
        Set<String> names = new HashSet<String>();
        for (String uss : _userSyncServices)
          if (TextUtil.isNullOrEmpty(uss) == false)
            try
              {
                if (names.add(uss) == false)
                  {
                    Wanda.LOG.error("The loginSystem entry defines a userSyncService class '" + uss + "' multiple times.");
                    OK = false;
                  }
                else
                  {
                    Class<LoginSyncService> c = (Class<LoginSyncService>) Class.forName(uss);
                    LoginSyncService LSS = c.getConstructor().newInstance();
                    _userSyncServiceClasses.add(LSS);
                  }
              }
            catch (Throwable T)
              {
                Wanda.LOG.error("The loginSystem entry defines a userSyncService class '" + uss + "' which cannot be resolved to a class.");
                OK = false;
              }
        return OK;
      }

    protected boolean validateSSOConfigs(boolean OK)
      {
        Set<String> ids = new HashSet<String>();
        Set<String> files = new HashSet<String>();
        for (SSOConfig conf : _ssoConfigs)
          if (conf != null)
            {
              if (TextUtil.isNullOrEmpty(conf._id) == true)
                {
                  Wanda.LOG.error("The loginSystem entry has an ssoConfig without an id value.");
                  OK = false;
                }
              else if (ids.add(conf._id) == false)
                {
                  Wanda.LOG.error("The loginSystem entry defines an ssoConfig '" + conf._id + "' multiple times.");
                  OK = false;
                }

              // saml IP config file
              if (TextUtil.isNullOrEmpty(conf._identityProviderConfigFile) == true)
                {
                  Wanda.LOG.error("The loginSystem entry has an ssoConfig '" + conf._id + "' without an identityProviderConfigFile value.");
                  OK = false;
                }
              else if (files.add(conf._identityProviderConfigFile) == false)
                {
                  Wanda.LOG.error("The loginSystem entry defines an ssoConfig '" + conf._id + "' with identityProviderConfigFile '" + conf._identityProviderConfigFile + "' which is being used multiple times.");
                  OK = false;
                }
              else
                {
                  File f = new File(conf._identityProviderConfigFile);
                  if (f.exists() == false)
                    {
                      Wanda.LOG.error("The loginSystem entry has an ssoConfig '" + conf._id + "' with identityProviderConfigFile '" + conf._identityProviderConfigFile + "' which cannot be found.");
                      OK = false;
                    }
                }

              // saml SP config file
              if (TextUtil.isNullOrEmpty(conf._serviceProviderConfigFile) == true)
                {
                  Wanda.LOG.error("The loginSystem entry has an ssoConfig '" + conf._id + "' without a serviceProviderConfigFile value.");
                  OK = false;
                }
              else if (files.add(conf._serviceProviderConfigFile) == false)
                {
                  Wanda.LOG.error("The loginSystem entry defines an ssoConfig '" + conf._id + "' with serviceProviderConfigFile '" + conf._identityProviderConfigFile + "' which is being used multiple times.");
                  OK = false;
                }
              else
                {
                  File f = new File(conf._serviceProviderConfigFile);
                  if (f.exists() == false)
                    {
                      Wanda.LOG.error("The loginSystem entry has an ssoConfig '" + conf._id + "' with serviceProviderConfigFile '" + conf._identityProviderConfigFile + "' which cannot be found.");
                      OK = false;
                    }
                }
              
              if (TextUtil.isNullOrEmpty(conf._identityProviderEntityId) == true)
                {
                  Wanda.LOG.error("The loginSystem entry has an ssoConfig '" + conf._id + "' without an ipEntityId value.");
                  OK = false;
                }

              if (TextUtil.isNullOrEmpty(conf._redirectUrl) == true)
                {
                  Wanda.LOG.error("The loginSystem entry has an ssoConfig '" + conf._id + "' without a redirectUrl value.");
                  OK = false;
                }
              else if (conf._redirectUrl.indexOf("${partnerId}") < 0)
                {
                  Wanda.LOG.error("The loginSystem entry has an ssoConfig '" + conf._id + "' a redirectUrl value that is missing the partner replacement marker '${partnerId}'.");
                  OK = false;
                }
              else
                conf._redirectUrl = conf._redirectUrl.replace("${partnerId}", conf._id);

              // Keystore path
              if (TextUtil.isNullOrEmpty(conf._keyStorePath) == true)
                {
                  Wanda.LOG.error("The loginSystem entry has an ssoConfig '" + conf._id + "' without a keyStorePath value.");
                  OK = false;
                }
              else
                {
                  File f = new File(conf._keyStorePath);
                  if (f.exists() == false)
                    {
                      Wanda.LOG.error("The loginSystem entry has an ssoConfig '" + conf._id + "' with a keystore file '" + conf._keyStorePath + "' which cannot be found.");
                      OK = false;
                    }
                  else if (EncryptionUtil.isKeystorePasswordValid(conf._keyStorePath, conf._keyStorePswd) == false)
                    {
                      Wanda.LOG.error("The loginSystem entry has an ssoConfig '" + conf._id + "' with a keystore file '" + conf._keyStorePath + "' and a password which is invalid.");
                      OK = false;
                    }
                }

              if (TextUtil.isNullOrEmpty(conf._keyStorePswd) == true)
                {
                  Wanda.LOG.error("The loginSystem entry has an ssoConfig '" + conf._id + "' without a keyStorePswd value.");
                  OK = false;
                }
              
              // defaut promo code
              if (TextUtil.isNullOrEmpty(conf._defaultPromoCode) == true)
                {
                  Wanda.LOG.error("The loginSystem entry has an ssoConfig '" + conf._id + "' without a defaultPromoCode value.");
                  OK = false;
                }
            }
        return OK;
      }

    public List<LoginSyncService> getUserSyncServiceClasses()
      {
        return _userSyncServiceClasses;
      }

    public SSOConfig getSsoConfig(String id)
    throws CloneNotSupportedException
      {
        if (_ssoConfigs != null)
          for (SSOConfig conf : _ssoConfigs)
            if (conf != null && conf._id.equals(id) == true)
              return conf.clone();
        return null;
      }

    public String[] checkApiKeyAllowedSourceIps(String id, String key)
      {
        if (_apiKeys != null)
          for (ApiKey apiKey : _apiKeys)
            if (apiKey != null && apiKey._id.equals(id) == true && apiKey._key.equals(key) == true)
              return apiKey._sourceIps;
        return null;
      }
  }
