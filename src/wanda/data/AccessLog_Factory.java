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

import java.time.LocalDate;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.*;
import tilda.db.processors.StringListRP;
import tilda.utils.DateTimeUtil;

/**
This is the application class <B>Data_ACCESSLOG</B> mapped to the table <B>PEOPLE.ACCESSLOG</B>.
@see wanda.data._Tilda.TILDA__ACCESSLOG
*/
public class AccessLog_Factory extends wanda.data._Tilda.TILDA__ACCESSLOG_Factory
 {
   protected static final Logger LOG = LogManager.getLogger(AccessLog_Factory.class.getName());

   protected AccessLog_Factory() { }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//   Implement your customizations, if any, below.
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


   public static void init(Connection C) throws Exception
    {
      // Add logic to initialize your object, for example, caching some values, or validating some things.
    }
   
   
   /**
    * returns the distinct list of servlets that were active at least once between dayStart (inclusive)
    * and dayEnd (exclusive). Results are ordered by servlet name.
    * @param C
    * @param dayStart
    * @param dayEnd
    * @param start
    * @param size
    * @return
    * @throws Exception
    */
   public static List<String> getDistinctServlets(Connection C, LocalDate dayStart, LocalDate dayEnd, int start, int size) throws Exception
   {
     String q = """
                SELECT DISTINCT servlet FROM %s
                 WHERE created >= %s
                   AND created::DATE < %s
                   AND servlet not like '%%.jsp'
                   AND servlet <> '/svc/user/token'
                   AND "responseCode" not in ('302','401','404')
                 ORDER BY servlet
                """.formatted(SCHEMA_TABLENAME_LABEL
                             ,DateTimeUtil.printDateForSQLQuoted(dayStart)
                             ,DateTimeUtil.printDateForSQLQuoted(dayEnd)
                             );
    StringListRP RP = new StringListRP();
    C.executeSelect(SCHEMA_LABEL, TABLENAME_LABEL, q, RP, start, size);
    return RP.getResult();
   }

 }
