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

package wanda.web.tableau;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.utils.FileUtil;

public class TableauTicket
  {
    static final Logger LOG = LogManager.getLogger(TableauTicket.class.getName());

    final static public String get(String url, String userName)
      {
        try
          {
            Map<String, String> params = new HashMap<>();
            params.put("username", userName);
            // params.put("target_site", "Default");
            String response = FileUtil.getContentsFromPostUrl(url+"/trusted", params);
            if (response != null)
              return url + "/trusted/" + response;
          }
        catch (Exception E)
          {
            LOG.error(E);
          }

        LOG.error("An exception occurred getting a tableau token from: "+url+"/"+userName);
        return url;
      }

    public static void main(String[] args)
      {
        LOG.debug("Getting a tableau token");
        LOG.debug(get("https://xxxx.capsicohealth.com/trusted", "demo@capsicohealth.com"));
      }

  }
