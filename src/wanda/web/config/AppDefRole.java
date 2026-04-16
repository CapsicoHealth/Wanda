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

public class AppDefRole implements JSONable
  {
    /*@formatter:off*/
    @SerializedName("id"      ) public String              _id       = null;
    @SerializedName("label"   ) public String              _label    = null;
    @SerializedName("services") public List<AppDefService> _services = null;
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
        JSONUtil.print(Out, "id", true, this._id);
        JSONUtil.print(Out, "label", false, this._label);
        JSONUtil.print(Out, "services", "", false, this._services, "  ");
        Out.write("}");
      }

    @Override
    public void toJSON(Writer Out, String JsonExportName, String lead, boolean FullObject, ZonedDateTime lastsync)
    throws Exception
      {
        throw new Exception("No JSON sync exporter " + JsonExportName + " for App definition services.");
      }

    public boolean validate(String appLabel)
      {
        boolean OK = true;

        if (TextUtil.isNullOrEmpty(_id) == true)
          {
            Wanda.LOG.error("The Wanda app configuration file for app " + appLabel + " defined a role with no id.");
            OK = false;
          }
        if (TextUtil.isNullOrEmpty(_label) == true)
          {
            Wanda.LOG.error("The Wanda app configuration file for app " + appLabel + " defined a role '"+_id+"' with no label.");
            OK = false;
          }

        return OK;
      }
  }
