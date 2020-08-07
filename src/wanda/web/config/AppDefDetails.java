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
import java.util.List;

import com.google.gson.annotations.SerializedName;

import tilda.interfaces.JSONable;
import tilda.utils.TextUtil;
import tilda.utils.json.JSONUtil;

public class AppDefDetails implements JSONable
  {
    /*@formatter:off*/
    @SerializedName("label"   ) public String       _label     = null;
    @SerializedName("home"    ) public String       _home      = null;
    @SerializedName("pages"   ) public List<String> _pages     = null;
    @SerializedName("services") public List<String> _services  = null;
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
        JSONUtil.print(Out, "path", true, this._label);
        JSONUtil.print(Out, "id", false, this._home);
        Out.write("}");
      }

    @Override
    public void toJSON(Writer Out, String JsonExportName, String lead, boolean FullObject, ZonedDateTime lastsync)
    throws Exception
      {
        throw new Exception("No JSON sync exporter " + JsonExportName + " for PasswordRule.");
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

        return OK;
      }
  }
