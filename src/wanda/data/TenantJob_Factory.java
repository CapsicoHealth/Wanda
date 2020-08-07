/*
 Tilda V1.0 template application class.
*/

package wanda.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.*;

/**
 * This is the application class <B>Data_TenantJob</B> mapped to the table <B>WANDA.TenantJob</B>.
 * 
 * @see wanda.data._Tilda.TILDA__TENANTJOB
 */
public class TenantJob_Factory extends wanda.data._Tilda.TILDA__TENANTJOB_Factory
  {
    protected static final Logger LOG = LogManager.getLogger(TenantJob_Factory.class.getName());

    protected TenantJob_Factory()
      {
      }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Implement your customizations, if any, below.
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    public static void init(Connection C)
    throws Exception
      {
        // Add logic to initialize your object, for example, caching some values, or validating some things.
      }


    public static boolean hasPendingJobs(Connection C, Tenant_Data T, User_Data U)
    throws Exception
      {
        // Needs to be filled in!
        return false;
      }
  }
