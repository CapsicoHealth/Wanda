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

public class AppDef implements JSONable
  {
    /*@formatter:off*/
    @SerializedName("id"   ) public String _id   = null;
    @SerializedName("label") public String _label= null;
    @SerializedName("path" ) public String _path = null;
    /*@formatter:on*/

    transient public String _subAppOfId = null;
    transient public AppDefDetails _AppDefDetail = null;

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
        JSONUtil.print(Out, "path", true, this._path);
        JSONUtil.print(Out, "home", false, this._AppDefDetail._home);
        JSONUtil.print(Out, "admin", false, this._AppDefDetail._admin);
        JSONUtil.print(Out, "adminRole", false, TextUtil.isNullOrEmpty(this._AppDefDetail._admin) == true ? "" : "Admin"+this._id);
        JSONUtil.print(Out, "label", false, this._AppDefDetail._label);
        Out.write("}");

      }

    @Override
    public void toJSON(Writer Out, String JsonExportName, String lead, boolean FullObject, ZonedDateTime lastsync)
    throws Exception
      {
        throw new Exception("No JSON sync exporter " + JsonExportName + " for PasswordRule.");
      }
    
    public AppDef createSubApp(SubApp sa)
      {
        AppDef a = new AppDef();
        a._id = _id + "_"+sa._id;
        a._label = sa._label;
        a._path = _path;
        a._subAppOfId = _id;
        a._AppDefDetail = new AppDefDetails();
        a._AppDefDetail._label = sa._label;
        a._AppDefDetail._home = sa._home;
        a._AppDefDetail._admin = sa._admin;
        a._AppDefDetail._tour = sa._tour;
        a._AppDefDetail._policies = _AppDefDetail._policies;
        a._AppDefDetail._requiredRoles = _AppDefDetail._requiredRoles;
        a._AppDefDetail._services = _AppDefDetail._services;
        return a;
      }

  }
