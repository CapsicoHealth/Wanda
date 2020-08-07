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

import java.util.Arrays;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class WebBasicsDefApps
  {
    /*@formatter:off*/
    private transient final List<String> _masterPathsInternal = Arrays.asList(new String[] { "/svc/Login"
                                                                                           ,"/svc/Logout"
                                                                                           ,"/svc/Verifications"
                                                                                           ,"/svc/user/token"
                                                                                           ,"/svc/user/onboarding"
                                                                                           ,"/svc/user/forgotPswd"
                                                                                           ,"/svc/user/setPswd"
                                                                                           ,"/svc/admin/user/invite"
                                                                                           ,"/svc/admin/tenants/create"
                                                                                           ,"/svc/admin/tenants"
                                                                                           ,"/svc/admin/tenants/files"
                                                                                           ,"/svc/admin/tenants/files/list"
                                                                                           ,"/svc/admin/tenants/startJob"
                                                                                           ,"/svc/admin/tenants/jobs"
                                                                                           ,"/svc/admin/tenants/jobs/details"
                                                                                           ,"/svc/admin/tenants/jobFiles/messages"                                                                                           
                                                                                           ,"/svc/admin/users"
                                                                                           ,"/svc/admin/user/roles"
                                                                                           ,"/svc/admin/user"
                                                                                           ,"/svc/user/account"
                                                                                           ,"/svc/admin/user/validation"
                                                                                           ,"/svc/MyTenantList"
                                                                                           ,"/svc/config"
                                                                                           ,"/svc/SelectedTenant"
                                                                                          });
  
    private transient final List<String> _authPassthroughsInternal = Arrays.asList(new String[] { "/home.jsp"
                                                                                                ,"/svc/user/token"
                                                                                                ,"/svc/Login"
                                                                                                ,"/svc/Logout"
                                                                                                ,"/svc/Verifications"
                                                                                                ,"/svc/Signup"
                                                                                                ,"/svc/user/forgotPswd"
                                                                                                ,"/svc/user/setPswd"
                                                                                                ,"/svc/user/onboarding"
                                                                                                ,"/svc/config"
                                                                                               });
    
    @SerializedName("apps"            )  public List<AppDef> _apps                 = null;
    @SerializedName("authPassthroughs")  public List<String> _authPassthroughs     = _authPassthroughsInternal;
    @SerializedName("masterPaths"     )  public List<String> _masterPaths          = _masterPathsInternal;
    /*@formatter:on*/

    public boolean validate()
    throws Exception
      {
        boolean OK = true;

        if (validateAuthPassthroughs() == false)
          OK = false;
        if (validateMasterPaths() == false)
          OK = false;

        return OK;
      }

    public boolean validateAuthPassthroughs()
    throws Exception
      {
        boolean OK = true;
        if (_authPassthroughs == null)
          {
            WebBasics.LOG.error("The property 'authPassthroughs' in the WebBasics configuration file is defined as 'null'. Must be a valid list of Url path.");
            OK = false;
          }
        else
          {
            for (String path : _authPassthroughsInternal)
              {
                if (_authPassthroughs.contains(path) == false)
                  {
                    WebBasics.LOG.error("The property 'authPassthroughs' in the WebBasics configuration file must include the path: '" + path + "'");
                    OK = false;
                  }
              }
          }
        return OK;
      }

    public boolean validateMasterPaths()
    throws Exception
      {
        boolean OK = true;
        if (_masterPaths == null)
          {
            WebBasics.LOG.error("The property 'masterPaths' in the WebBasics configuration file is defined as 'null'. Must be a valid list of Url path.");
            OK = false;
          }
        else
          {
            for (String path : _masterPathsInternal)
              {
                if (_masterPaths.contains(path) == false)
                  {
                    WebBasics.LOG.error("The property 'masterPaths' in the WebBasics configuration file must include the path: '" + path + "'");
                    OK = false;
                  }
              }
          }
        return OK;
      }
  }
