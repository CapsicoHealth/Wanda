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
import java.util.ArrayList;
import java.util.List;



import tilda.utils.HttpStatus;
import tilda.utils.json.JSONUtil;
import tilda.utils.pairs.StringStringPair;


public class BadRequestException extends SimpleServletException
  {
    private static final long serialVersionUID = -7532753332088264853L;

    public BadRequestException(String ParamName, String ErrorMessage)
      {
        super(HttpStatus.BadRequest, "Missing or invalid parameter(s).");
        _Errors = new ArrayList<StringStringPair>();
        _Errors.add(new StringStringPair(ParamName, ErrorMessage));
      }

    public BadRequestException(List<StringStringPair> Errors)
      {
        super(HttpStatus.BadRequest, "Missing or invalid parameter(s).");
        _Errors = Errors;
      }

    public final List<StringStringPair> _Errors;

    @Override
    protected void PrintDetails(PrintWriter Out)
      throws IOException
      {
        Out.write("[");
        boolean First = true;
        for (StringStringPair nvp : _Errors)
          {
            if (First == true)
              First = false;
            else
              Out.write("\n,");
            Out.write("{");
            JSONUtil.print(Out, "p", true , nvp._N);
            JSONUtil.print(Out, "m", false, nvp._V);
            Out.write("}");
          }
        Out.println("]");
      }

  }
