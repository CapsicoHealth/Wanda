/* ===========================================================================
 * Copyright (C) 2018 CapsicoHealth Inc.
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

/**
 * A helper class to standadize getting paging attributes from the request. It expects the attributes "start" and "size". If not specified,
 * the values 0 and 100 will be used as defaults. If size is > masSize, then maxSize is returned instead.
 * 
 * @author Laurent Hasson
 *
 */
public class Paging
  {
    /**
     * Processes the request to extract "start" and "size" values. if "start" is not specified, 0 is used by default. If "size"
     * is not specified, 100 is used by default. if "size" > masSize, then maxSize is used (to avoid DOS attacks or misuses).
     * 
     * @param Req
     * @param maxSize
     */
    public Paging(RequestUtil Req, int minSize, int maxSize)
      {
        int start = Req.getParamInt("start", false);
        if (start < 0)
          start = 0;
        int size = Req.getParamInt("size", false);
        if (size < minSize)
          size = minSize;
        else if (size > maxSize)
          size = maxSize;
        _start = start;
        _size = size;
      }

    public Paging(RequestUtil Req)
      {
        this(Req, 10, 250);
      }

    public Paging(RequestUtil Req, int maxSize)
      {
        this(Req, 10, maxSize);
      }

    public final int _start;
    public final int _size;
  }

