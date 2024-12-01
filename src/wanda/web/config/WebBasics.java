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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import tilda.db.Connection;
import tilda.db.ConnectionPool;
import tilda.utils.DateTimeUtil;
import tilda.utils.FileUtil;
import tilda.utils.TextUtil;
import wanda.data.AppView_Data;
import wanda.data.AppView_Factory;
import wanda.data.Config_Data;
import wanda.data.Config_Factory;


public class WebBasics
  {
    static final Logger               LOG         = LogManager.getLogger(WebBasics.class.getName());

    private static WebBasicsDefConfig _Config;
    private static List<AppView_Data> _Apps       = new ArrayList<AppView_Data>();
    private static Config_Data        _AppsConfig = null;

    private WebBasics()
      {

      }

    public static void autoInit()
      {
      }

    static
      {
        try
          {
            readConfig();
          }
        catch (Throwable T)
          {
            LOG.error("An exception occurred while configuring the WebBasics library.\n", T);
            if (T.getCause() != null)
              {
                T = T.getCause();
                LOG.error("--------------------------------------------------------\n", T);
                if (T.getCause() != null)
                  {
                    T = T.getCause();
                    LOG.error("--------------------------------------------------------\n", T);
                  }
              }
            System.exit(-1);
          }
      }


    private static void readConfig()
    throws Exception
      {
        Reader R = null;
        Connection C = null;
        try
          {
            LOG.info("Loading '/WebBasics.config.json' from the classpath.");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            InputStream In = FileUtil.getResourceAsStream("WebBasics.config.json");
            if (In == null)
              throw new Exception("Cannot find the WebBasics configuration file '/WebBasics.config.json' in the classpath.");

            URL url = FileUtil.getResourceUrl("WebBasics.config.json");
            LOG.info("   Found WebBasics.config.json file in " + url.toString());

            C = ConnectionPool.get("MAIN");

            R = new BufferedReader(new InputStreamReader(In));
            _Config = gson.fromJson(R, WebBasicsDefConfig.class);
            if (_Config.validate(C) == false)
              throw new Exception("Invalid WebBasics configuration file '" + url.toString() + "'.");

            _Apps = AppView_Factory.lookupWhereAll(C, WebBasics.getHostName(), 0, -1);
            _AppsConfig = Config_Factory.lookupById("MAIN");
            if (_AppsConfig.read(C) == false)
              {
                LOG.warn("The WebBasics app configuration is empty. This may be normal if this is the first time the server is started.");
                // throw new Exception("The WebBasics app configuration is empty. Make sure to run the utility LoadAppsConfig before launching the server.");
              }
            StringBuilder Str = new StringBuilder();
            Str.append("\n   ************************************************************************************************************************\n");
            Str.append("   ** Wanda Configuration\n");
            Str.append("   **\n");
            Str.append("   ** AuthPassthroughs: " + TextUtil.print(_AppsConfig.getAuthPassthroughs()) + "\n");
            Str.append("   ** MasterPaths     : " + TextUtil.print(_AppsConfig.getMasterPaths()) + "\n");
            Str.append("   ** Apps            :\n");
            for (AppView_Data A : _Apps)
              Str.append("   **      " + A.getAppLabel() + " (" + A.getAppHome() + ")" + (A.getAppActive() == false ? "  --INACTIVE--" : "") + "\n");
            Str.append("   ************************************************************************************************************************\n");
            LOG.info(Str.toString());
          }
        finally
          {
            if (R != null)
              R.close();
            if (C != null)
              {
                C.rollback();
                C.close();
              }
          }
      }

    // Getters
    public static EmailConfigDetails getEmailSettingsUsr()
      {
        return _Config._emailSettingsUsr;
      }

    public static EmailConfigDetails getEmailSettingsSys()
      {
        return _Config._emailSettingsSys;
      }

    public static BeaconConfig getBeaconConfig()
      {
        return _Config._beacon;
      }

    public static int getJobCheckIntervalSeconds()
      {
        return _Config._jobCheckIntervalSeconds;
      }

    public static int getLoginAttempts()
      {
        return _Config._sessionConfig._loginOrResetAttempts;
      }

    public static int getWithinTime()
      {
        return _Config._sessionConfig._withinMins;
      }

    public static int getLockFor()
      {
        return _Config._sessionConfig._lockForMins;
      }

    public static int getLoginFailedCycle()
      {
        return _Config._sessionConfig._failedLoginCycle;
      }

    public static int getLockForever()
      {
        return _Config._sessionConfig._lockForeverDays;
      }

    public static String getHostName()
      {
        return _Config._hostName;
      }

    public static String getAppName()
      {
        return _Config._appName;
      }

    public static String getAppUUID()
      {
        return _Config._appUUID;
      }

    public static String getAppPath()
      {
        return _Config._appPath;
      }

    public static String getHomePagePath()
      {
        return _Config._homePagePath;
      }

    public static List<String> getResetEmailTexts()
      {
        return _Config._resetEmailTexts;
      }

    public static List<PasswordRule> getPasswordRules()
      {
        return _Config._passwordRules;
      }

    public static List<String> validatePassword(String password)
      {
        List<String> failedPatterns = new ArrayList<>();
        for (PasswordRule pRule : _Config._passwordRules)
          {
            if (!Pattern.matches(pRule._rule, password))
              {
                failedPatterns.add(pRule._description);
              }
          }
        return failedPatterns;
      }

    public static List<String> getInviteUserTexts()
      {
        return _Config._inviteEmailTexts;
      }

    public static boolean getGuestRegistrationAllowed()
      {
        return _Config._guestRegistration == null ? false : _Config._guestRegistration._allowed;
      }

    public static long[] getGuestRegistrationAppRefnums()
      {
        return _Config._guestRegistration == null ? null : _Config._guestRegistration._appRefnums;
      }

    public static long[] getGuestRegistrationTenantRefnums()
      {
        return _Config._guestRegistration == null ? null : _Config._guestRegistration._tenantRefnums;
      }

    public static List<String> getEmailVerificationTexts()
      {
        return _Config._emailVerificationTexts;
      }

    public static int getResetCodeTTL()
      {
        return _Config._sessionConfig._resetCodeExpiryMins;
      }

    public static int getPasswordExpiry()
      {
        return _Config._sessionConfig._passwordExpiryDays;
      }

    public static int getMaxPswdHistory()
      {
        return _Config._sessionConfig._maxPswdHistory;
      }

    public static int getForceReLoginMins()
      {
        return _Config._sessionConfig._forceReLoginMins;
      }


    public static String getUrlRedirectPostLogin()
      {
        return _Config._laf._urlRedirectPostLogin;
      }

    public static String getPageTitle(boolean LoggedIn)
      {
        return LoggedIn == true && TextUtil.isNullOrEmpty(_Config._laf._pageTitlePostLogin) == false ? _Config._laf._pageTitlePostLogin : _Config._laf._pageTitle;
      }

    public static String getLogoBig(boolean LoggedIn)
      {
        return LoggedIn == true && TextUtil.isNullOrEmpty(_Config._laf._logoBigPostLogin) == false ? _Config._laf._logoBigPostLogin : _Config._laf._logoBig;
      }

    public static String getOverlayText(boolean LoggedIn)
      {
        return LoggedIn == true && TextUtil.isNullOrEmpty(_Config._laf._overlayTextPostLogin) == false ? _Config._laf._overlayTextPostLogin : _Config._laf._overlayText;
      }

    public static String getLogoSmall(boolean LoggedIn)
      {
        return LoggedIn == true && TextUtil.isNullOrEmpty(_Config._laf._logoSmallPostLogin) == false ? _Config._laf._logoSmallPostLogin : _Config._laf._logoSmall;
      }

    public static String getPoweredBy()
      {
        return _Config._laf._poweredBy;
      }

    public static String getCopyright()
      {
        return _Config._laf._copyright.replace("%%CURRENT_YEAR%%", "" + DateTimeUtil.nowUTC().getYear());
      }

    public static List<AppView_Data> getApps()
      {
        return _Apps;
      }

    public static Iterator<String> getAuthPassthroughs()
      {
        return _AppsConfig.getAuthPassthroughs();
      }

    public static Iterator<String> getMasterPaths()
      {
        return _AppsConfig.getMasterPaths();
      }

    public static Iterator<String> getGuestPaths()
      {
        return _AppsConfig.getGuestPaths();
      }


    public static String getOverrideCssFile()
      {
        return _Config._laf._overrideCssFile;
      }

    public static String getTwofishesUrl()
      {
        return _Config._twofishesUrl;
      }

    public static Eula getEula(String TenantName)
      {
        if (TenantName == null)
          TenantName = "";
        Eula e = null;
        for (Eula E : _Config._eulas)
          if (E != null)
            {
              if (E._tenantName.equals(TenantName) == true)
                return E;
              if (E._tenantName.equals("") == true)
                e = E;
            }
        return e;
      }

    public static LoginSystem getLoginSystem()
      {
        return _Config._loginSystem;
      }

    public static Map<String, String> getExtra(String configName)
      {
        return _Config._extras == null ? null : _Config._extras.get(configName);
      }

    public static String getExtra(String configName, String elementName)
      {
        if (_Config._extras == null)
          return null;

        Map<String, String> config = _Config._extras.get(configName);
        return config == null ? null : config.get(elementName);
      }

  }
