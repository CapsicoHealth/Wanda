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

package wanda.web.looker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.utils.DateTimeUtil;
import tilda.utils.EncryptionUtil;
import tilda.utils.TextUtil;
import wanda.web.config.WebBasics;

/**
 * Inspired by https://github.com/looker/looker_embed_sso_examples/blob/master/LookerEmbedClientExample.java and
 * https://docs.looker.com/reference/embedding/sso-embed
 * @author Laurent Hasson
 *
 */
public class LookerSSO
  {
    static final Logger LOG = LogManager.getLogger(LookerSSO.class.getName());

    final static public String get(String embedURL, String subConfigName)
    throws Exception
      {
        String url = WebBasics.getExtra("looker-"+subConfigName, "url");
        String secret = WebBasics.getExtra("looker-"+subConfigName, "secret");
        String embed = WebBasics.getExtra("looker-"+subConfigName, "embed");
        String models = WebBasics.getExtra("looker-"+subConfigName, "models");

        return get(url, secret, embed, models, embedURL);
      }
    
    final static protected String get(String url, String secret, String embed, String models, String embedURL)
    throws Exception
      {
        String externalUserID = "\"CapsicoEmbededUser\""; // Random stuff.
        String userPermissions = "[\"see_user_dashboards\",\"see_lookml_dashboards\",\"access_data\",\"see_looks\"]";
        String groupIDs = "[]";
//        String externalGroupID = "\"\"";
        String externalGroupID = "";
        int sessionLength = 60*60*24*2; // 2 days
        boolean forceLoginLogout = true;
        String accessFilters = "{}";
        String userAttributes = "{}";
        
        String embedUrl = createURL(url, embedURL, embed, secret, externalUserID, userPermissions, models, sessionLength, accessFilters, forceLoginLogout, groupIDs, externalGroupID, userAttributes);
        return embedUrl;
      }

    final static protected String createURL(String host, String embedURL, String embedDomain, String secret, String externalUserID, String userPermissions, String models, int sessionLength, String accessFilters, boolean forceLoginLogout, String groupIDs, String externalGroupID, String userAttributes)
    throws Exception
      {
        String path = "/login/embed/" + java.net.URLEncoder.encode(embedURL, "UTF-8");
        String nonce = "\""+EncryptionUtil.getToken(18,true)+"\"";
        long time = DateTimeUtil.nowUTC().toEpochSecond();
        
        // Order of these here is very important!
        String urlToSign = host + "\n"
        + path + "\n"
        + nonce + "\n"
        + time + "\n"
        + sessionLength + "\n"
        + externalUserID + "\n"
        + userPermissions + "\n"
        + models + "\n"
        + groupIDs + "\n"
        + externalGroupID + "\n"
        + userAttributes + "\n"
        + accessFilters // It is VERY important to not have a \n here as a final blank like breaks evrything.
        ;
        
//        LOG.debug(urlToSign);
        
        String signature = EncryptionUtil.hmacSHA1(urlToSign, secret);

        // you need to %20-encode each parameter before you add to the URL
        String signedURL = "nonce=" + java.net.URLEncoder.encode(nonce, "UTF-8") +
        "&time=" + time +
        "&session_length=" + sessionLength +
        "&external_user_id=" + java.net.URLEncoder.encode(externalUserID, "UTF-8") +
        "&permissions=" + java.net.URLEncoder.encode(userPermissions, "UTF-8") +
        "&models=" + java.net.URLEncoder.encode(models, "UTF-8") +
        "&access_filters=" + java.net.URLEncoder.encode(accessFilters, "UTF-8") +
        "&group_ids=" + java.net.URLEncoder.encode(groupIDs, "UTF-8") +
        "&external_group_id=" + java.net.URLEncoder.encode(externalGroupID, "UTF-8") +
        "&user_attributes=" + java.net.URLEncoder.encode(userAttributes, "UTF-8") +
        "&force_logout_login=" + forceLoginLogout +
        "&first_name=" + java.net.URLEncoder.encode(TextUtil.printJsonQuotedStringValue("Embed"), "UTF-8") +
        "&last_name=" + java.net.URLEncoder.encode(TextUtil.printJsonQuotedStringValue("Embed"), "UTF-8") +
        "&signature=" + java.net.URLEncoder.encode(signature, "UTF-8")
        ;

        return "https://" + host + path + '?' + signedURL;

      }


    public static void main(String[] args)
    throws Exception
      {
        LOG.debug("Getting the Looker sso embed url");
        String embedURL = "/embed/dashboards/7";
        LOG.debug(get(embedURL, "asthma"));
      }

  }
