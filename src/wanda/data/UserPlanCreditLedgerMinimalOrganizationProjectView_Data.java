/*
 Tilda V2.5 template application class.
*/

package wanda.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;

/**
This is the application class <B>Data_UserPlanCreditLedgerMinimalOrganizationProjectView</B> mapped to the table <B>WANDA.UserPlanCreditLedgerMinimalOrganizationProjectView</B>.
@see wanda.data._Tilda.TILDA__USERPLANCREDITLEDGERMINIMALORGANIZATIONPROJECTVIEW
*/
public class UserPlanCreditLedgerMinimalOrganizationProjectView_Data extends wanda.data._Tilda.TILDA__USERPLANCREDITLEDGERMINIMALORGANIZATIONPROJECTVIEW
 {
   protected static final Logger LOG = LogManager.getLogger(UserPlanCreditLedgerMinimalOrganizationProjectView_Data.class.getName());

   public UserPlanCreditLedgerMinimalOrganizationProjectView_Data() { }

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
