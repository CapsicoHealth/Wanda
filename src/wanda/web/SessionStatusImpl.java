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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.utils.SystemValues;
import tilda.utils.pairs.StringIntPair;

public class SessionStatusImpl implements SessionStatus
  {

    protected static final Logger LOG = LogManager.getLogger(SessionStatusImpl.class.getName());

    public SessionStatusImpl(RequestUtil req)
      {
        _Req = req;
      }

    protected RequestUtil _Req;

    @Override
    public void setStatus(String Message)
      {
        // LOG.debug("Setting session status for '"+_Req._Req.getServletPath()+"/msg' as "+Message);
        _Req.setSessionString(_Req.getServletPath() + "/msg", Message);
      }

    @Override
    public void setStatus(int Percent)
      {
        // LOG.debug("Setting session status for '"+_Req._Req.getServletPath()+"/percent' as "+Percent+"%");
        _Req.setSessionInt(_Req.getServletPath() + "/percent", Percent);
      }

    @Override
    public StringIntPair getSessionStatus(String servletPath)
      {
        String Message = _Req.getSessionString(servletPath + "/msg");
        int Percent = _Req.getSessionInt(servletPath + "/percent");
        // LOG.debug("Getting session status for '"+servletPath+"/msg' as "+Message);
        // LOG.debug("Getting session status for '"+servletPath+"/percent' as "+Percent+"%");

        return new StringIntPair(Message, Percent);
      }

    @Override
    public boolean isSessionStatusMarkedForClear(String servletPath)
      {
        return _Req.getSessionInt(servletPath + "/clear") == 1;
      }
    
    @Override
    public void clear(String servletPath, boolean fullClear)
      {
        clear(_Req, servletPath, fullClear);
      }
    
    public static void clear(RequestUtil req, String servletPath, boolean fullClear)
      {
        if (fullClear == true)
          {
            req.removeSessionAttribute(servletPath + "/msg");
            req.removeSessionAttribute(servletPath + "/percent");
            req.removeSessionAttribute(servletPath + "/clear");
          }
        else
          {
            String Message = req.getSessionString(servletPath + "/msg");
            int Percent = req.getSessionInt(servletPath + "/percent");
            if (Message != null || Percent != SystemValues.EVIL_VALUE)
              req.setSessionInt(servletPath + "/clear", 1);
          }
      }

    @Override
    public String getRequestServletPath()
      {
        return _Req.getServletPath();
      }
    
  }
