/*
 Tilda V2.5 template application class.
*/

package wanda.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.*;

/**
This is the application class <B>Data_Project</B> mapped to the table <B>WANDA.Project</B>.
@see wanda.data._Tilda.TILDA__PROJECT
*/
public class Project_Factory extends wanda.data._Tilda.TILDA__PROJECT_Factory
 {
   protected static final Logger LOG = LogManager.getLogger(Project_Factory.class.getName());

   protected Project_Factory() { }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//   Implement your customizations, if any, below.
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


   public static void init(Connection C) throws Exception
    {
      // Add logic to initialize your object, for example, caching some values, or validating some things.
    }

 }
