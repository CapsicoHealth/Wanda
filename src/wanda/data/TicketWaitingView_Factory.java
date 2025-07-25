/*
 Tilda V2.5 template application class.
*/

package wanda.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.*;

/**
This is the application class <B>Data_TicketWaitingView</B> mapped to the table <B>WANDA.TicketWaitingView</B>.
@see wanda.data._Tilda.TILDA__TICKETWAITINGVIEW
*/
public class TicketWaitingView_Factory extends wanda.data._Tilda.TILDA__TICKETWAITINGVIEW_Factory
 {
   protected static final Logger LOG = LogManager.getLogger(TicketWaitingView_Factory.class.getName());

   protected TicketWaitingView_Factory() { }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//   Implement your customizations, if any, below.
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


   public static void init(Connection C) throws Exception
    {
      // Add logic to initialize your object, for example, caching some values, or validating some things.
    }

 }
