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

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import tilda.utils.ParseUtil;
import tilda.utils.SystemValues;
import tilda.utils.TextUtil;
import tilda.utils.pairs.StringStringPair;
import wanda.data.User_Data;
import wanda.web.exceptions.BadRequestException;


public class RequestUtil
  {
    protected static final Logger LOG = LogManager.getLogger(RequestUtil.class.getName());

    public static enum Attributes
      {
      CONNECTION, USER, TENANT, TENANTUSER, EXCEPTION;
      }

    public RequestUtil(HttpServletRequest request)
      {
        _Req = request;
      }

    protected HttpServletRequest     _Req;
    protected List<StringStringPair> _Errors       = new ArrayList<StringStringPair>();
    protected String                 _apiCallSsoId = null;

    public void addError(String ParamName, String Error)
      {
        _Errors.add(new StringStringPair(ParamName, Error));
      }

    public void addErrors(List<StringStringPair> errs)
      {
        _Errors.addAll(errs);
      }

    public Enumeration<String> getParameterNames()
      {
        return _Req.getParameterNames();
      }

    public Map<String, String[]> getParameterMap()
      {
        return _Req.getParameterMap();
      }

    public HttpServletRequest getHttpServletRequest()
      {
        return _Req;
      }


    public String getParamString(String Name, boolean Mandatory, String DefaultValue, String[] ValidValues, boolean CaseInsensitive)
      {
        String Val = ParseUtil.parseString(Name, Mandatory, _Req.getParameter(Name), _Errors);
        if (Mandatory == false && Val == null)
          {
            Val = DefaultValue;
          }
        if (ValidValues != null && ValidValues.length > 0 && Val != null)
          if (TextUtil.findElement(ValidValues, Val, CaseInsensitive, 0) == -1)
            {
              LOG.error("Invalid parameter '" + Name + "''s value '" + Val + "'. Valid values are: " + TextUtil.print(ValidValues) + ".");
              _Errors.add(new StringStringPair(Name, "Invalid value '" + Val + "'. Valid values are: " + TextUtil.print(ValidValues) + "."));
            }
        return Val;
      }

    public String getParamString(String Name, boolean Mandatory, String DefaultValue)
      {
        return getParamString(Name, Mandatory, DefaultValue, null, false);
      }

    public String getParamString(String Name, boolean Mandatory)
      {
        return getParamString(Name, Mandatory, null, null, false);
      }

    public String[] getParamsString(String Name, boolean Mandatory, String[] DefaultValues, String[] ValidValues, boolean CaseInsensitive)
      {
        String[] Vals = ParseUtil.parseString(Name, Mandatory, _Req.getParameterValues(Name), _Errors);
        if (Mandatory == false && (Vals == null || Vals.length == 0))
          {
            Vals = DefaultValues;
          }
        if (ValidValues != null && ValidValues.length > 0 && Vals != null)
          for (String Val : Vals)
            if (TextUtil.findElement(ValidValues, Val, CaseInsensitive, 0) == -1)
              {
                LOG.error("Invalid parameter '" + Name + "''s value '" + Val + "'. Valid values are: " + TextUtil.print(ValidValues) + ".");
                _Errors.add(new StringStringPair(Name, "Invalid value '" + Val + "'. Valid values are: " + TextUtil.print(ValidValues) + "."));
              }
        return Vals;
      }

    public String[] getParamsString(String Name, boolean Mandatory, String[] DefaultValues)
      {
        return getParamsString(Name, Mandatory, DefaultValues, null, false);
      }

    public String[] getParamsString(String Name, boolean Mandatory)
      {
        return getParamsString(Name, Mandatory, null, null, false);
      }

    public short getParamShort(String Name, boolean Mandatory)
      {
        return ParseUtil.parseShort(Name, Mandatory, _Req.getParameter(Name), _Errors);
      }

    public short getParamShort(String Name, short Default)
      {
        return ParseUtil.parseShort(_Req.getParameter(Name), Default);
      }

    public short[] getParamsShort(String Name, boolean Mandatory)
      {
        return ParseUtil.parseShort(Name, Mandatory, _Req.getParameterValues(Name), _Errors);
      }

    public int getParamInt(String Name, boolean Mandatory)
      {
        return ParseUtil.parseInteger(Name, Mandatory, _Req.getParameter(Name), _Errors);
      }

    public int getParamInt(String Name, int Default)
      {
        return ParseUtil.parseInteger(_Req.getParameter(Name), Default);
      }

    public int[] getParamsInt(String Name, boolean Mandatory)
      {
        return ParseUtil.parseInteger(Name, Mandatory, _Req.getParameterValues(Name), _Errors);
      }

    public long getParamLong(String Name, boolean Mandatory)
      {
        return ParseUtil.parseLong(Name, Mandatory, _Req.getParameter(Name), _Errors);
      }

    public long getParamLong(String Name, long Default)
      {
        return ParseUtil.parseLong(_Req.getParameter(Name), Default);
      }

    public long[] getParamsLong(String Name, boolean Mandatory)
      {
        return ParseUtil.parseLong(Name, Mandatory, _Req.getParameterValues(Name), _Errors);
      }

    public float getParamFloat(String Name, boolean Mandatory)
      {
        return ParseUtil.parseFloat(Name, Mandatory, _Req.getParameter(Name), _Errors);
      }

    public float getParamFloat(String Name, float Default)
      {
        return ParseUtil.parseFloat(_Req.getParameter(Name), Default);
      }

    public float[] getParamsFloat(String Name, boolean Mandatory)
      {
        return ParseUtil.parseFloat(Name, Mandatory, _Req.getParameterValues(Name), _Errors);
      }

    public double getParamDouble(String Name, boolean Mandatory)
      {
        return ParseUtil.parseDouble(Name, Mandatory, _Req.getParameter(Name), _Errors);
      }

    public double getParamDouble(String Name, double Default)
      {
        return ParseUtil.parseDouble(_Req.getParameter(Name), Default);
      }

    public double[] getParamsDouble(String Name, boolean Mandatory)
      {
        return ParseUtil.parseDouble(Name, Mandatory, _Req.getParameterValues(Name), _Errors);
      }

    public boolean getParamBoolean(String Name, boolean Mandatory)
      {
        return ParseUtil.parseBoolean(Name, Mandatory, _Req.getParameter(Name), _Errors);
      }

    public boolean[] getParamsBoolean(String Name, boolean Mandatory)
      {
        return ParseUtil.parseBoolean(Name, Mandatory, _Req.getParameterValues(Name), _Errors);
      }

    public char getParamChar(String Name, boolean Mandatory)
      {
        return ParseUtil.parseCharacter(Name, Mandatory, _Req.getParameter(Name), _Errors);
      }

    public char[] getParamsChar(String Name, boolean Mandatory)
      {
        return ParseUtil.parseCharacter(Name, Mandatory, _Req.getParameterValues(Name), _Errors);
      }

    public ZonedDateTime getParamZonedDateTime(String Name, boolean Mandatory)
      {
        return ParseUtil.parseZonedDateTime(Name, Mandatory, _Req.getParameter(Name), _Errors);
      }

    public ZonedDateTime getParamZonedDateTime(String Name, ZonedDateTime DefaultValue)
      {
        ZonedDateTime ZDT = ParseUtil.parseZonedDateTime(Name, false, _Req.getParameter(Name), _Errors);
        return ZDT != null ? ZDT : DefaultValue;
      }

    public ZonedDateTime[] getParamsZonedDateTime(String Name, boolean Mandatory)
      {
        return ParseUtil.parseZonedDateTime(Name, Mandatory, _Req.getParameterValues(Name), _Errors);
      }

    public LocalDate getParamLocalDate(String Name, boolean Mandatory)
      {
        return ParseUtil.parseLocalDate(Name, Mandatory, _Req.getParameter(Name), _Errors);
      }

    public LocalDate getParamLocalDate(String Name, LocalDate DefaultValue)
      {
        LocalDate LD = ParseUtil.parseLocalDate(Name, false, _Req.getParameter(Name), _Errors);
        return LD != null ? LD : DefaultValue;
      }

    public LocalDate[] getParamsLocalDate(String Name, boolean Mandatory)
      {
        return ParseUtil.parseLocalDate(Name, Mandatory, _Req.getParameterValues(Name), _Errors);
      }

    public boolean hasErrors()
      {
        return _Errors.isEmpty() == false;
      }

    public void throwIfErrors()
    throws BadRequestException
      {
        if (hasErrors() == true)
          throw new BadRequestException(getErrors());
      }

    public void throwIfErrors(List<StringStringPair> errs)
    throws BadRequestException
      {
        addErrors(errs);
        if (hasErrors() == true)
          throw new BadRequestException(getErrors());
      }

    public List<StringStringPair> getErrors()
      {
        return _Errors;
      }

    public void setSessionUser(User_Data U)
      {
        HttpSession S = SessionUtil.getSession(_Req);
        if (S != null)
          S.setAttribute(SessionUtil.Attributes.USERREFNUM.toString(), Long.valueOf(U.getRefnum()));
      }


    public void setSessionTenantUser(long tenantUserRefnum)
      {
        setSessionLong(SessionUtil.Attributes.TENANTUSERREFNUM.toString(), Long.valueOf(tenantUserRefnum));
      }

    public void setSessionLong(String Name, Long Value)
      {
        HttpSession S = SessionUtil.getSession(_Req);
        if (S != null)
          S.setAttribute(Name, Value);
      }

    public void removeSessionUser()
      {
        HttpSession S = SessionUtil.getSession(_Req);
        if (S != null)
          {
            S.removeAttribute(SessionUtil.Attributes.USERREFNUM.toString());
            S.removeAttribute(SessionUtil.Attributes.TENANTUSERREFNUM.toString());
            S.removeAttribute(SessionUtil.Attributes.EULA_CODE.toString());
            S.removeAttribute(SessionUtil.Attributes.EULA_CLEAR.toString());
          }
      }

    public void setSessionString(String Name, String Value)
      {
        HttpSession S = SessionUtil.getSession(_Req);
        if (S != null)
          S.setAttribute(Name, Value);
      }

    public void setSessionInt(String Name, int Value)
      {
        HttpSession S = SessionUtil.getSession(_Req);
        if (S != null)
          S.setAttribute(Name, Value);
      }

    public void setSessionBool(String Name, boolean Value)
      {
        HttpSession S = SessionUtil.getSession(_Req);
        if (S != null)
          S.setAttribute(Name, Value);
      }

    public Enumeration<String> getSessionNames()
      {
        HttpSession S = SessionUtil.getSession(_Req);
        return S == null ? null : S.getAttributeNames();
      }

    public String getSessionAttributes()
      {
        StringBuilder Str = new StringBuilder();
        HttpSession S = SessionUtil.getSession(_Req);
        if (S == null)
          return null;
        Enumeration<String> E = S.getAttributeNames();
        boolean First = true;
        while (E.hasMoreElements() == true)
          {
            String name = E.nextElement();
            if (First == false)
              Str.append("; ");
            else
              First = false;
            Str.append(name + ":" + S.getAttribute(name));
          }
        return Str.toString();
      }

    public String getSessionString(String Name)
      {
        HttpSession S = SessionUtil.getSession(_Req);
        return S == null ? null : (String) S.getAttribute(Name);
      }

    public int getSessionInt(String Name)
      {
        HttpSession S = SessionUtil.getSession(_Req);
        Object o = S == null ? null : S.getAttribute(Name);
        return o == null ? SystemValues.EVIL_VALUE : (int) o;
      }

    public long getSessionLong(String Name)
      {
        HttpSession S = SessionUtil.getSession(_Req);
        Object o = S == null ? null : S.getAttribute(Name);
        return o == null ? SystemValues.EVIL_VALUE : (long) o;
      }

    public void removeSessionAttribute(String Name)
      {
        HttpSession S = SessionUtil.getSession(_Req);
        if (S != null)
          S.removeAttribute(Name);
      }


    public String getBasePath()
      {
        return _Req.getServletContext().getRealPath("");
      }

    public String getUploadsPath()
    throws Exception
      {
        String uploadsPath = getBasePath() + File.separator + ".." + File.separator + ".." + File.separator + ".." + File.separator + "uploads";
        File D = new File(uploadsPath);
        if (D.mkdirs() == false)
          {
            if (D.exists() == false)
              {
                LOG.error("Cannot create/access base upload folder '" + D.getAbsolutePath() + "'.");
                throw new Exception("Cannot create/access base upload folder.");
              }
          }

        return D.getAbsolutePath();
      }

    public String getRequestURL()
      {
        return _Req.getRequestURL().toString();
      }

    public String getRemoteAddr()
      {
        return _Req.getRemoteAddr();
      }

    public String getRequestedSessionId()
      {
        return _Req.getRequestedSessionId();
      }

    public String getHeader(String header)
      {
        return _Req.getHeader(header);
      }

    public Collection<Part> getParts()
    throws IOException, ServletException
      {
        return _Req.getParts();
      }

    /**
     * Extracts file name from HTTP header content-disposition
     */
    public static String extractFileName(Part part)
      {
        String contentDisp = part.getHeader("content-disposition");
        String[] items = contentDisp.split(";");
        for (String s : items)
          if (s.trim().startsWith("filename"))
            return s.substring(s.indexOf("=") + 2, s.length() - 1);
        return "";
      }

    public String getQueryString()
      {
        // TODO Auto-generated method stub
        return _Req.getQueryString();
      }

    public SessionStatus getSessionStatus()
      {
        return new SessionStatusImpl(this);
      }

    public String getServletPath()
      {
        return _Req.getServletPath();
      }

    public Collection<? extends ServletRegistration> getServletList()
      {
        return _Req.getServletContext().getServletRegistrations().values();
      }

    public Object getAttribute(String name)
      {
        return _Req.getAttribute(name);
      }

    public Cookie getCookie(String cookieName)
      {
        Cookie[] cookies = _Req.getCookies();
        if (cookies != null)
          for (Cookie c : cookies)
            if (c.getName().equals(cookieName) == true)
              return c;
        return null;
      }

    public void setApiCall(String ssoId)
      {
        _apiCallSsoId = ssoId;
      }

    public String getApiCallSsoId()
      {
        return _apiCallSsoId;
      }

    public boolean isResourceMapped()
      {
        return isResourceMapped(_Req.getServletContext(), _Req.getServletPath());
      }

    public static boolean isResourceMapped(HttpServletRequest req)
      {
        return isResourceMapped(req.getServletContext(), req.getServletPath());
      }

    public static boolean isResourceMapped(ServletContext context, String resourcePath)
      {
        try
          {
            return context.getResource(resourcePath) != null;
          }
        catch (Exception e)
          {
            return false; // Handle invalid paths or other exceptions
          }
      }

    public ServletContext getServletContext()
      {
        return _Req.getServletContext();
      }

    public String getReferrerFolder(ServletContext context)
      {
        String referrer = _Req.getHeader("Referer");
        if (TextUtil.isNullOrEmpty(referrer) == true)
          return null;

        int i = referrer.lastIndexOf('?');
        if (i != -1)
          referrer = referrer.substring(0, i);
        i = referrer.lastIndexOf('/');
        if (i != -1)
          referrer = referrer.substring(0, i);
        String contextPath = _Req.getContextPath() + "/";
        i = referrer.indexOf(contextPath);
        if (File.separator.equals("\\") == true)
          referrer = referrer.replace("/", File.separator);
        if (i != -1)
          return context.getRealPath("/") + referrer.substring(i + contextPath.length()) + File.separator;

        return null;
      }

  }
