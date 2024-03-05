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
import wanda.servlets.admin.InviteUserServlet;
import wanda.utils.MockRequestUtil;

public class InviteUserTest
  {

    protected static final Logger LOG = LogManager.getLogger(InviteUserTest.class.getName());
    
    public static void main(String[] args)
      {
        LOG.info("\n*************************************************************************************************************************************");
        SystemValues.autoInit();
        ConnectionPool.autoInit();
        LOG.info("\n*************************************************************************************************************************************\n");
        Connection C = null;
        try
          {
            C = ConnectionPool.get("MAIN");            
            User_Data U = seedAdminUser(C);

            MockRequestUtil mockedUtil = new MockRequestUtil();
            mockedUtil.email = "xxx@xxx.com";
            mockedUtil.firstName = "Xxx";
            mockedUtil.lastName = "Yyy";
            mockedUtil.roles = new String[] {"A"};
            
            InviteUserServlet inviteUser = new InviteUserServlet();
            // inviteUser.justDo(mockedUtil, new PrintWriter(System.out), C, U);
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

    private static User_Data seedAdminUser(Connection C)
    throws Exception
      {
        Set<String> Roles = new HashSet<String>();
        Roles.add("A");
        String salt = "123";
        User_Data U = User_Factory.create("test@capsicohealth.com", "Test", Roles, EncryptionUtil.hash("xxx", salt), salt);
        U.setLoginTypeLocal();
        if (U.write(C) == false)
         throw new Exception("Cannot create Test User");

        UserDetail_Data P = UserDetail_Factory.create(U.getRefnum(), "Test", "User");
        P.setDob(DateTimeUtil.newTZ(1789, 7, 14, 0, 0, 0, 0, DateTimeZone.USEastern._ZoneId));
        P.setGender("M");
        if (P.write(C) == false)
          throw new Exception("Cannot create Test Person");

        C.commit();
        return U;
      }
    
  }
