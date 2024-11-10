/*
 Tilda V2.5 template application class.
*/

package wanda.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;
import tilda.utils.TextUtil;

/**
This is the application class <B>Data_UserView</B> mapped to the table <B>WANDA.UserView</B>.
@see wanda.data._Tilda.TILDA__USERVIEW
*/
public class UserView_Data extends wanda.data._Tilda.TILDA__USERVIEW
 {
   protected static final Logger LOG = LogManager.getLogger(UserView_Data.class.getName());

   public UserView_Data() { }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//   Implement your customizations, if any, below.
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


   @Override
   public String getNameStandard()
    {
      return TextUtil.standardizeFullName(getNameTitle(), getNameLast(), getNameFirst(), getNameMiddle());
    }

   @Override
   protected boolean afterRead(Connection C) throws Exception
     {
       // Do things after an object has just been read form the data store, for example, take care of AUTO fields.
       return true;
     }

 }
