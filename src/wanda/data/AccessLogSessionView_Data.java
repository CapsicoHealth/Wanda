/*
 Tilda V2.5 template application class.
*/

package wanda.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;

/**
This is the application class <B>Data_AccessLogSessionView</B> mapped to the table <B>WANDA.AccessLogSessionView</B>.
@see wanda.data._Tilda.TILDA__ACCESSLOGSESSIONVIEW
*/
public class AccessLogSessionView_Data extends wanda.data._Tilda.TILDA__ACCESSLOGSESSIONVIEW
 {
   protected static final Logger LOG = LogManager.getLogger(AccessLogSessionView_Data.class.getName());

   public AccessLogSessionView_Data() { }

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
