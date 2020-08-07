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


public class NotFoundException extends SimpleServletException
  {
    private static final long serialVersionUID = -7532753332088264853L;
    public NotFoundException(String ResourceType, String ResourceId, String ErrorMessage)
      {
        super(HttpStatus.ResourceNotFound, ErrorMessage);

        _ResourceType = ResourceType;
        _ResourceId = ResourceId;
      }

    public NotFoundException(String ResourceType, String ResourceId)
      {
        super(HttpStatus.ResourceNotFound, "Unknown resource '"+ResourceType+"': '"+ResourceId+"'.");

        _ResourceType = ResourceType;
        _ResourceId = ResourceId;
      }
    public NotFoundException(String ResourceType, long ResourceRefnum)
      {
        this(ResourceType, Long.toString(ResourceRefnum));
      }

    public final String _ResourceType;
    public final String _ResourceId;

    @Override
    protected void PrintDetails(PrintWriter Out)
      throws IOException
      {
        Out.write("{");
        JSONUtil.print(Out, "type", true , _ResourceType);
        JSONUtil.print(Out, "id"  , false, _ResourceId  );
        Out.write("}");
      }

  }
