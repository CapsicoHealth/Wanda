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

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
public abstract class SimpleServlet extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet
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
        _mustAuth = mustAuthenticate;
        _postOnly = postOnly;
        _guestAllowed = guestAllowed;
      }

    protected final boolean _mustAuth;
    protected final boolean _postOnly;
    protected final boolean _guestAllowed;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
      {
        if (_postOnly == true)
          throw new SimpleServletException(HttpStatus.MethodNotAllowed, "This endpoint can only be called as a POST.");
        doPost(request, response);
      }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
      {
        RequestUtil req = new RequestUtil(request);
        ResponseUtil res = new ResponseUtil(response);
        User_Data U = null;
        try
          {
            if (cancelOtherLongRunningServlet(req) == true)
              response.setHeader("x-wanda-canceler", "1");
            Connection C = (Connection) request.getAttribute(RequestUtil.Attributes.CONNECTION.toString());
            if (C == null)
              throw new SimpleServletException(HttpStatus.InternalServerError, "No DB connection found in the request's attributes!");

            U = (User_Data) request.getAttribute(RequestUtil.Attributes.USER.toString());
            if (U == null && _mustAuth == true)
              throw new SimpleServletException(HttpStatus.Unauthorized, "Unauthorized anonymous request");
            if (U != null && _mustAuth == true && U.hasRoles(RoleHelper.GUEST) == true && _guestAllowed == false)
              throw new SimpleServletException(HttpStatus.Unauthorized, "Unauthorized guest request");

            justDo(req, res, C, U);
          }
        catch (SimpleServletException E)
          {
            E.Print(response.getWriter());
            throw E;
          }
        catch (ServletException | IOException E)
          {
            SimpleServletException.Print(response.getWriter(), E);
            throw E;
          }
        catch (Throwable T)
          {
            LOG.catching(T);
            SimpleServletException.Print(response.getWriter(), T);
            throw new ServletException("Generic exception in servlet", T);
          }
        finally
          {
            SessionStatusImpl.clear(req, req.getServletPath(), false); // Mark session status for clear only
            List<LongRunningServletCanceler> LRSCL = _LongRunningServletRegistry.remove(getServletInstanceSignature(req)); // Clear any long running signatures if any.
            if (LRSCL != null)
              for (LongRunningServletCanceler LRSC : LRSCL)
                if (LRSC != null)
                  try
                    {
                      LRSC.cancel();
                    }
                  catch (Throwable T)
                    {
                      LOG.error("Cannot cancel a LongRunningServletCanceler\n", T);
                    }
          }
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

    /**
     * Registry of long running servlet cancelers
     */
    private static Map<String, List<LongRunningServletCanceler>> _LongRunningServletRegistry = new HashMap<String, List<LongRunningServletCanceler>>();

    /**
     * Creates a signature for a servlet consisting of the servlet's name and the user so we can detect for long-running expensive servlets
     * if a user is trying to rerun this again concurrently.
     * 
     * @return
     */
    private static String getServletInstanceSignature(RequestUtil Req)
      {
        return Req._Req.getServletPath() + "``" + Req.getSessionLong(SessionUtil.Attributes.USERREFNUM.toString()) + "``" + Req.getSessionLong(SessionUtil.Attributes.TENANTUSERREFNUM.toString());
      }

    /**
     * Allows a simple servlet derived class to register cancelers. By calling this method, the servlet essentially self-registers itself
     * as a cancelable servlet. This means that if the current user calls this servlet again before the first run is complete, that prior
     * run will be canceled, whatever that means from the point of view of the servlet implementation of the canceler's cancel() method.
     * 
     * @param LRSC The canceler
     */
    public static void registerCanceler(RequestUtil Req, LongRunningServletCanceler LRSC)
      {
        String Id = getServletInstanceSignature(Req);

        List<LongRunningServletCanceler> LRSCL = _LongRunningServletRegistry.get(Id);
        if (LRSCL == null)
          {
            LRSCL = new ArrayList<LongRunningServletCanceler>();
            _LongRunningServletRegistry.put(Id, LRSCL);
          }
        LRSCL.add(LRSC);
      }

    /**
     * Calls the cancel() method on all the registered cancelers for the prior running instance of this servlet for this user.
     * 
     * @throws Exception
     */
    private static boolean cancelOtherLongRunningServlet(RequestUtil Req)
    throws Exception
      {
        String Id = getServletInstanceSignature(Req);
        List<LongRunningServletCanceler> LRSCL = _LongRunningServletRegistry.get(Id);
        boolean canceled = false;
        if (LRSCL != null)
          for (LongRunningServletCanceler LRSC : LRSCL)
            {
              LRSC.cancel();
              canceled = true;
            }
        return canceled;
      }
  }
