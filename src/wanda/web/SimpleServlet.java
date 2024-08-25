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

package wanda.web;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;
import tilda.utils.HttpStatus;
import wanda.data.User_Data;
import wanda.servlets.helpers.RoleHelper;
import wanda.web.config.WebBasics;
import wanda.web.exceptions.SimpleServletException;

/**
 * A class to simplify how servlets work for WandA applications
 * <P>
 * This class provides several services and simplifications on top of the standard Servlet model. In particular:
 * <UL>
 * <LI>Merges GET and POST as a single method justDo() and handles these at the access control level instead through configuration.</LI>
 * <LI>Manages the transaction scope automatically, including resource cleanup if an exception is raised.</LI>
 * <LI>Manages instantiating the User object from session info.</LI>
 * <LI>Wraps HttpServletRequest and HttpServletResponse into helper classes RequestUtil and ResponseUtil.</LI>
 * <LI>Provides a helper to handle paging parameters.</LI>
 * <LI>Provides helpers to handle cancelable long running servlet executions.</LI>
 * </UL>
 * 
 * @author Laurent Hasson
 */
public abstract class SimpleServlet extends SimpleServletNonTransactional
  {
    protected static final Logger LOG = LogManager.getLogger(SimpleServlet.class.getName());

    public SimpleServlet(boolean mustAuthenticate)
      {
        this(mustAuthenticate, false, false);
      }

    public SimpleServlet(boolean mustAuthenticate, boolean postOnly)
      {
        this(mustAuthenticate, postOnly, false);
      }

    public SimpleServlet(boolean mustAuthenticate, boolean postOnly, boolean guestAllowed)
      {
        this(mustAuthenticate, postOnly, guestAllowed, false);
      }

    public SimpleServlet(boolean mustAuthenticate, boolean postOnly, boolean guestAllowed, boolean skipTransactionSetup)
      {
        super(postOnly);
        _mustAuth = mustAuthenticate;
        _guestAllowed = guestAllowed;
      }

    protected final boolean _mustAuth;
    protected final boolean _guestAllowed;

    @Override
    protected void justDo(RequestUtil request, ResponseUtil response)
    throws Exception
      {
        User_Data U = null;
        Connection C = (Connection) request.getAttribute(RequestUtil.Attributes.CONNECTION.toString());
        if (C == null)
          throw new SimpleServletException(HttpStatus.InternalServerError, "No DB connection found in the request's attributes!");

        U = (User_Data) request.getAttribute(RequestUtil.Attributes.USER.toString());
        if (U == null && _mustAuth == true)
          throw new SimpleServletException(HttpStatus.Unauthorized, "Unauthorized anonymous request");
        if (U != null && _mustAuth == true && U.hasRoles(RoleHelper.GUEST) == true && _guestAllowed == false)
          throw new SimpleServletException(HttpStatus.BadRequest, "Unauthorized guest request as per servlet configuration");

        justDo(request, response, C, U);
      }

    protected abstract void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception;


    /**
     * Checks if the user's password has expired. Mostly used for the login servlet. Wondering if it should be elsewhere.
     * 
     * @param U
     * @return
     */
    protected boolean hasPasswordExpired(User_Data U)
      {
        return U.getPswdCreate() != null && ChronoUnit.DAYS.between(U.getPswdCreate(), ZonedDateTime.now()) > WebBasics.getPasswordExpiry();
      }

    protected boolean isUserLocked(User_Data U)
      {
        return User_Data.isUserLocked(U);
      }

    /**
     * Tests is a user has at least one of the specified roles. If not, will throw a HttpStatus.Forbidden exception.
     * 
     * @param U The user to test
     * @param roles The roles requested from this user. At least one must have been assigned to the user.
     * @throws Exception A SimpleServletException exception of type HttpStatus.Forbidden
     */
    protected static void throwIfUserInvalidRole(User_Data U, String[] roles)
    throws Exception
      {
        if (!U.hasRoles(roles))
          {
            throw new SimpleServletException(HttpStatus.Forbidden, "User does not have access to this page");
          }
      }

    /**
     * Tests is a user is a SuperAdmin or explicitely an App Admin, ie, a user with the role "Admin"+appId.
     * 
     * @param U The user to test
     * @param appId The app id.
     * @throws Exception A SimpleServletException exception of type HttpStatus.Forbidden
     */
    protected static boolean throwIfUserNotSuperOrAppAdmin(User_Data U, String appId)
    throws Exception
      {
        boolean superAdmin = U.hasRoles(RoleHelper.SUPERADMIN);
        if (!superAdmin && !U.hasRoles(new String[] { "Admin" + appId
        }))
          {
            throw new SimpleServletException(HttpStatus.Forbidden, "User does not have access to this page");
          }
        return superAdmin;
      }
  }
