/*
 Tilda V2.5 template application class.
*/

package wanda.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.*;

/**
This is the application class <B>Data_AccessLogSessionView</B> mapped to the table <B>WANDA.AccessLogSessionView</B>.
@see wanda.data._Tilda.TILDA__ACCESSLOGSESSIONVIEW
*/
public class AccessLogSessionView_Factory extends wanda.data._Tilda.TILDA__ACCESSLOGSESSIONVIEW_Factory
 {
   protected static final Logger LOG = LogManager.getLogger(AccessLogSessionView_Factory.class.getName());

   protected AccessLogSessionView_Factory() { }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//   Implement your customizations, if any, below.
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


   public static void init(Connection C) throws Exception
    {
      // Add logic to initialize your object, for example, caching some values, or validating some things.
    }

 }
