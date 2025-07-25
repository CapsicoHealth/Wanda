/*
 Tilda V2.5 template application class.
*/

package wanda.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;

/**
This is the application class <B>Data_AccessLogPerformanceDayView</B> mapped to the table <B>WANDA.AccessLogPerformanceDayView</B>.
@see wanda.data._Tilda.TILDA__ACCESSLOGPERFORMANCEDAYVIEW
*/
public class AccessLogPerformanceDayView_Data extends wanda.data._Tilda.TILDA__ACCESSLOGPERFORMANCEDAYVIEW
 {
   protected static final Logger LOG = LogManager.getLogger(AccessLogPerformanceDayView_Data.class.getName());

   public AccessLogPerformanceDayView_Data() { }

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
