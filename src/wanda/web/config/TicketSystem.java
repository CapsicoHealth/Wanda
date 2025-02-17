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

package wanda.web.config;

import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.output.StringBuilderWriter;

import com.google.gson.annotations.SerializedName;

import tilda.db.Connection;
import tilda.db.ConnectionPool;
import wanda.Beacon;
import wanda.beacons.ActiveTicketNotification;
import wanda.data.User_Data;
import wanda.data.User_Factory;
import wanda.web.EMailSender;

public class TicketSystem
  {
    /*@formatter:off*/
    @SerializedName("enabled"      ) public boolean      _enabled      = false  ;
    @SerializedName("notifications") public Notification _notifications= new Notification()  ;
    /*@formatter:on*/

    public long[]       _notificationUserRefnums = new long[] {};

    public void validate(Connection C)
    throws Exception
      {
        if (_enabled == false)
          return;

        if (_notifications._scheduleMinutes < 60)
          _notifications._scheduleMinutes = 60;
        else if (_notifications._scheduleMinutes > 24 * 60)
          _notifications._scheduleMinutes = 24 * 60;

        String[] accounts = _notifications._accounts;
        for (int i = 0; i < accounts.length; ++i)
          if (accounts[i] != null)
            accounts[i] = accounts[i].trim().toLowerCase();
        List<User_Data> L = User_Factory.lookupWhereActiveAdminOrEmails(C, accounts, 0, 20);
        _notificationUserRefnums = new long[L.size()];
        for (int i = 0; i < L.size(); ++i)
          _notificationUserRefnums[i] = L.get(i).getRefnum();
      }

    /**
     * Must be called after validate
     */
    public void launch()
      {
        ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
        Runnable task = () -> {
          Connection C = null;
          try
            {
              C = ConnectionPool.get("MAIN");
              ActiveTicketNotification ATN = new ActiveTicketNotification();
              StringBuilderWriter out = new StringBuilderWriter(); 
              if (ATN.run(new PrintWriter(out), C, 0, null) == true)
               EMailSender.sendMailSys(_notifications._accounts, null, null, "Outstanding Tickets - "+Wanda.getAppName() , out.toString(), false, true);
              C.commit();
            }
          catch (Exception E)
            {
              Beacon.LOG.error("The Ticketing System's notification beacon failed execution\n", E);
            }
          if (C != null)
           C.closeNoThrow();
        };
        service.scheduleAtFixedRate(task, 10, _notifications._scheduleMinutes, TimeUnit.MINUTES);
      }
  }
