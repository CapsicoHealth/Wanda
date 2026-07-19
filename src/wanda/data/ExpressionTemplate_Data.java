/*
 Tilda V2.5 template application class.
*/

package wanda.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;

/**
This is the application class <B>Data_ExpressionTemplate</B> mapped to the table <B>WANDA.ExpressionTemplate</B>.
@see wanda.data._Tilda.TILDA__EXPRESSIONTEMPLATE
*/
public class ExpressionTemplate_Data extends wanda.data._Tilda.TILDA__EXPRESSIONTEMPLATE
 {
   protected static final Logger LOG = LogManager.getLogger(ExpressionTemplate_Data.class.getName());

   public ExpressionTemplate_Data() { }

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
