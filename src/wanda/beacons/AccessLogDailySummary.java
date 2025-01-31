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

package wanda.beacons;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;

import tilda.db.Connection;
import tilda.utils.DateTimeUtil;
import tilda.utils.HttpStatus;
import wanda.data.AccessLogDailyView_Data;
import wanda.data.AccessLogDailyView_Factory;
import wanda.web.BeaconBit;
import wanda.web.config.BeaconBitConfig.LookbackUnit;

public class AccessLogDailySummary implements BeaconBit
  {

  @Override
  public String getTitle()
    {
      return "Access Log Daily Summary";
    }

  protected static HttpStatus[] _STATUSES = new HttpStatus[] {
      HttpStatus.OK
     ,HttpStatus.BadRequest
     ,HttpStatus.Unauthorized
     ,HttpStatus.ResourceNotFound
     ,HttpStatus.InternalServerError
   };
  
  @Override
  public boolean run(PrintWriter out, Connection C, int lookbackCount, LookbackUnit lookbackUnit)
  throws Exception
    {
      LocalDate Today = DateTimeUtil.nowLocalDate().plusDays(1);
      LocalDate SomeDaysAgo = Today.minusDays(lookbackCount);

      
      List<AccessLogDailyView_Data> L = AccessLogDailyView_Factory.lookupWhereDay(C, SomeDaysAgo, Today, 0, 90);
      out.println("<TABLE>\n");
      out.println("<TR><TH>Day</TH><TH>Users</TH><TH>Sessions</TH><TH>Requests</TH><TH>200</TH><TH>400</TH><TH>500</TH><TH>401</TH><TH>404</TH><TH>Logins</TH><TH>Login Errs</TH></TR>");
      for (AccessLogDailyView_Data ALDV : L)
        {
          out.println("<TR><TD>"+DateTimeUtil.printDate(ALDV.getDay())
                     +"</TD><TD align=\"right\">"+ALDV.getUniqueUsers()
                     +"</TD><TD align=\"right\">"+ALDV.getUniqueSessions()
                     +"</TD><TD align=\"right\">"+ALDV.getCountRequests()
                     +"</TD><TD align=\"right\">"+ALDV.getCountResponseCode200()
                     +"</TD><TD align=\"right\">"+ALDV.getCountResponseCode400()
                     +"</TD><TD align=\"right\">"+ALDV.getCountResponseCode500()
                     +"</TD><TD align=\"right\">"+ALDV.getCountResponseCode401()
                     +"</TD><TD align=\"right\">"+ALDV.getCountResponseCode404()
                     +"</TD><TD align=\"right\">"+ALDV.getCountLogins()
                     +"</TD><TD align=\"right\">"+ALDV.getCountLoginErrors()
                     +"</TD></TR>");
        }
      out.println("</TABLE>");
      printHttpStatusLegend(out);
      return true;
    }

  public static void printHttpStatusLegend(PrintWriter out)
    {
      out.println("<PRE style=\"font-size:80%;\">");
      for (HttpStatus s : _STATUSES)
        out.println("   <B>"+s._Code+"</B>: "+s._Message);
      out.println("</PRE>");
    }
  }
