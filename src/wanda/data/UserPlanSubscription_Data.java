/*
 Tilda V2.5 template application class.
*/

package wanda.data;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;

/**
This is the application class <B>Data_UserPlanSubscription</B> mapped to the table <B>WANDA.UserPlanSubscription</B>.
@see wanda.data._Tilda.TILDA__USERPLANSUBSCRIPTION
*/
public class UserPlanSubscription_Data extends wanda.data._Tilda.TILDA__USERPLANSUBSCRIPTION
 {
   protected static final Logger LOG = LogManager.getLogger(UserPlanSubscription_Data.class.getName());

   public UserPlanSubscription_Data() { }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//   Implement your customizations, if any, below.
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


   @Override
   protected boolean beforeWrite(Connection C) throws Exception
     {
       // Do things before writing the object to disk, for example, take care of AUTO fields.
       return true;
     }

   @Override
   protected boolean afterRead(Connection C) throws Exception
     {
       // Do things after an object has just been read form the data store, for example, take care of AUTO fields.
       return true;
     }

   public LocalDate getExpiryDtFrom(LocalDate orderDt)
    {
      if (isCycleMonthly() == true)
        return orderDt.plus(1, ChronoUnit.MONTHS);
      else if (isCycleYearly() == true)
        return orderDt.plus(1, ChronoUnit.YEARS);

      return null;
    }
   
 }
