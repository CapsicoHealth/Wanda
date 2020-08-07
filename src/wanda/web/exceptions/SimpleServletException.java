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

package wanda.web.exceptions;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.QueryDetails;
import tilda.utils.HttpStatus;
import tilda.utils.json.JSONUtil;

/**
 * Default mechanism for applications to return an error code and payload to a client application
 * under a standard HTTP 200 response. Derive from this class to implement your own structure. Any
 * derived class' should print a JSON structure of the form:
 *  { "code": some_int_code
 *   ,"msg": "some_str_message"
 *   ,"errors": {
 *        some_json_object
 *     }
 *  }
 * @author Laurent Hasson
 *
 */
public class SimpleServletException extends ServletException
  {
    private static final long serialVersionUID = 8294604560642579915L;
    
    static final Logger LOG = LogManager.getLogger(SimpleServletException.class.getName());

    public SimpleServletException(HttpStatus Status, String Msg)
      {
        super(Msg + "  (" + Status._Code + ")");
        _Status = Status;
        _Msg = Msg;
      }

    protected final HttpStatus _Status;
    protected final String     _Msg;

    public HttpStatus getStatus()
      {
        return _Status;
      }

    protected void PrintDetails(PrintWriter Out)
      throws IOException
      {
        Out.write("null");
      }

    public void Print(PrintWriter Out)
      throws IOException
      {
        if (QueryDetails.isLastQueryDeadlocked() == true)
          LOG.error(" =====> The servlet was interrupted by a deadlock");
         if (QueryDetails.isLastQueryCanceled() == true)
           LOG.error(" =====> The servlet was interrupted by a cancelation");
        Out.write("{");
        JSONUtil.print(Out, "code", true , _Status._Code);
        JSONUtil.print(Out, "type", false, QueryDetails.isLastQueryDeadlocked() ? "DEADLOCKED" : QueryDetails.isLastQueryCanceled() ? "CANCELED" : "OTHER");
        JSONUtil.print(Out, "msg" , false, _Msg);
        Out.write(",\"errors\":\n");
        PrintDetails(Out);
        Out.println("}");
      }


    public static void Print(PrintWriter Out, Throwable T)
      throws IOException
      {
        if (QueryDetails.isLastQueryDeadlocked() == true)
         LOG.error(" =====> The servlet was interrupted by a deadlock");
        if (QueryDetails.isLastQueryCanceled() == true)
          LOG.error(" =====> The servlet was interrupted by a cancelation");
        Out.write("{");
        JSONUtil.print(Out, "code"  , true , HttpStatus.InternalServerError._Code);
        JSONUtil.print(Out, "type"  , false, QueryDetails.isLastQueryDeadlocked() ? "DEADLOCKED" : QueryDetails.isLastQueryCanceled() ? "CANCELED" : "OTHER");
        JSONUtil.print(Out, "msg"   , false, T.getMessage());
        JSONUtil.print(Out, "errors", false, (String) null);
        Out.write("}");
      }

  }
