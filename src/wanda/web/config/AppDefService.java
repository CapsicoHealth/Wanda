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

package wanda.web.config;

import java.io.Writer;
import java.time.ZonedDateTime;

import com.google.gson.annotations.SerializedName;

import tilda.interfaces.JSONable;
import tilda.utils.TextUtil;
import tilda.utils.json.JSONUtil;

public class AppDefService implements JSONable
  {
    /*@formatter:off*/
    @SerializedName("path" )  public String  _path   = null;
    @SerializedName("access") public String  _access = null;
    @SerializedName("apiKey") public boolean _apiKey = false;
    /*@formatter:on*/

    @Override
    public void toJSON(Writer Out, String JsonExportName, String lead, boolean FullObject)
    throws Exception
      {
        toJSON(Out, JsonExportName, lead, FullObject, false);
      }

    @Override
    public void toJSON(Writer Out, String JsonExportName, String lead, boolean FullObject, boolean noNullArrays)
    throws Exception
      {
        Out.write("{");
        JSONUtil.print(Out, "path"  , true , this._path);
        JSONUtil.print(Out, "access", false, this._access);
        JSONUtil.print(Out, "apiKey", false, this._apiKey);
        Out.write("}");
      }
    
    /**
     * GST: Guest access
     * A: Administrator access
     * AA: App Administrator access
     */
    protected static final String[] _ACCESS_VALUES = new String[] {"A", "GST", "AA"};

    @Override
    public void toJSON(Writer Out, String JsonExportName, String lead, boolean FullObject, ZonedDateTime lastsync)
    throws Exception
      {
        throw new Exception("No JSON sync exporter " + JsonExportName + " for App definition services.");
      }

    public boolean validate(String appLabel)
      {
        boolean OK = true;

        if (TextUtil.isNullOrEmpty(_path) == true)
          {
            Wanda.LOG.error("The Wanda app configuration file for app " + appLabel + " defined a service value with no path.");
            OK = false;
          }
        
        if (_access != null && TextUtil.findElement(_ACCESS_VALUES, _access, true, 0) == -1)
          {
            Wanda.LOG.error("The Wanda app configuration file for app " + appLabel + " define a service '"+_path+"' with an access flag '"+_access+"' which is not one of the following: "+TextUtil.print(_ACCESS_VALUES)+".");
            OK = false;
          }
        
        return OK;
      }
  }
