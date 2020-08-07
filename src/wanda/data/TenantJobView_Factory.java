/*
 Tilda V1.0 template application class.
*/

package wanda.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.*;

/**
 * This is the application class <B>Data_TenantJobView</B> mapped to the table <B>WANDA.TenantJobView</B>.
 * 
 * @see wanda.data._Tilda.TILDA__TENANTJOBVIEW
 */
public class TenantJobView_Factory extends wanda.data._Tilda.TILDA__TENANTJOBVIEW_Factory
  {
    protected static final Logger LOG = LogManager.getLogger(TenantJobView_Factory.class.getName());

    protected TenantJobView_Factory()
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

    public static ListResults<TenantJobView_Data> getAllByTenantRefnum(Connection C, Long TenantRefnum, int Start, int Size)
    throws Exception
      {
        SelectQuery Q = newWhereQuery(C);
        Q.equals(COLS.TENANTREFNUM, TenantRefnum);
        Q.orderBy(COLS.CREATED, false);
        return runSelect(C, Q, Start, Size);
      }

  }
