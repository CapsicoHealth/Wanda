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

package wanda.utils;

import wanda.data.User_Data;

import tilda.utils.pairs.StringStringPair;
import wanda.web.RequestUtil;

public class MockRequestUtil extends RequestUtil
  { 

    // Login Servlet
    public String email;
    public String password;
    public String ipAddress;
    
    // InviteUser Servlet
    public String firstName, lastName;
    public String[] roles;
    
    // UserOnBoarding Servlet
    public String token;
    public String phone;
    
    public MockRequestUtil() {
      super(null);
    }

    @Override
    public void addError(String ParamName, String Error)
      {
        _Errors.add(new StringStringPair(ParamName, Error));
      }
    
    @Override
    public String getParamString(String Name, boolean Mandatory, String DefaultValue, String[] ValidValues, boolean CaseInsensitive)
      {
        switch(Name)
          {
            case "email":
              return this.email;
            case "pswd":
            case "password":
              return this.password;
            case "firstName":
              return this.firstName;
            case "lastName":
              return this.lastName;
            case "token":
              return this.token;
            case "phone":
              return this.phone;
          }
        return null;
      }
    
    @Override
    public String[] getParamsString(String Name, boolean Mandatory)
      {
        switch(Name)
          {
            case "role":
              return this.roles; 
          }
        return null;
      }

    @Override
    public long getParamLong(String Name, boolean Mandatory)
      {
        return 0;
      }

    
    @Override
    public String getRemoteAddr()
      {
        return this.ipAddress;
      }
    
    @Override
    public void setSessionUser(User_Data U)
      {
         // Do Nothing
      }    

  }
