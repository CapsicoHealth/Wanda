/*
 Tilda V1.0 template application class.
*/

package wanda.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;

/**
This is the application class <B>Data_TenantJob</B> mapped to the table <B>WANDA.TenantJob</B>.
@see wanda.data._Tilda.TILDA__TENANTJOB
*/
public class TenantJob_Data extends wanda.data._Tilda.TILDA__TENANTJOB
 {
   protected static final Logger LOG = LogManager.getLogger(TenantJob_Data.class.getName());

   public TenantJob_Data() { }

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

 }
