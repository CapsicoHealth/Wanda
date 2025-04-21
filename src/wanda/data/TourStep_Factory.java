/*
 Tilda V2.5 template application class.
*/

package wanda.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.*;

/**
 * This is the application class <B>Data_TourStep</B> mapped to the table <B>WANDA.TourStep</B>.
 * 
 * @see wanda.data._Tilda.TILDA__TOURSTEP
 */
public class TourStep_Factory extends wanda.data._Tilda.TILDA__TOURSTEP_Factory
  {
    protected static final Logger LOG = LogManager.getLogger(TourStep_Factory.class.getName());

    protected TourStep_Factory()
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


    /**
     * Shifts all the step positions for the steps for the specified tour part by +1000. This method is to be used in concert
     * with cleanOldEntries to clean out all left-over steps with pos >= 1000
     * @param C
     * @param tourPartRefnum
     * @return
     * @throws Exception
     */
    public static int shiftOutSteps(Connection C, long tourPartRefnum)
    throws Exception
      {
        return C.executeUpdate(SCHEMA_LABEL, TABLENAME_LABEL, "UPDATE " + SCHEMA_TABLENAME_LABEL + " SET \"pos\"=1000+\"pos\" WHERE \"tourPartRefnum\"=" + tourPartRefnum);
      }

    /**
     * Deletes steps in the specified tour part with a pos >= 1000 (as shifted previously by shiftOutSteps.
     * @param C
     * @param tourPartRefnum
     * @return
     * @throws Exception
     */
    public static int cleanOldSteps(Connection C, long tourPartRefnum)
    throws Exception
      {
        return C.executeDelete(SCHEMA_LABEL, TABLENAME_LABEL, "DELETE FROM " + SCHEMA_TABLENAME_LABEL + " WHERE \"pos\">=1000 AND \"tourPartRefnum\"=" + tourPartRefnum);
      }

  }
