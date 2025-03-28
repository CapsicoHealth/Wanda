/*
 Tilda V2.5 template application class.
*/

package wanda.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.*;

/**
This is the application class <B>Data_TourView</B> mapped to the table <B>WANDA.TourView</B>.
@see wanda.data._Tilda.TILDA__TOURVIEW
*/
public class TourView_Factory extends wanda.data._Tilda.TILDA__TOURVIEW_Factory
 {
   protected static final Logger LOG = LogManager.getLogger(TourView_Factory.class.getName());

   protected TourView_Factory() { }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//   Implement your customizations, if any, below.
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


   public static void init(Connection C) throws Exception
    {
      // Add logic to initialize your object, for example, caching some values, or validating some things.
    }

 }
