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
import tilda.utils.TextUtil;
import tilda.utils.json.JSONUtil;
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
import wanda.web.config.WebBasicsDefApps;

public class LoadAppsConfig
  {
    static final Logger LOG = LogManager.getLogger(LoadAppsConfig.class.getName());

    public static void main(String[] Args)
      {
        LOG.info("");
        LOG.info("Wanda App Definition Configuration Loader");
        LOG.info("  - This utility will load /WebBasics.apps.json and its /WebBasics.app.Xyz.json ");
        LOG.info("   dependencies in the classpath.");
        LOG.info("  - The information will be loaded into the " + App_Factory.SCHEMA_TABLENAME_LABEL + " and ");
        LOG.info("   " + Config_Factory.SCHEMA_TABLENAME_LABEL + " tables");
        LOG.info("");
        Connection C = null;

        try
          {
            Iterator<String> connectionIds = ConnectionPool.getAllDataSourceIds().keySet().iterator();
            while (connectionIds.hasNext())
              {
                String connectionId = connectionIds.next();
                LOG.info("\n\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n"
                + "!!! Loading Apps For Tenant/Database\n"
                + "!!!     ==> " + connectionId + ": " + ConnectionPool.getDBDetails(connectionId) + "\n"
                + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
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
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            InputStream In = FileUtil.getResourceAsStream("WebBasics.apps.json");
            if (In == null)
              throw new Exception("Cannot find 'WebBasics.apps.json' initialization file in classpath");
            R = new BufferedReader(new InputStreamReader(In));
            WebBasicsDefApps Apps = gson.fromJson(R, WebBasicsDefApps.class);

            if (Apps._apps != null)
              for (AppDef ad : Apps._apps)
                if (ad != null)
                  {
                    String srcFile = "WebBasics.app." + ad._id + ".json";
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
              throw new Exception("The WebBasics apps file is invalid.");

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

    public static void process(Connection C, WebBasicsDefApps DA)
    throws Exception
      {
        ZonedDateTime ZDT = C.getCurrentTimestamp();

        int i = -1;
        for (AppDef ad : DA._apps)
          if (ad != null)
            {
              App_Data A = App_Factory.lookupByPathHome(ad._path, ad._AppDefDetail._home); // search by path and home
              A.setLabel(ad._AppDefDetail._label);
              A.setSeq(++i);
              A.setId(ad._id);
              A.setAdmin(ad._AppDefDetail._admin);
              A.setServices(printRawAppDefDetailServicesArray(ad._AppDefDetail._services));
              A.setId(ad._id);
              A.setNullDeleted();
              if (A.write(C) == false) // not existing
                {
                  A = App_Factory.lookupByLabel(ad._AppDefDetail._label); // search by label
                  A.setPath(ad._path);
                  A.setHome(ad._AppDefDetail._home);
                  A.setAdmin(ad._AppDefDetail._admin);
                  A.setSeq(i);
                  A.setId(ad._id);
                  A.setServices(printRawAppDefDetailServicesArray(ad._AppDefDetail._services));
                  A.setId(ad._id);
                  A.setNullDeleted();
                  if (A.write(C) == false)
                    {
                      A = App_Factory.create(ad._path, ad._AppDefDetail._home, ad._AppDefDetail._label, i);
                      A.setId(ad._id);
                      A.setAdmin(ad._AppDefDetail._admin);
                      A.setServices(printRawAppDefDetailServicesArray(ad._AppDefDetail._services));
                      if (A.write(C) == false)
                        throw new Exception("Cannot insert/update App record");
                    }
                }
              A.refresh(C);
              AppUser_Data AU = AppUser_Factory.lookupByUnassignedApp(A.getRefnum());
              if (AU.read(C) == false)
                {
                  AU = AppUser_Factory.create(A.getRefnum());
                  if (AU.write(C) == false)
                    throw new Exception("Cannot insert default app record for app " + A.getRefnum());
                }
              // Create Administrator role for the application
              if (TextUtil.isNullOrEmpty(A.getAdmin()) == false)
                {
                  Role_Data R = Role_Factory.create("Admin" + A.getId(), "Admin" + A.getId(), "Administrator for application " + A.getLabel());
                  if (R.write(C) == false)
                    {
                      R = Role_Factory.lookupById("Admin" + A.getId());
                      R.setLabel("Administrator for application " + A.getLabel()); // In case the label changed for the app
                      R.write(C);
                    }
                }
            }

        // App not updated in this round, i.e., lastUpdated < ZDT, have likely been removed, so they should be marked as deleted.
        List<App_Data> L = App_Factory.lookupWhereLastUpdated(C, ZDT, 0, -1);
        for (App_Data A : L)
          {
            A.setDeletedNow();
            if (A.write(C) == false)
              throw new Exception("Cannot cleanup old apps");
          }

        Config_Data Conf = Config_Factory.create("MAIN");
        Conf.setAuthPassthroughs(DA._authPassthroughs);
        Conf.setMasterPaths(DA._masterPaths);
        if (Conf.upsert(C, true) == false)
          throw new Exception("Cannot insert/update Config record");
      }
  }
