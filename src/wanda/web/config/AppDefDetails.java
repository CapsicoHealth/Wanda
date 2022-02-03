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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.annotations.SerializedName;

import tilda.interfaces.JSONable;
import tilda.utils.TextUtil;
import tilda.utils.json.JSONUtil;

public class AppDefDetails implements JSONable
  {
    /*@formatter:off*/
    @SerializedName("label"   ) public String              _label    = null;
    @SerializedName("home"    ) public String              _home     = null;
    @SerializedName("services") public List<AppDefService> _services = null;
    @SerializedName("admin"   ) public String              _admin    = null;
    @SerializedName("policies") public List<AppDefPolicy>  _policies = null;
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
        JSONUtil.print(Out, "label", true, this._label);
        JSONUtil.print(Out, "home", false, this._home);
        JSONUtil.print(Out, "admin", false, this._admin);
        JSONUtil.print(Out, "services", "", false, _services, "  ");
        Out.write("}");
      }

    @Override
    public void toJSON(Writer Out, String JsonExportName, String lead, boolean FullObject, ZonedDateTime lastsync)
    throws Exception
      {
        throw new Exception("No JSON sync exporter " + JsonExportName + " for App definition details.");
      }

    public boolean validate(String srcFile)
      {
        boolean OK = true;

        if (TextUtil.isNullOrEmpty(_label) == true)
          {
            WebBasics.LOG.error("The WebBasics app configuration file " + srcFile + " didn't define any 'label' property");
            OK = false;
          }

        if (TextUtil.isNullOrEmpty(_home) == true)
          {
            WebBasics.LOG.error("The WebBasics app configuration file " + srcFile + " didn't define any 'home' property");
            OK = false;
          }
        
        Set<String> values = new HashSet<String>();
        if (_services != null)
         for (AppDefService s : _services)
          {
            if (s.validate(_label) == false)
             OK = false;
            else if (values.add(s._path) == false)
              {
                WebBasics.LOG.error("The WebBasics app configuration file " + srcFile + " defined the service '"+s._path+"' more than once.");
                OK = false;
              }
          }
        
        values.clear();
        if (_policies != null)
          for (AppDefPolicy p : _policies)
           {
             if (p.validate(_label) == false)
              OK = false;
             else if (values.add(p._name) == false)
               {
                 WebBasics.LOG.error("The WebBasics app configuration file " + srcFile + " defined the policy '"+p._name+"' more than once.");
                 OK = false;
               }
           }
        
        return OK;
      }
  }
