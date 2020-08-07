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

package wanda.servlets;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;
import tilda.db.ConnectionPool;
import tilda.utils.DateTimeUtil;
import tilda.utils.DateTimeZone;
import tilda.utils.EncryptionUtil;
import tilda.utils.SystemValues;
import wanda.data.UserDetail_Data;
import wanda.data.UserDetail_Factory;
import wanda.data.User_Data;
import wanda.data.User_Factory;
import wanda.utils.MockRequestUtil;
import wanda.utils.MockResponseUtil;

public class SetPasswordTest
  {
    protected static final Logger LOG = LogManager.getLogger(SetPasswordTest.class.getName());
    
    public static void main(String[] args)
      {
        // email, token, password
        LOG.info("\n*************************************************************************************************************************************");
        SystemValues.autoInit();
        ConnectionPool.autoInit();
        LOG.info("\n*************************************************************************************************************************************\n");
        Connection C = null;
        try
          {
            C = ConnectionPool.get("MAIN");
            seedTestUser(C);
                        
            MockRequestUtil mockedUtil = new MockRequestUtil();
            mockedUtil.token = "xxx";
            mockedUtil.email = "xxx@xxx.com";
            mockedUtil.password = "xxx";
            
            MockResponseUtil mockedResUtil = new MockResponseUtil();
            
            SetPassword onboarding = new SetPassword();
            onboarding.justDo(mockedUtil, mockedResUtil, C, null);            
          }
        catch (Exception E)
          {
            LOG.error("An exception occurred", E);
          }
        finally
          {
            if (C != null)
              try { C.rollback(); C.close(); } catch(SQLException E) { }
          }
      }

    private static void seedTestUser(Connection C)
    throws Exception
      {
        Set<String> Roles = new HashSet<String>();
        Roles.add("SA");
        User_Data U = User_Factory.create("xxx@xxx.com", "Test", Roles, EncryptionUtil.hash("xxx"));
        U.setLoginTypeLocal();
        U.setPswdCreateNow();
        U.setPswdResetCode("xxx");
        U.setPswdResetCreateNow();
        if (U.write(C) == false)
         throw new Exception("Cannot create Test User");

        UserDetail_Data P = UserDetail_Factory.create(U.getRefnum(), "Xxx", "Xxx");
        P.setDob(DateTimeUtil.newTZ(1789, 7, 14, 0, 0, 0, 0, DateTimeZone.USEastern._ZoneId));
        P.setGender("M");
        if (P.write(C) == false)
          throw new Exception("Cannot create Test Person");
        
        
        C.commit();            
      }
    
  }
