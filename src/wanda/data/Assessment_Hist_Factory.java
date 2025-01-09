/*
 Tilda V2.5 template application class.
*/

package wanda.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.*;

/**
This is the application class <B>Data_Assessment_Hist</B> mapped to the table <B>WANDA.Assessment_Hist</B>.
@see wanda.data._Tilda.TILDA__ASSESSMENT_HIST
*/
public class Assessment_Hist_Factory extends wanda.data._Tilda.TILDA__ASSESSMENT_HIST_Factory
 {
   protected static final Logger LOG = LogManager.getLogger(Assessment_Hist_Factory.class.getName());

   protected Assessment_Hist_Factory() { }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//   Implement your customizations, if any, below.
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


   public static void init(Connection C) throws Exception
    {
      // Add logic to initialize your object, for example, caching some values, or validating some things.
    }

 }
