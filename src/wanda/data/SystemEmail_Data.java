/*
 Tilda V1.0 template application class.
*/

package wanda.data;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;
import tilda.db.ListResults;
import wanda.web.EMailSender;
import wanda.web.config.WebBasics;

/**
This is the application class <B>Data_SYSTEMEMAIL</B> mapped to the table <B>ADMIN.SYSTEMEMAIL</B>.
@see wanda.data._Tilda.TILDA__SYSTEMEMAIL
*/
public class SystemEmail_Data extends wanda.data._Tilda.TILDA__SYSTEMEMAIL
 {
   protected static final Logger LOG = LogManager.getLogger(SystemEmail_Data.class.getName());

   public SystemEmail_Data() { }

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
   
   // TODO move to thread
   public void deliver(User_Data User, Connection C) throws Exception
     {
       int Start = 0;
       int Size = WebBasics.getEmailSettingsSys()._maxCount; // Max Limit for To is 5000. https://technet.microsoft.com/en-in/library/exchange-online-limits.aspx#RecipientLimits
       List<String> BCC = new ArrayList<String>();
       String []BCC_Arr = null;
       String []To_Arr = {User.getEmail()};
       String []CC = {};
       boolean status = false;
       // Hack here about testing for null tenant refnum.
       // Tilda does not set the null flag when a new object is created.
       if(isNullTenantRefnum() == true || getTenantRefnum() == 0)
         {
           ListResults<AdminUsersView_Data> Results = AdminUsersView_Factory.filter(C, User, null, null, "active", null, Start, Size);
           while(Results.size() > 0)
             {
               for(AdminUsersView_Data U : Results)
                 {
                   if(User.getRefnum() != U.getPersonRefnum())
                     {
                       BCC.add(U.getPersonId());
                     }
                 }
               BCC_Arr = new String[BCC.size()];
               BCC.toArray(BCC_Arr);
               BCC.clear();
               status = EMailSender.sendMailSys(To_Arr, CC, BCC_Arr, getSubject(), getBody(), true, true);
               if(status == false)
                 break;
               Start = Size + 1;
               Results = AdminUsersView_Factory.filter(C, User, null, null, "active", null, Start, Size);
             }
         }
       else
         {
           ListResults<AdminUsersAndTenantsView_Data> Results = AdminUsersAndTenantsView_Factory.filter(C, User, null, null, getTenantRefnum(), "active", null, Start, Size);
           while(Results.size() > 0)
             {
               for(AdminUsersAndTenantsView_Data U : Results)
                 {
                   if(User.getRefnum() != U.getPersonRefnum())
                     {
                       BCC.add(U.getPersonId());
                     }
                 }
               BCC_Arr = new String[BCC.size()];
               BCC.toArray(BCC_Arr);
               BCC.clear();
               status = EMailSender.sendMailSys(To_Arr, CC, BCC_Arr, getSubject(), getBody(), true, true);
               if(status == false)
                 break;
               Start = Size + 1;
               Results = AdminUsersAndTenantsView_Factory.filter(C, User, null, null, getTenantRefnum(), "active", null, Start, Size);
             }
         }
         if(status)
           {
             this.setStatusDelivered();
           }
         else
           {
             this.setStatusFailed();
           }
         this.write(C);
     }
 }
