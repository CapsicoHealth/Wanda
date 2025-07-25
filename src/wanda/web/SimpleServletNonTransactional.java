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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.utils.HttpStatus;
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
public abstract class SimpleServletNonTransactional extends jakarta.servlet.http.HttpServlet implements jakarta.servlet.Servlet
  {
    protected static final Logger LOG = LogManager.getLogger(SimpleServletNonTransactional.class.getName());

    public SimpleServletNonTransactional(boolean postOnly)
      {
        _postOnly = postOnly;
      }

    protected final boolean _postOnly;

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
        try
          {
            if (cancelOtherLongRunningServlet(req) == true)
              response.setHeader("x-wanda-canceler", "1");

            justDo(req, res);
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

    protected abstract void justDo(RequestUtil Req, ResponseUtil Res)
    throws Exception;


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
