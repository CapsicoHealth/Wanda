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
import tilda.utils.json.JSONUtil;

public class SubApp implements JSONable
  {
    /*@formatter:off*/
    @SerializedName("id"   ) public String  _id   = null;
    @SerializedName("label") public String  _label= null;
    @SerializedName("home" ) public String  _home = null;
    @SerializedName("tour" ) public boolean _tour = false;
    @SerializedName("admin") public String  _admin= null;
    /*@formatter:on*/

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
        JSONUtil.print(Out, "id"   , true , this._id   );
        JSONUtil.print(Out, "home" , false, this._home );
        JSONUtil.print(Out, "label", false, this._label);
        JSONUtil.print(Out, "tour" , false, this._tour );
        JSONUtil.print(Out, "admin", false, this._admin );
        Out.write("}");

      }

    @Override
    public void toJSON(Writer Out, String JsonExportName, String lead, boolean FullObject, ZonedDateTime lastsync)
    throws Exception
      {
        throw new Exception("No JSON sync exporter " + JsonExportName + " for PasswordRule.");
      }

  }
