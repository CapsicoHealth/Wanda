/*
 Tilda V2.5 template application class.
*/

package wanda.data;

import java.time.LocalDate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;
import tilda.db.ListResults;
import tilda.db.SelectQuery;
import tilda.types.Type_StringPrimitive;
import tilda.utils.DateTimeUtil;
import tilda.utils.TextUtil;

/**
 * This is the application class <B>Data_Promo</B> mapped to the table <B>WANDA.Promo</B>.
 * 
 * @see wanda.data._Tilda.TILDA__PROMO
 */
public class Promo_Factory extends wanda.data._Tilda.TILDA__PROMO_Factory
  {
    protected static final Logger LOG = LogManager.getLogger(Promo_Factory.class.getName());

    protected Promo_Factory()
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

    protected static Type_StringPrimitive[] STR_COLS = { COLS.CODE, COLS.DESCR
    };

    public static ListResults<Promo_Data> lookupWhereParams(Connection C, String name, boolean active, boolean current, boolean includeSystem, int start, int size)
    throws Exception
      {
        LocalDate Now = DateTimeUtil.nowLocalDate();
        SelectQuery q = newWhereQuery(C);
        
        // Only show active 
        if (active == true)
         q.and().equals(COLS.ACTIVE, true);
        
        if (current == true)
         q.and().lte(COLS.START, Now)
          .and().openPar().isNull(COLS.END).or().gte(COLS.END, Now).closePar();

        if (TextUtil.isNullOrEmpty(name) == false)
         q.and().like(STR_COLS, "%"+name.trim().toLowerCase().replaceAll("\\W+", "%")+"%", true, false);
        
        if (includeSystem == false)
          q.and().equals(COLS.SYSTEM, false);
        
        return runSelect(C, q, start, size);
      }

  }
