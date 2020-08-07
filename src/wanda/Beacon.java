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

import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;

import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;
import tilda.db.ConnectionPool;
import tilda.utils.AsciiArt;
import tilda.utils.DateTimeUtil;
import tilda.utils.TextUtil;
import wanda.web.SystemMailSender;
import wanda.web.config.BeaconBitConfig;
import wanda.web.config.BeaconConfig;
import wanda.web.config.WebBasics;

public class Beacon
  {
    static final Logger LOG = LogManager.getLogger(Beacon.class.getName());

    public static void main(String[] Args)
      {
        LOG.info("");
        LOG.info("Wanda Beacon");
        LOG.info("  - This utility will load the list of active beacon bits from /WebBasics.config.json, run them");
        LOG.info("  - This utility will then email the results to the destination list as per the configuration");
        LOG.info("");
        Connection C = null;

        try
          {
            C = ConnectionPool.get("MAIN");
            run(C);
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
            + "                                         Cannot run the Beacon.\n"
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
        + "                                   The Wanda Beacon was run successfully.\n"
        + "          ======================================================================================");
      }

    private static void run(Connection C)
    throws Exception
      {
        BeaconConfig B = WebBasics.getBeaconConfig();
        
        if (B == null || TextUtil.isNullOrEmpty(B._emails) == true)
          {
            LOG.warn("The beacon has not been configured properly in the Wanda configuration file or no emails have been supplied to send the beacon to.");
            return;
          }
        
        StringBuilderWriter Str = new StringBuilderWriter();
        PrintWriter Out = new PrintWriter(Str);

        InetAddress ip = null;
        try
          {
            ip = InetAddress.getLocalHost();
          }
        catch (UnknownHostException E)
          {
            LOG.warn("Cannot retrieve the current machine's IP address and hostname\n", E);
          }

        Out.println("<PRE>");
        Out.println("<B>APPLICATION</B>  : " + WebBasics.getAppName());
        Out.println("<B>SERVER CONFIG</B>: " + WebBasics.getHostName());
        Out.println("<B>MACHINE</B>      : " + ip);
        Out.println("<B>DATE/TIME</B>    : " + DateTimeUtil.printDateTimeFriendly(DateTimeUtil.nowLocal(), true, true));
        Out.println("</PRE>");

        for (BeaconBitConfig b : B._bits)
          {
            if (b != null && b._bitObj != null)
              {
                Out.println("<BR>");
                Out.println("<BR>");
                Out.println("<B>=========================================================================<BR>");
                Out.println(   "==  <SPAN style=\"font-variant:small-caps;\">" + b._bitObj.getTitle()+"</SPAN><BR>");
                Out.println(   "=========================================================================</B><BR>");
                try
                  {
                    LOG.debug("Running beacon '"+b._bitObj.getTitle()+"' from '"+b._className+"'.");
                    b._bitObj.run(Out, C, b._lookback, b._timing);
                  }
                catch (Throwable T)
                  {
                    Out.println("<BR>");
                    Out.println("<PRE>");
                    Out.println("################################################################################");
                    Out.println("## Some error occurred in the Beacon Bit:");
                    Out.println(T.getMessage());
                    Out.println("################################################################################\n\n\n\n");
                    Out.println("</PRE>");
                    Out.println("<BR>");
                    Out.println("<BR>");
                    Out.println("<BR>");
                    LOG.error("\n\n\n"
                             +"###################################################################\n"
                             +"The beacon '"+b._bitObj.getTitle()+"' failed!\n"
                             +"###################################################################");
                    LOG.error("\n", T);
                  }
              }
          }
        Out.println("<BR>");
        Out.println("<BR>");
        Out.println("<BR>");
        Out.println("<PRE>");
        Out.println("FINISHED: " + DateTimeUtil.printDateTimeFriendly(DateTimeUtil.nowLocal(), true, true));
        Out.println("</PRE>");
        Out.println("<BR>");
        
        SystemMailSender.sendMail(B._emails, null, null, "Beacon - "+WebBasics.getAppName()+" - "+ip , Str.toString(), false, true);
        
    }

  }
