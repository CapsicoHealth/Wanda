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

/*
 Tilda V1.0 template application class.
*/

package wanda.data;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.*;
import tilda.db.processors.LongListRP;

/**
This is the application class <B>Data_App</B> mapped to the table <B>PEOPLE.App</B>.
@see wanda.data._Tilda.TILDA__APP
*/
public class App_Factory extends wanda.data._Tilda.TILDA__APP_Factory
 {
   protected static final Logger LOG = LogManager.getLogger(App_Factory.class.getName());

   protected App_Factory() { }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//   Implement your customizations, if any, below.
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


   public static void init(Connection C) throws Exception
    {
      // Add logic to initialize your object, for example, caching some values, or validating some things.
    }
   
   public static List<Long> getAppRefnums(Connection C, String[] appIds) throws Exception
    {
      SelectQuery Q = newSelectQuery(C);
      Q.selectColumn(App_Factory.COLS.REFNUM).where().in(App_Factory.COLS.ID, appIds);
      LongListRP RP = new LongListRP();
      Q.execute(RP, 0, -1);
      return RP.getResult();
    }

 }
