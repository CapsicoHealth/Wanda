/*
 Tilda V2.5 template application class.
*/

package wanda.data;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.annotations.SerializedName;

import tilda.db.Connection;

/**
This is the application class <B>Data_TourPart</B> mapped to the table <B>WANDA.TourPart</B>.
@see wanda.data._Tilda.TILDA__TOURPART
*/
public class TourPart_Data extends wanda.data._Tilda.TILDA__TOURPART
 {
   protected static final Logger LOG = LogManager.getLogger(TourPart_Data.class.getName());

   public TourPart_Data() { }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//   Implement your customizations, if any, below.
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   
    /*@formatter:off*/
    @SerializedName("steps" ) public List<TourStep_Data> _steps = new ArrayList<TourStep_Data>();
    /*@formatter:on*/


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
