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
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import tilda.db.Connection;
import tilda.utils.DateTimeUtil;
import wanda.data.AccessLogHourlyView_Data;
import wanda.data.AccessLogHourlyView_Factory;
import wanda.web.BeaconBit;
import wanda.web.config.BeaconBitConfig.LookbackUnit;

public class AccessLogHourlySummary implements BeaconBit
  {

    @Override
    public String getTitle()
      {
        return "Access Log Hourly Summary";
      }

    @Override
    public void run(PrintWriter out, Connection C, int lookbackCount, LookbackUnit lookbackUnit)
    throws Exception
      {
        ZonedDateTime Now = DateTimeUtil.nowLocal();
        ZonedDateTime SomeHoursAgo = Now.truncatedTo(ChronoUnit.HOURS).minusHours(lookbackCount);

        List<AccessLogHourlyView_Data> L = AccessLogHourlyView_Factory.lookupWhereHour(C, Now, SomeHoursAgo, 0, 90);
        out.println("<TABLE>\n");
        out.println("<TR><TH>Hour</TH><TH>Requests</TH><TH>200</TH><TH>400</TH><TH>500</TH><TH>401</TH><TH>404</TH><TH>Logins</TH><TH>LoginErrs</TH></TR>");
        for (AccessLogHourlyView_Data ALDV : L)
          {
            out.println("<TR><TD>" + ALDV.getHour().getHour()
            + "</TD><TD align=\"right\">" + ALDV.getCountRequests()
            + "</TD><TD align=\"right\">" + ALDV.getCountResponseCode200()
            + "</TD><TD align=\"right\">" + ALDV.getCountResponseCode400()
            + "</TD><TD align=\"right\">" + ALDV.getCountResponseCode500()
            + "</TD><TD align=\"right\">" + ALDV.getCountResponseCode401()
            + "</TD><TD align=\"right\">" + ALDV.getCountResponseCode404()
            + "</TD><TD align=\"right\">" + ALDV.getCountLogins()
            + "</TD><TD align=\"right\">" + ALDV.getCountLoginErrors()
            + "</TD></TR>");
          }
        out.println("</TABLE>");
        AccessLogDailySummary.printHttpStatusLegend(out);
      }
  }
