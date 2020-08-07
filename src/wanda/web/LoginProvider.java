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

package wanda.web;

import wanda.data.User_Data;

import tilda.db.Connection;

public class LoginProvider
  {

    public static interface Interface
     {
        public User_Data login(Connection C, String username, String password, String domain)  throws Exception;
     }
    
    protected static Interface _ProviderSingleton = null;
    
    public static void RegisterProvider(Interface P)
     {
       _ProviderSingleton = P;
     }    

    public static User_Data login(Connection C, String username, String password, String domain) throws Exception
     {
       if (_ProviderSingleton == null)
        throw new Exception("Login Provider not initialized");
       return _ProviderSingleton.login(C, username, password, domain);
     }
    
  }
