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

package wanda;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;
import tilda.db.ConnectionPool;
import wanda.data.AppConfig_Data;
import wanda.data.AppConfig_Factory;
import wanda.data.App_Data;
import wanda.data.App_Factory;

public class TestDB
  {
    protected static final Logger LOG = LogManager.getLogger(TestDB.class.getName());

    public static void main(String[] args)
      {
        Connection C = null;
        try
          {
            C = ConnectionPool.get("MAIN");
            
            
            // Testing a Lookup/Read/Update changing an identity col
            AppConfig_Data AC = AppConfig_Factory.lookupByAppHost(11510, "");
            if (AC.read(C) == false) // cannot find.
             throw new Exception("Cannot read AppConfig");
            AC.setHostName("aaaaa");
            if (AC.write(C) == false)
             throw new Exception("Cannot insert/update AppConfig record");

            // Testing a Lookup/Update changing an identity col
            AC = AppConfig_Factory.lookupByAppHost(11510, "aaaaa");
            AC.setHostName("bbbbbb");
            if (AC.write(C) == false)
             throw new Exception("Cannot insert/update AppConfig record");

            // Testing a Lookup/Update changing an identity col
            AC = AppConfig_Factory.create(11510, "bbbbbb", "Case Manager (DEPRECATED)", 999);
            AC.setHostName("ccccc");
            if (AC.upsert(C) == false)
             throw new Exception("Cannot upsert AppConfig record");
            
            App_Data A = App_Factory.create("aaa", "bbb", "ccc");
            LOG.debug("refnum: "+A.getRefnum());
            if (A.upsert(C) == false)
              throw new Exception("Cannot upsert App record");
            LOG.debug("refnum: "+A.getRefnum());

            A = App_Factory.create("aaa", "bbb", "eee");
            LOG.debug("refnum: "+A.getRefnum());
            if (A.upsert(C) == false)
              throw new Exception("Cannot upsert App record");
            LOG.debug("refnum: "+A.getRefnum());
            
            A.refresh(C);
            LOG.debug("refnum: "+A.getRefnum());
            
            C.rollback();
          }
        catch (Throwable E)
          {
            LOG.error("An exception occurred", E);
            try { C.rollback(); } catch(SQLException x) { }
          }
        finally
          {
            if (C != null)
              try { C.close(); } catch(SQLException E) { }
          }

      }

  }
