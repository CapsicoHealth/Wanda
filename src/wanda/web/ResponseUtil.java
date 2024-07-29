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
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.interfaces.JSONable;
import tilda.utils.HttpStatus;
import tilda.utils.TextUtil;
import tilda.utils.json.JSONPrinter;
import tilda.utils.json.JSONUtil;
import wanda.web.exceptions.SimpleServletException;


public class ResponseUtil
  {
    protected static final Logger LOG = LogManager.getLogger(ResponseUtil.class.getName());

    public ResponseUtil(HttpServletResponse response)
      {
        _Res = response;
      }

    protected HttpServletResponse _Res;
    protected PrintWriter         _Out;

    public enum ContentType
      {
      JSON("text/json"), XML("text/xml"), CSV("text/csv"), HTML("text/html"), TXT("text/plain");

        protected String _ContentType;

        private ContentType(String contentType)
          {
            _ContentType = contentType;
          }

        public String getContentType()
          {
            return _ContentType;
          }
      }

    public PrintWriter setContentType(ContentType CT)
    throws IOException
      {
        if (_Out != null)
          throw new Error("You cannot call setContentType twice.");
        _Res.setContentType(CT.getContentType());
        _Out = _Res.getWriter();
        return _Out;
      }

    public void setDownloadFileName(String fileName)
      {
        _Res.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
      }

    public void success()
    throws IOException
      {
        if (_Out == null)
          setContentType(ContentType.JSON);
        _Out.println("{\"code\":" + HttpStatus.OK._Code + ",\"data\":{}}");
      }

    public void successJson(String JsonExportName, JSONable obj, String perfMessage)
    throws Exception
      {
        if (_Out == null)
          setContentType(ContentType.JSON);
        JSONUtil.response(_Out, JsonExportName, obj, perfMessage);
      }
    public void successJson(String JsonExportName, JSONable obj)
    throws Exception
      {
        successJson(JsonExportName, obj, null);
      }

    public void successJson(String JsonExportName, List<? extends JSONable> L, String perfMessage)
    throws Exception
      {
        if (_Out == null)
          setContentType(ContentType.JSON);
        JSONUtil.response(_Out, JsonExportName, L, perfMessage);
      }
    public void successJson(String JsonExportName, List<? extends JSONable> L)
    throws Exception
      {
        successJson(JsonExportName, L, null);
      }

    public void successJson(JSONPrinter J, String perfMessage)
    throws Exception
      {
        if (_Out == null)
          setContentType(ContentType.JSON);
        J.print(_Out, perfMessage);
      }
    public void successJson(JSONPrinter J)
    throws Exception
      {
        successJson(J, null);
      }
    public void successJsonRaw(JSONPrinter J)
    throws Exception
      {
        if (_Out == null)
          setContentType(ContentType.JSON);
        J.printRaw(_Out);
      }

    /**
     * When using client-side frameworks such as Dojo that may use an iFrame for ajax-contents, the protocol
     * is typically yo return the json data packaged inside a textarea. This function does that. It is exactly
     * equivalent to the "plain" response method except the jsonable object is output inside a textarea and the
     * writer is expected to be set up as an HTML one.
     * 
     * @param Out
     * @param JsonExportName
     * @param Obj
     * @throws Exception
     */
    public void successDojoMultipartConfig()
    throws Exception
      {
        if (_Out == null)
          setContentType(ContentType.HTML);
        _Out.write("<textarea>\n");
        success();
        _Out.write("</textarea>\n");
      }


    /**
     * This is meant for a serious system issue and will return a traditional error sequence for HTTP.
     * This is expected mostly to be used by frameworks on top of WebBasics. If you have an application
     * error from a simpleServlet, you should use the standard {@link SimpleServletException} mechanism
     * to return a regular HTTPStatus=200 response, but with a JSON error payload.
     * 
     * @param Code
     * @param Msg
     * @throws IOException
     * @throws ServletException
     */
    public void SystemError(HttpStatus Code, String Msg)
    throws IOException, ServletException
      {
        _Res.sendError(Code._Code, Msg);
        throw new ServletException(Msg);
      }

    /**
     * Streams a file's binary data using a default content type of application/octet-stream
     * 
     * @param fileName
     * @param f
     * @throws IOException
     */
    public void success(String fileName, File f)
    throws Exception
      {
        success(fileName, f, null);
      }

    /**
     * Streams a file's binary data using the supplied content type, or, if missing, the default of application/octet-stream.
     * The file must exist and an exception will be thrown if it doesn't exist.
     * 
     * @param fileName
     * @param f
     * @param contentType
     * @throws IOException
     */
    public void success(String fileName, File f, String contentType)
    throws Exception
      {
        if (f.exists() == false)
          {
            LOG.error("The file '" + f.getCanonicalPath() + "' cannot be found or accessed.");
            throw new Exception("File " + fileName + " cannot be found");
          }
        _Res.setContentType(TextUtil.isNullOrEmpty(contentType) == true ? "application/octet-stream" : contentType);
        setDownloadFileName(fileName);
        FileUtils.copyFile(f, _Res.getOutputStream());
      }

    public void setCookie(Cookie c)
      {
        _Res.addCookie(c);
      }
    
  }
