/* ===========================================================================
 * Copyright (C) 2019 CapsicoHealth Inc.
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

package wanda;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;
import tilda.db.ConnectionPool;
import tilda.db.config.ConnDefs;
import tilda.utils.AsciiArt;
import tilda.utils.EncryptionUtil;
import tilda.utils.ParseUtil;
import tilda.utils.SystemValues;
import tilda.utils.TextUtil;
import wanda.data.User_Factory;

public class CreateUpdateUser
  {
    static final Logger LOG = LogManager.getLogger(CreateUpdateUser.class.getName());

    public static void main(String[] args)
      {
        LOG.info("");
        LOG.info("Wanda User Create/Update");
        LOG.info("  - This utility will create/update a user.");
        LOG.info("  - It requires 6 parameters: <refnum>, <email>, <password>, <nameFirst>, <nameLast>, <roles>.");
        LOG.info("      - If creating a regular user, use roles \"CarCo,Nurse,Reviewer\".");
        LOG.info("      - If creating a regular user, use roles \"SA,CarCo,Nurse,Reviewer\".");
        LOG.info("");
        Connection C = null;

        try
          {
            ConnDefs._SKIP_TILDA_LOADING = true; // Skip loading Tilda infrastructure since we are 100% JDBC-based DB meta-data only.
            C = ConnectionPool.get("MAIN");
            run(C, args[0], args[1], args[2], args[3], args[4], args[5]);
            C.commit();
            C = null;
          }
        catch (Exception E)
          {
            LOG.error("An exception occurred\n", E);
            LOG.error("\n"
            + "          ======================================================================================\n"
            + AsciiArt.Error("               ")
            + "\n"
            + "                                         Cannot run the Utility.\n"
            + "          ======================================================================================\n", E);
            System.exit(-1);
          }
        finally
          {
            if (C != null)
              try
                {
                  C.rollback();
                  C.close();
                }
              catch (SQLException E)
                {
                }
          }

        LOG.info("\n"
        + "          ======================================================================================\n"
        + AsciiArt.Woohoo("                       ")
        + "\n"
        + "                                   The Utility was run successfully.\n"
        + "          ======================================================================================");
      }

    private static void run(Connection C, String refnumStr, String email, String password, String nameFirst, String nameLast, String roles)
    throws Exception
      {
        int refnum = ParseUtil.parseInteger(refnumStr, SystemValues.EVIL_VALUE);
        if (refnum == SystemValues.EVIL_VALUE)
         throw new Exception("Invalid refnum parameter passed: must be a positive or negative integer.");
        email = email.toLowerCase();
        roles = "{"+roles+"}";
        String salt = EncryptionUtil.getToken(8);
        String hashedPassword = EncryptionUtil.hash(password, salt);
        
        String q = "INSERT INTO wanda.\"user\" (refnum, email, id, roles, pswd, \"pswdSalt\", \"pswdCreateTZ\", \"pswdCreate\", \"loginType\") \n"
                  +"     VALUES ("+refnum+", "+TextUtil.escapeSingleQuoteForSQL(email)+", "+TextUtil.escapeSingleQuoteForSQL(email)+", "+TextUtil.escapeSingleQuoteForSQL(roles)+", "+TextUtil.escapeSingleQuoteForSQL(hashedPassword)+", "+TextUtil.escapeSingleQuoteForSQL(salt)+", 'UTC', now(), 'LO')\n"
                  +"     on conflict(refnum) do update set email = excluded.email, id = excluded.id, roles = excluded.roles, pswd = excluded.pswd, \"pswdSalt\" = excluded.\"pswdSalt\""
                  +";"
                  +"\n"
                  +"INSERT INTO wanda.userdetail (\"userRefnum\",\"nameLast\",\"nameFirst\")\n"
                  +"     VALUES ("+refnum+","+TextUtil.escapeSingleQuoteForSQL(nameLast)+","+TextUtil.escapeSingleQuoteForSQL(nameFirst)+")\n"
                  +"     on conflict(\"userRefnum\") do update set \"nameLast\" = excluded.\"nameLast\", \"nameFirst\" = excluded.\"nameFirst\""
                  +";"
                  ;
        
        C.executeDDL(User_Factory.SCHEMA_LABEL, User_Factory.TABLENAME_LABEL, q);
    }

  }
