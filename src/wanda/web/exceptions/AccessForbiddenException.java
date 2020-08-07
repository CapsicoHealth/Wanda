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

import tilda.utils.HttpStatus;
import tilda.utils.json.JSONUtil;

public class AccessForbiddenException extends SimpleServletException
  {

  private static final long serialVersionUID = 386226072608821181L;
  public final String _ResourceType;
  public final String _Msg;

  public AccessForbiddenException(String ResourceType, String Msg)
    {
        super(HttpStatus.Forbidden, ResourceType+": "+Msg);
        _ResourceType = ResourceType;
        _Msg = Msg;
    }
  
  @Override
  protected void PrintDetails(PrintWriter Out)
    throws IOException
    {
      Out.write("{");
      JSONUtil.print(Out, "type", true , _ResourceType);
      JSONUtil.print(Out, "msg" , false, _Msg  );
      Out.write("}");
    }
  }
