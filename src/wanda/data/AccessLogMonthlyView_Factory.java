/*
 Tilda V2.5 template application class.
*/

package wanda.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.*;

/**
This is the application class <B>Data_AccessLogMonthlyView</B> mapped to the table <B>WANDA.AccessLogMonthlyView</B>.
@see wanda.data._Tilda.TILDA__ACCESSLOGMONTHLYVIEW
*/
public class AccessLogMonthlyView_Factory extends wanda.data._Tilda.TILDA__ACCESSLOGMONTHLYVIEW_Factory
 {
   protected static final Logger LOG = LogManager.getLogger(AccessLogMonthlyView_Factory.class.getName());

   protected AccessLogMonthlyView_Factory() { }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//   Implement your customizations, if any, below.
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


   public static void init(Connection C) throws Exception
    {
      // Add logic to initialize your object, for example, caching some values, or validating some things.
    }

 }
