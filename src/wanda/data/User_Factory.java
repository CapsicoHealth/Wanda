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


package wanda.data;

import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.servlet.http.HttpServletRequest;
import tilda.db.Connection;
import tilda.utils.EncryptionUtil;

/**
 * This is the application class <B>Data_USER</B> mapped to the table <B>WANDA.USER</B>.
 * 
 * @see wanda.data._Tilda.TILDA__USER
 */
public class User_Factory extends wanda.data._Tilda.TILDA__USER_Factory
  {
    protected static final Logger LOG = LogManager.getLogger(User_Factory.class.getName());

    public User_Factory()
      {
      }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Implement your customizations, if any, below.
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    public static void init(Connection C)
    throws Exception
      {
        // Add logic to initialize your object, for example, caching some values, or validating some things.
      }

    protected static String makePartnerEmail(String partnerId)
      {
        return "@@@" + partnerId;
      }
    
    protected static Set<String> _DEFAULT_API_ROLE = new HashSet<String>(Set.of("API"));

    public static User_Data createAPiUser(HttpServletRequest request, Connection C, String partnerId)
    throws Exception
      {
        String email = makePartnerEmail(partnerId);
        User_Data apiUser = User_Factory.lookupByEmail(email);
        if (apiUser.read(C) == false)
          {
            apiUser = User_Factory.create(email, email, _DEFAULT_API_ROLE, EncryptionUtil.getToken(24, true), EncryptionUtil.getToken(12, true));
            apiUser.setLockedNow();
            apiUser.setLoginType(User_Data._loginTypeAPI);
          }
        apiUser.setLastipaddress(request.getRemoteAddr());
        apiUser.setLastLoginNow();
        apiUser.setLoginCount(apiUser.getLoginCount() + 1);

        if (apiUser.write(C) == false)
          throw new Exception("Cannot create API user for partner " + partnerId + "!");

        return apiUser;
      }


  }
