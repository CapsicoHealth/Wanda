/*
 Tilda V2.3 template application class.
*/

package wanda.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.*;

/**
This is the application class <B>Data_GeoData</B> mapped to the table <B>WANDA.GeoData</B>.
@see wanda.data._Tilda.TILDA__GEODATA
*/
public class GeoData_Factory extends wanda.data._Tilda.TILDA__GEODATA_Factory
 {
   protected static final Logger LOG = LogManager.getLogger(GeoData_Factory.class.getName());

   protected GeoData_Factory() { }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//   Implement your customizations, if any, below.
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


   public static void init(Connection C) throws Exception
    {
      // Add logic to initialize your object, for example, caching some values, or validating some things.
    }

 }
