/* ===========================================================================
 * Copyright (C) 2015 CapsicoHealth Inc.
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class SessionUtil
  {
    protected static final Logger LOG               = LogManager.getLogger(SessionUtil.class.getName());

    public static final int       FORCE_RELOAD_USER = 1;
    /**
     * Force a commit on a request even if an error ocurred, for example, during a login/logout.
     */
    public static final int       FORCE_COMMIT      = 2;

    public static enum Attributes
      {
      USERREFNUM, TENANTUSERREFNUM, FORCE_COMMIT, FORCE_RELOAD_USER, PASSWORD_RESET_TRIES, EULA_CODE, EULA_CLEAR, MASKING_MODE;
      }


    /**
     * Gets the session object if it exists. Otherwise, creates a new one;
     * 
     * @param Request
     * @return
     */
    public static HttpSession getSession(HttpServletRequest Request)
      {
        HttpSession S = Request.getSession(false);
        if (S == null)
          {
            S = Request.getSession(true);
            LOG.info("Picking up new session");
          }
        else
          {
            // Too much logging here that is not very useful.
            // LOG.info("Picking up existing session");
          }
        return S;
      }

    public static void InvalidateSession(HttpServletRequest Request)
      {
        HttpSession S = Request.getSession(false);
        if (S != null)
          synchronized (S)
            {
              try
                {
                  S.invalidate();
                  LOG.info("Invalidating existing session");
                }
              catch (Exception E)
                {
                  LOG.info("Invalidating existing session: session was already invalidated.");
                }
            }
      }

  }
