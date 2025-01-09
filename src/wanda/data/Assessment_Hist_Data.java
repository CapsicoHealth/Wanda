/*
 Tilda V2.5 template application class.
*/

package wanda.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;

/**
This is the application class <B>Data_Assessment_Hist</B> mapped to the table <B>WANDA.Assessment_Hist</B>.
@see wanda.data._Tilda.TILDA__ASSESSMENT_HIST
*/
public class Assessment_Hist_Data extends wanda.data._Tilda.TILDA__ASSESSMENT_HIST
 {
   protected static final Logger LOG = LogManager.getLogger(Assessment_Hist_Data.class.getName());

   public Assessment_Hist_Data() { }

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
