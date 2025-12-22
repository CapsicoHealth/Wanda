/*
 Tilda V2.5 template application class.
*/

package wanda.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.*;

/**
This is the application class <B>Data_UserPlanPreOrder</B> mapped to the table <B>WANDA.UserPlanPreOrder</B>.
@see wanda.data._Tilda.TILDA__USERPLANPREORDER
*/
public class UserPlanPreOrder_Factory extends wanda.data._Tilda.TILDA__USERPLANPREORDER_Factory
 {
   protected static final Logger LOG = LogManager.getLogger(UserPlanPreOrder_Factory.class.getName());

   protected UserPlanPreOrder_Factory() { }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//   Implement your customizations, if any, below.
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


   public static void init(Connection C) throws Exception
    {
      // Add logic to initialize your object, for example, caching some values, or validating some things.
    }

   public static int delete(Connection C, long userRefnum) throws Exception
    {
      DeleteQuery Q = newDeleteQuery(C);
      Q.where().equals(COLS.USERREFNUM, userRefnum);
      
      return Q.execute();
    }

 }
