/*
 Tilda V2.5 template application class.
*/

package wanda.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.*;

/**
This is the application class <B>Data_TourUserClick</B> mapped to the table <B>WANDA.TourUserClick</B>.
@see wanda.data._Tilda.TILDA__TOURUSERCLICK
*/
public class TourUserClick_Factory extends wanda.data._Tilda.TILDA__TOURUSERCLICK_Factory
 {
   protected static final Logger LOG = LogManager.getLogger(TourUserClick_Factory.class.getName());

   protected TourUserClick_Factory() { }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//   Implement your customizations, if any, below.
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


   public static void init(Connection C) throws Exception
    {
      // Add logic to initialize your object, for example, caching some values, or validating some things.
    }

 }
