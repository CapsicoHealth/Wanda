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

import com.google.gson.annotations.SerializedName;

public class LookAndFeel
  {
    /*@formatter:off*/
    @SerializedName("pageTitle"                  ) public String _pageTitle                   = null;
    @SerializedName("logoBig"                    ) public String _logoBig                     = null;
    @SerializedName("logoSmall"                  ) public String _logoSmall                   = null;
    @SerializedName("overlayText"                ) public String _overlayText                 = "";

    @SerializedName("urlRedirectPostLogin"       ) public String _urlRedirectPostLogin        = null;
    @SerializedName("pageTitlePostLogin"         ) public String _pageTitlePostLogin          = null;
    @SerializedName("logoBigPostLogin"           ) public String _logoBigPostLogin            = null;
    @SerializedName("logoSmallPostLogin"         ) public String _logoSmallPostLogin          = null;
    @SerializedName("overlayTextPostLogin"       ) public String _overlayTextPostLogin        = "";

    @SerializedName("poweredBy"                  ) public String _poweredBy                   = "";
    @SerializedName("copyright"                  ) public String _copyright                   = "";

    @SerializedName("overrideCssFile"            ) public String _overrideCssFile             = null;
    /*@formatter:on*/
  }
