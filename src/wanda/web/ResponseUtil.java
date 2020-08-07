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
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.utils.HttpStatus;
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
      JSON("text/json"), XML("text/xml"), CSV("text/csv");

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

    public void setDownloadFileName(String FileName)
      {
        _Res.setHeader("Content-Disposition", "attachment; filename=\"" + FileName + "\"");
      }

    public void Success()
    throws IOException
      {
        if (_Out == null)
          setContentType(ContentType.JSON);
        _Out.println("{\"code\":" + HttpStatus.OK._Code + ",\"data\":{}}");
      }

    /**
     * This is meant for a serious system issue and will return a traditional error sequence for HTTP. 
     * This is expected mostly to be used by frameworks on top of WebBasics. If you have an application
     * error from a simpleServlet, you should use the standard {@link SimpleServletException} mechanism 
     * to return a regular HTTPStatus=200 response, but with a JSON error payload.
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

  }
