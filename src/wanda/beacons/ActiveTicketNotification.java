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

import tilda.db.Connection;
import tilda.db.ListResults;
import tilda.utils.DateTimeUtil;
import wanda.data.TicketWaitingView_Data;
import wanda.data.TicketWaitingView_Factory;
import wanda.web.BeaconBit;
import wanda.web.config.BeaconBitConfig.LookbackUnit;
import wanda.web.config.Wanda;

public class ActiveTicketNotification implements BeaconBit
  {
    protected static long[] _ADMIN_REFNUMS = Wanda.getTicketAccountRefnums();
    protected static int    _ALERT_TIMING  = Wanda.getTicketAlertMinutes();

    @Override
    public String getTitle()
      {
        // TODO Auto-generated method stub
        return null;
      }

    @Override
    public boolean run(PrintWriter out, Connection C, int lookbackCount, LookbackUnit lookbackUnit)
    throws Exception
      {
        ZonedDateTime Now = DateTimeUtil.nowLocal();
        ZonedDateTime SomeMinutesAgo = Now.truncatedTo(ChronoUnit.MINUTES).minusHours(_ALERT_TIMING);

        ListResults<TicketWaitingView_Data> L = TicketWaitingView_Factory.lookupWhereUnanswered(C, _ADMIN_REFNUMS, 0, 50);
        if (L.isEmpty() == true)
          return false;

        String url = Wanda.getHostName() + Wanda.getAppPath() + "/apps/admin/tickets.jsp";
        out.println("<BR>Please find below Tickets awaiting a response for the system '" + Wanda.getAppName() + "'. Tickets highlighted are past due!<BR><BR>");
        out.println("Visit <A href=\"" + url + "\">" + url + "</A> to respond to these tickets.<BR><BR>");
        out.println("<TABLE>\n");
        out.println("<TR><TH>Topic</TH><TH>Time</TH></TR>");
        for (TicketWaitingView_Data t : L)
          {
            ZonedDateTime ZDT = t.getLastAnswer() != null ? t.getLastAnswer() : t.getLastUpdated();
            boolean alert = ZDT.isBefore(SomeMinutesAgo);
            out.println("<TR " + (alert == true ? "background=\"#FFCCCC\"" : "") + ">"
            + "<TD>" + t.getTopic() + "<BR>\n" + t.getSubject() + "</TD>"
            + "<TD>" + DateTimeUtil.printDateTimeFriendly(ZDT, true, false) + "</TD>"
            + "</TR>");
          }
        out.println("</TABLE>");

        if (L.hasMore() == true)
          out.println("<BR>The are more Tickets awaiting a response...<BR>\n");

        return true;
      }
  }
