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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.*;

/**
This is the application class <B>Data_AccessLogPerformanceDayView</B> mapped to the table <B>WANDA.AccessLogPerformanceDayView</B>.
@see wanda.data._Tilda.TILDA__ACCESSLOGPERFORMANCEDAYVIEW
*/
public class AccessLogPerformanceDayView_Factory extends wanda.data._Tilda.TILDA__ACCESSLOGPERFORMANCEDAYVIEW_Factory
 {
   protected static final Logger LOG = LogManager.getLogger(AccessLogPerformanceDayView_Factory.class.getName());

   protected AccessLogPerformanceDayView_Factory() { }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//   Implement your customizations, if any, below.
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


   public static void init(Connection C) throws Exception
    {
      // Add logic to initialize your object, for example, caching some values, or validating some things.
    }

 }