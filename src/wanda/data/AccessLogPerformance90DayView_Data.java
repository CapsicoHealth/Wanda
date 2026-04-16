/*
 Tilda V2.5 template application class.
*/

package wanda.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;

/**
This is the application class <B>Data_AccessLogPerformance90DayView</B> mapped to the table <B>WANDA.AccessLogPerformance90DayView</B>.
@see wanda.data._Tilda.TILDA__ACCESSLOGPERFORMANCE90DAYVIEW
*/
public class AccessLogPerformance90DayView_Data extends wanda.data._Tilda.TILDA__ACCESSLOGPERFORMANCE90DAYVIEW
 {
   protected static final Logger LOG = LogManager.getLogger(AccessLogPerformance90DayView_Data.class.getName());

   public AccessLogPerformance90DayView_Data() { }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//   Implement your customizations, if any, below.
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


   @Override
   protected boolean afterRead(Connection C) throws Exception
     {
       // Do things after an object has just been read form the data store, for example, take care of AUTO fields.
       return true;
     }

 }
