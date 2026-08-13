/* ===========================================================================
 * Copyright (C) 2025 CapsicoHealth Inc.
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

package wanda.web;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import tilda.utils.HttpStatus;
import tilda.utils.TextUtil;
import wanda.web.config.Wanda;
import wanda.web.exceptions.SimpleServletException;

public class AuthApiToken
  {
    protected static final Pattern _AUTH = Pattern.compile("Bearer\\s+([\\w-]+)\\s+([\\w-]+)");

    protected AuthApiToken(String partnerId)
      {
        _partnerId = partnerId;
      }

    public final String _partnerId;

    public static AuthApiToken getAuthToken(HttpServletRequest req)
    throws Exception
      {
        String authHeader = req.getHeader("Authorization");
        if (TextUtil.isNullOrEmpty(authHeader) == true)
          return null;

        Matcher m = _AUTH.matcher(authHeader);
        if (m.matches() == false)
          throw new SimpleServletException(HttpStatus.Unauthorized, "Unauthorized request with an invalid Authorization header format: expecting 'Bearer <partnerId> <apiKey>' where <partnerId> and <apiKey> match [\\w-]+.");

        AuthApiToken t = new AuthApiToken(m.group(1));

        if (Wanda.validateApiKey(req, t._partnerId, m.group(2)) == false)
          throw new SimpleServletException(HttpStatus.Unauthorized, "Unauthorized request with an invalid partner ID and/or API Key");

        return t;
      }
  }
