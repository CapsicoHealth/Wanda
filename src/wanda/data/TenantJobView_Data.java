/*
 Tilda V1.0 template application class.
*/

package wanda.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;

/**
This is the application class <B>Data_TenantJobView</B> mapped to the table <B>WANDA.TenantJobView</B>.
@see wanda.data._Tilda.TILDA__TENANTJOBVIEW
*/
public class TenantJobView_Data extends wanda.data._Tilda.TILDA__TENANTJOBVIEW
 {
   protected static final Logger LOG = LogManager.getLogger(TenantJobView_Data.class.getName());

   public TenantJobView_Data() { }

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
