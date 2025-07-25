/*
 Tilda V2.5 template application class.
*/

package wanda.data;

import java.time.LocalDate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.threeten.bp.ZonedDateTime;

import tilda.db.Connection;
import tilda.utils.DateTimeUtil;

/**
This is the application class <B>Data_Promo</B> mapped to the table <B>WANDA.Promo</B>.
@see wanda.data._Tilda.TILDA__PROMO
*/
public class Promo_Data extends wanda.data._Tilda.TILDA__PROMO
 {
   protected static final Logger LOG = LogManager.getLogger(Promo_Data.class.getName());

   public Promo_Data() { }

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

  /**
   * To be valid, a promo code must be active, and the start date must be in the past, and the end date is either null or in the future.
   * @return
   */
  public boolean isActiveAndValid()
    {
      LocalDate now = DateTimeUtil.nowLocalDate();
      return getActive() == true && now.compareTo(getStart()) >= 0 && (isNullEnd() == true || now.compareTo(getEnd()) <= 0);
    }

 }
