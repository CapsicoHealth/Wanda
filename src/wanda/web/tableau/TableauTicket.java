/* ===========================================================================
 * Copyright (C) 2020 CapsicoHealth Inc.
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

package wanda.web.tableau;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.utils.FileUtil;
import tilda.utils.TextUtil;
import wanda.web.config.Wanda;

public class TableauTicket
  {
    static final Logger LOG = LogManager.getLogger(TableauTicket.class.getName());


    final static public String getTrustedUrl(String subConfigName)
    throws Exception
      {
        String url = Wanda.getExtra("tableau-" + subConfigName, "url");
        String user = Wanda.getExtra("tableau-" + subConfigName, "user");
        String targetSite = Wanda.getExtra("tableau-" + subConfigName, "targetSite");

        return get(url, user, targetSite);
      }

    final static public String getSite(String subConfigName)
    throws Exception
      {
        return Wanda.getExtra("tableau-" + subConfigName, "site");
      }

    final static protected String get(String url, String userName, String targetSite)
      {
        try
          {
            LOG.debug("Getting Tableau token for URL '" + url + "', user '" + userName + "' and site '"+TextUtil.print(targetSite, "default")+"'.");
            Map<String, String> params = new HashMap<>();
            params.put("username", userName);
            if (TextUtil.isNullOrEmpty(targetSite) == false)
              params.put("target_site", targetSite);
            String response = FileUtil.getContentsFromPostUrl(url + "/trusted", params);
            LOG.debug("    --> Tableau token: '" + response + "'.");
            if (response != null)
              return url + "/trusted/" + response;
          }
        catch (Exception E)
          {
            LOG.error(E);
          }

        LOG.error("An exception occurred getting a Tableau token for URL '" + url + "' and user '" + userName + "'");
        return url;
      }

    public static void main(String[] args)
    throws Exception
      {
        LOG.debug("Getting a tableau token");
        LOG.debug(getTrustedUrl("main"));
      }

  }
