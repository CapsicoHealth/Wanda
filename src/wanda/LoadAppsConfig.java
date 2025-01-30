/* ===========================================================================
 * Copyright (C) 2019 CapsicoHealth Inc.
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

package wanda;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import tilda.db.Connection;
import tilda.db.ConnectionPool;
import tilda.utils.AsciiArt;
import tilda.utils.FileUtil;
import tilda.utils.LogUtil;
import tilda.utils.TextUtil;
import tilda.utils.json.JSONUtil;
import wanda.data.AppConfig_Data;
import wanda.data.AppConfig_Factory;
import wanda.data.AppUser_Data;
import wanda.data.AppUser_Factory;
import wanda.data.App_Data;
import wanda.data.App_Factory;
import wanda.data.Config_Data;
import wanda.data.Config_Factory;
import wanda.data.Role_Data;
import wanda.data.Role_Factory;
import wanda.web.config.AppDef;
import wanda.web.config.AppDefDetails;
import wanda.web.config.AppDefService;
import wanda.web.config.Wanda;
import wanda.web.config.WandaDefApps;

public class LoadAppsConfig
  {
    static final Logger   LOG               = LogManager.getLogger(LoadAppsConfig.class.getName());

    static public boolean _COMMAND_LINE_RUN = true;

    public static void main(String[] Args)
      {
        if (_COMMAND_LINE_RUN == true)
          {
            LOG.info("");
            LOG.info("Wanda App Definition Configuration Loader");
            LOG.info("  - This utility will load /wanda.apps.json and its /wanda.app.Xyz.json ");
            LOG.info("   dependencies in the classpath.");
            LOG.info("  - The information will be loaded into the " + App_Factory.SCHEMA_TABLENAME_LABEL + " and ");
            LOG.info("   " + Config_Factory.SCHEMA_TABLENAME_LABEL + " tables");
            LOG.info("");
          }
        Connection C = null;

        try
          {
            Iterator<String> connectionIds = ConnectionPool.getAllDataSourceIds().keySet().iterator();
            while (connectionIds.hasNext())
              {
                String connectionId = connectionIds.next();
                LOG.info("\n\n"
                + "--------------------------------------------------------------------------------------------------------------------------------------\n"
                + "--- Loading Apps For Tenant/Database " + connectionId + ": " + ConnectionPool.getDBDetails(connectionId) + "\n");
                C = ConnectionPool.get(connectionId);
                load(C);
                C.commit();
                C = null;
              }
          }
        catch (Exception E)
          {
            LOG.error("An exception occurred\n", E);
            LOG.error("\n"
            + "          ======================================================================================\n"
            + AsciiArt.Error("               ")
            + "\n"
            + "                      Cannot load Wanda application configuration data to the database.\n"
            + "          ======================================================================================\n", E);
            if (_COMMAND_LINE_RUN == false)
              return;
            System.exit(-1);
          }
        finally
          {
            if (C != null)
              try
                {
                  C.rollback();
                  C.close();
                }
              catch (SQLException E)
                {
                }
          }

        LOG.info("\n"
        + "          ======================================================================================\n"
        + AsciiArt.Woohoo("                       ")
        + "\n"
        + "                  The Wanda application configuration data was loaded to the database.\n"
        + "          ======================================================================================");
      }

    private static void load(Connection C)
    throws Exception
      {
        Reader R = null;
        try
          {
            LOG.info("Loading wanda.apps.json...");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            InputStream In = FileUtil.getResourceAsStream("wanda.apps.json");
            if (In == null)
              throw new Exception("Cannot find 'wanda.apps.json' initialization file in classpath");
            R = new BufferedReader(new InputStreamReader(In));
            WandaDefApps Apps = gson.fromJson(R, WandaDefApps.class);

            if (Apps._apps != null)
              for (AppDef ad : Apps._apps)
                if (ad != null)
                  {
                    String srcFile = "wanda.app." + ad._id + ".json";
                    In = FileUtil.getResourceAsStream(srcFile);
                    if (In == null)
                      throw new Exception("The file " + srcFile + " cannot be found.");
                    R = new BufferedReader(new InputStreamReader(In));
                    ad._AppDefDetail = gson.fromJson(R, AppDefDetails.class);
                    if (ad._AppDefDetail.validate(srcFile) == false)
                      throw new Exception("The file " + srcFile + " is invalid.");
                    if (TextUtil.isNullOrEmpty(ad._label) == false)
                      ad._AppDefDetail._label = ad._label;
                  }

            if (Apps.validate() == false)
              throw new Exception("The Wanda apps file is invalid.");

            process(C, Apps);
          }
        finally
          {
            if (R != null)
              R.close();
          }
      }

    public static String printRawAppDefDetailServicesArray(List<AppDefService> L)
    throws Exception
      {
        StringBuilderWriter SBW = new StringBuilderWriter();
        JSONUtil.print(SBW, "", L, "", null);
        return SBW.getBuilder().toString();
      }

    public static void process(Connection C, WandaDefApps DA)
    throws Exception
      {
        ZonedDateTime ZDT = C.getCurrentTimestamp();

        int i = -1;
        LOG.debug("Updating App configurations...");
        // LogUtil.setLogLevel(Level.ERROR);
        for (AppDef ad : DA._apps)
          if (ad != null)
            {
              App_Data A = App_Factory.lookupByPathHome(ad._path, ad._AppDefDetail._home); // search by path and home
              A.setId(ad._id);
              A.setAdmin(ad._AppDefDetail._admin);
              A.setServices(printRawAppDefDetailServicesArray(ad._AppDefDetail._services));
              A.setNullDeleted();
              if (A.write(C) == false) // not existing
                {
                  A = App_Factory.lookupById(ad._id); // search by id
                  A.setPath(ad._path);
                  A.setHome(ad._AppDefDetail._home);
                  A.setAdmin(ad._AppDefDetail._admin);
                  A.setServices(printRawAppDefDetailServicesArray(ad._AppDefDetail._services));
                  A.setNullDeleted();
                  if (A.write(C) == false) // not existing, need to create new
                    {
                      A = App_Factory.create(ad._id, ad._path, ad._AppDefDetail._home);
                      A.setAdmin(ad._AppDefDetail._admin);
                      A.setServices(printRawAppDefDetailServicesArray(ad._AppDefDetail._services));
                      if (A.write(C) == false)
                        {
                          LogUtil.resetLogLevel();
                          throw new Exception("Cannot insert/update App record");
                        }
                    }
                }
              A.refresh(C);

              ++i; // sequence increment.
              // Looking up the app config for this host.
              AppConfig_Data AC = AppConfig_Factory.lookupByAppHost(A.getRefnum(), Wanda.getHostName());
              if (AC.read(C) == false) // cannot find.
                {
                  // Need migration support: Looking up the app config for the default "" hostName value.
                  AC = AppConfig_Factory.lookupByAppHost(A.getRefnum(), "");
                  if (AC.read(C) == false)
                    AC = AppConfig_Factory.create(A.getRefnum(), Wanda.getHostName(), ad._AppDefDetail._label, i);
                }
              AC.setHostName(Wanda.getHostName());
              AC.setLabel(ad._AppDefDetail._label);
              AC.setSeq(i);
              AC.setNullDeleted();
              if (AC.write(C) == false)
                {
                  LogUtil.resetLogLevel();
                  throw new Exception("Cannot insert/update AppConfig record");
                }

              AppUser_Data AU = AppUser_Factory.lookupByUnassignedApp(A.getRefnum());
              if (AU.read(C) == false)
                {
                  AU = AppUser_Factory.create(A.getRefnum());
                  if (AU.write(C) == false)
                    {
                      LogUtil.resetLogLevel();
                      throw new Exception("Cannot insert default app record for app " + A.getRefnum());
                    }
                }
              // Create Administrator role for the application
              if (TextUtil.isNullOrEmpty(A.getAdmin()) == false)
                {
                  Role_Data R = Role_Factory.create("Admin" + A.getId(), "Admin" + A.getId(), "Administrator for application " + AC.getLabel());
                  if (R.write(C) == false)
                    {
                      R = Role_Factory.lookupById("Admin" + A.getId());
                      R.setLabel("Administrator for application " + AC.getLabel()); // In case the label changed for the app
                      R.write(C);
                    }
                }
            }
        LogUtil.resetLogLevel();
        LOG.debug("   --> Updated " + i + " App configurations.");

        // App not updated in this round, i.e., lastUpdated < ZDT, have likely been removed, so they should be marked as deleted.
        List<AppConfig_Data> L = AppConfig_Factory.lookupWhereLastUpdated(C, Wanda.getHostName(), ZDT, 0, -1);
        for (AppConfig_Data A : L)
          {
            A.setDeletedNow();
            if (A.write(C) == false)
              throw new Exception("Cannot cleanup old apps");
          }

        Config_Data Conf = Config_Factory.create("MAIN");
        Conf.setAuthPassthroughs(DA._authPassthroughs);
        Conf.setMasterPaths(DA._masterPaths);
        if (Conf.upsert(C) == false)
          throw new Exception("Cannot insert/update Config record");
      }
  }
