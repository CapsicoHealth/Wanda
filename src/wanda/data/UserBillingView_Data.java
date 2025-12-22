/*
 Tilda V2.5 template application class.
*/

package wanda.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;

/**
This is the application class <B>Data_UserBillingView</B> mapped to the table <B>WANDA.UserBillingView</B>.
@see wanda.data._Tilda.TILDA__USERBILLINGVIEW
*/
public class UserBillingView_Data extends wanda.data._Tilda.TILDA__USERBILLINGVIEW
 {
   protected static final Logger LOG = LogManager.getLogger(UserBillingView_Data.class.getName());

   public UserBillingView_Data() { }

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
