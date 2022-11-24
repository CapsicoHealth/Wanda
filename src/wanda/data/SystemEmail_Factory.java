/*
 Tilda V1.0 template application class.
*/

package wanda.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;
import tilda.db.ListResults;
import tilda.db.SelectQuery;
import tilda.utils.SystemValues;

/**
This is the application class <B>Data_SYSTEMEMAIL</B> mapped to the table <B>ADMIN.SYSTEMEMAIL</B>.
@see wanda.data._Tilda.TILDA__SYSTEMEMAIL
*/
public class SystemEmail_Factory extends wanda.data._Tilda.TILDA__SYSTEMEMAIL_Factory
 {
   protected static final Logger LOG = LogManager.getLogger(SystemEmail_Factory.class.getName());

   protected SystemEmail_Factory() { }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//   Implement your customizations, if any, below.
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   public static ListResults<SystemEmail_Data> getAll(Connection C, User_Data U, long tenantRefnum,int Start, int Size)
   throws Exception
     {
       SelectQuery Q = newWhereQuery(C);
       Q.equals(COLS.USERREFNUM, U.getRefnum());
       if(tenantRefnum != SystemValues.EVIL_VALUE)
         {
           Q.and();
           Q.equals(COLS.TENANTREFNUM, tenantRefnum);
         }
       return runSelect(C, Q, Start, Size);
     }

   
   public static void init(Connection C) throws Exception
    {
      // Add logic to initialize your object, for example, caching some values, or validating some things.
    }

 }
