/* ===========================================================================
 * Copyright (C) 2017 CapsicoHealth Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package wanda.data;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;
import tilda.utils.CollectionUtil;
import tilda.utils.DateTimeUtil;
import tilda.utils.EncryptionUtil;
import tilda.utils.TextUtil;
import tilda.utils.pairs.StringStringPair;
import wanda.servlets.helpers.RoleHelper;
import wanda.web.SystemMailSender;
import wanda.web.config.WebBasics;
import wanda.web.exceptions.BadRequestException;
import wanda.web.exceptions.ResourceNotAuthorizedException;


/**
 * This is the application class <B>Data_USER</B> mapped to the table <B>WANDA.USER</B>.
 * 
 * @see wanda.data._Tilda.TILDA__USER
 */
public class User_Data extends wanda.data._Tilda.TILDA__USER
  {
    protected static final Logger LOG = LogManager.getLogger(User_Data.class.getName());

    public User_Data()
      {
      }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Implement your customizations, if any, below.
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void setEmail(String i)
    throws Exception
      {
        super.setEmail(i == null ? null : i.toLowerCase());
      }

    @Override
    protected boolean beforeWrite(Connection C)
    throws Exception
      {
        // Do things before writing the object to disk, for example, take care of AUTO fields.
        return true;
      }

    @Override
    protected boolean afterRead(Connection C)
    throws Exception
      {
        // Do things after an object has just been read form the data store, for example, take care of AUTO fields.
        return true;
      }

    protected UserDetail_Data _UD;

    public void setUserDetail(UserDetail_Data ud)
      {
        _UD = ud;
      }

    public UserDetail_Data getUserDetails()
      {
        return _UD;
      }

    public void pushToPswdHistory(String hashedPassword)
    throws Exception
      {
        ArrayList<String> pswdHistory = new ArrayList<>();
        if (getPswdHistAsArray() != null)
          {
            pswdHistory.addAll(Arrays.asList(getPswdHistAsArray()));
          }
        pswdHistory.add(hashedPassword);
        int difference = pswdHistory.size() - WebBasics.getMaxPswdHistory();
        for (int i = 0; i < difference; i++)
          {
            pswdHistory.remove(0);
          }
        setPswdHist(pswdHistory);
      }

    public boolean hasRoles(Iterator<String> I)
      {
        while (I.hasNext() == true)
          if (hasRoles(I.next()) == true)
            return true;
        return false;
      }

    public boolean hasRoles(String[] roles)
      {
        return hasRoles(Arrays.asList(roles).iterator());
      }

    public void sendForgotPswdEmail(Connection C)
    throws Exception
      {
        setPswdResetCode(EncryptionUtil.getToken(6, true));
        setPswdResetCreateNow();
        write(C);
        String[] to = { getEmail()
        }, cc = {}, bcc = {};
        new Thread()
          {
            @Override
            public void run()
              {
                super.run();
                StringBuilder sb = new StringBuilder();
                List<String> copyTexts = WebBasics.getResetEmailTexts();
                if (copyTexts != null)
                  {
                    Iterator<String> i = copyTexts.listIterator();
                    while (i.hasNext())
                      {
                        sb.append(i.next());
                      }
                  }
                sb.append("<p><a href='");
                sb.append(WebBasics.getHostName());
                sb.append(WebBasics.getAppPath());
                sb.append(WebBasics.getHomePagePath());
                sb.append("?action=setPswd");
                sb.append("&token=");
                sb.append(getPswdResetCode());
                sb.append("'>Click to reset password</a></p>");
                sb.append("or <p> use code: <b>" + getPswdResetCode() + "</b> to reset your password.</p>");
                SystemMailSender.sendMail(to, cc, bcc, "Reset your password --" + WebBasics.getAppName(), sb.toString(), true, true);
              }
          }.start();
      }

    public static void inviteUser(Connection C, String email, String firstName, String lastName, String[] roles, long[] tenantRefnums, long[] appRefnums)
    throws Exception
      {
        List<StringStringPair> Errors = new ArrayList<StringStringPair>();

        String password = EncryptionUtil.getToken(16, true);
        HashSet<String> _roles = new HashSet<String>(Arrays.asList(roles));
        User_Data U = User_Factory.create(email, email, _roles, password);
        U.setPswdResetCode(EncryptionUtil.getToken(16, true));
        U.setPswdResetCreateNow();
        U.setInvitedUser(true);
        U.setInviteCancelledNull();
        U.setLockedNull();
        if (U.write(C) == false)
         Errors.add(new StringStringPair("User", "Unable to save changes"));

        UserDetail_Data UD = UserDetail_Factory.create(U.getRefnum(), lastName, firstName);
        UD.setEmailHome(email);
        if (UD.write(C) == false)
         Errors.add(new StringStringPair("UserDetail", "Unable to save changes"));

        if (tenantRefnums != null)
          {
            for (long tenantRefnum : tenantRefnums)
              {
                TenantUser_Data tenantUser = TenantUser_Factory.create(U.getRefnum(), tenantRefnum);
                if (tenantUser.write(C) == false)
                  {
                    Errors.add(new StringStringPair("TenantUser " + tenantRefnum, "Unable to save changes"));
                  }
              }
          }
        if (appRefnums != null)
          {
            for (long appRefnum : appRefnums)
              {
                AppUser_Data appUser = AppUser_Factory.create(appRefnum);
                appUser.setUserRefnum(U.getRefnum());
                if (appUser.write(C) == false)
                  {
                    Errors.add(new StringStringPair("TenantUser " + appRefnum, "Unable to save changes"));
                  }
              }
          }
        if (Errors.isEmpty() == false)
          {
            throw new BadRequestException(Errors);
          }
        U.sendInviteEmail();
      }

    public static void updateDetailsAndInvite(Connection C, User_Data U, String email,
    String firstName, String lastName, String[] roles, long[] appRefnums, List<Long> tenantRefnumList,
    long[] oldTenantRefnums)
    throws Exception
      {
        List<StringStringPair> Errors = new ArrayList<StringStringPair>();
        // Do not send invite if tenantRefnums/Roles/Name are change and loginCount > 0
        boolean isResetPassword = U.getLoginCount() == 0;
        UserDetail_Data UD = UserDetail_Factory.lookupByUserRefnum(U.getRefnum());
        if (UD.read(C) == false)
          {
            UD = UserDetail_Factory.create(U.getRefnum(), lastName, firstName);
          }
        else
          {
            UD.setNameLast(lastName);
            UD.setNameFirst(firstName);
          }
        U.setEmail(email);
        U.setRoles(new HashSet<String>(Arrays.asList(roles)));
        if (isResetPassword)
          {
            U.setPswdResetCode(EncryptionUtil.getToken(16, true));
            U.setPswdResetCreateNow();
            U.setInvitedUser(true);
          }
        U.setInviteCancelledNull();
        U.setLockedNull();

        if (U.write(C) == false || UD.write(C) == false)
          {
            Errors.add(new StringStringPair("User", "Unable to save changes"));
          }

        if (tenantRefnumList != null)
          {
            for (long tenantRefnum : tenantRefnumList)
              {
                TenantUser_Data tenantUser = TenantUser_Factory.LookUpByTenantRefnum(C, tenantRefnum, U.getRefnum());
                if (tenantUser == null)
                  {
                    // Create Tenant User
                    tenantUser = TenantUser_Factory.create(U.getRefnum(), tenantRefnum);
                  }
                else if (tenantUser.getActive() == false)// TODO change schema for better naming
                  {
                    tenantUser.setActive(true);// TODO change schema for better naming
                  }
                if (tenantUser.write(C) == false)
                  {
                    Errors.add(new StringStringPair("TenantUser " + tenantRefnum, "Unable to save changes"));
                  }
              }
            for (long oldTenant : oldTenantRefnums)
              {
                if (tenantRefnumList.indexOf(oldTenant) == -1)
                  {
                    TenantUser_Data tenantUser = TenantUser_Factory.LookUpByTenantRefnum(C, oldTenant, U.getRefnum());
                    if (tenantUser != null)
                      {
                        tenantUser.setActive(false);
                        if (tenantUser.write(C) == false)
                          Errors.add(new StringStringPair("TenantUser " + oldTenant, "Unable to save changes"));
                      }
                  }
              }

            if (appRefnums != null)
              {
                AppUser_Factory.deleteUserApps(C, U.getRefnum());
                List<AppUser_Data> L = new ArrayList<AppUser_Data>();
                for (long appRefnum : appRefnums)
                  {
                    AppUser_Data appUser = AppUser_Factory.create(appRefnum);
                    appUser.setUserRefnum(U.getRefnum());
                    L.add(appUser);
                  }
                if (AppUser_Factory.writeBatch(C, L, 100, 500) != -1)
                  throw new Exception("Cannot update user application list");
              }
          }
        if (Errors.isEmpty() == false)
          {
            throw new BadRequestException(Errors);
          }
        if (isResetPassword)
          {
            U.sendInviteEmail();
          }
      }

    public void sendInviteEmail()
      {
        String[] to = { getEmail()
        }, cc = {}, bcc = {};
        new Thread()
          {
            @Override
            public void run()
              {
                super.run();
                StringBuilder sb = new StringBuilder();
                List<String> copyTexts = WebBasics.getInviteUserTexts();
                if (copyTexts != null)
                  {
                    Iterator<String> i = copyTexts.listIterator();
                    while (i.hasNext())
                      {
                        sb.append(i.next());
                      }
                  }
                sb.append("<p><a href='");
                sb.append(WebBasics.getHostName());
                sb.append(WebBasics.getAppPath());
                sb.append(WebBasics.getHomePagePath());
                sb.append("?action=signUp");
                sb.append("&token=");
                sb.append(getPswdResetCode());
                sb.append("'>Click here to set your password</a></p>");
                SystemMailSender.sendMail(to, cc, bcc, "Set Password: Invited to " + WebBasics.getAppName(), sb.toString(), true, true);
              }
          }.start();
      }

    public void CopyToWithRefnum(User_Data Dst)
    throws Exception
      {
        Dst.setRefnum(Dst.getRefnum());
        super.copyTo(Dst);
      }

    public static User_Data cloneWithCreateMode(User_Data Dst)
    throws Exception
      {
        String email = Dst.getEmail();
        String id = Dst.getId();
        String pswd = Dst.getPswd();
        Iterator<String> roles = Dst.getRoles();
        HashSet<String> user_roles = new HashSet<String>();
        while (roles.hasNext())
          {
            user_roles.add(roles.next());
          }
        User_Data NewObject = User_Factory.create(email, id, user_roles, pswd);
        Dst.copyTo(NewObject);
        NewObject.setRefnum(Dst.getRefnum());
        return NewObject;
      }

    public void updateEmail(Connection C, String newEmail)
    throws Exception
      {
        if (TextUtil.isNullOrEmpty(newEmail) == false)
          {

            if (newEmail.equalsIgnoreCase(getEmail()) == false)
              {

                User_Data emailUser = User_Factory.lookupByEmail(newEmail);
                if (emailUser.read(C))
                  throw new BadRequestException("email", "User already exists with email '" + newEmail + "'");

                setEmailUnverified(newEmail);
                setEmailVerificationCode(EncryptionUtil.getToken(16, true));
                sendVerificationEmail(newEmail);
                write(C);
              }
          }
      }

    public void sendVerificationEmail(String email)
      {
        String[] to = { email
        }, cc = {}, bcc = {};
        new Thread()
          {
            @Override
            public void run()
              {
                super.run();
                StringBuilder sb = new StringBuilder();
                List<String> copyTexts = WebBasics.getEmailVerificationTexts();
                if (copyTexts != null)
                  {
                    Iterator<String> i = copyTexts.listIterator();
                    while (i.hasNext())
                      {
                        sb.append(i.next());
                      }
                  }
                sb.append("<p><a href='");
                sb.append(WebBasics.getHostName());
                sb.append(WebBasics.getAppPath());
                sb.append(WebBasics.getHomePagePath());
                sb.append("?action=emailVerification");
                sb.append("&token=");
                sb.append(getEmailVerificationCode());
                sb.append("'>Click Here to complete verification</a></p>");
                SystemMailSender.sendMail(to, cc, bcc, "Email Verification: " + WebBasics.getAppName(), sb.toString(), true, true);
              }
          }.start();
      }

    public static void markUserLoginFailure(Connection C, User_Data U)
    throws Exception
      {
        ZonedDateTime firstFailure = U.getFailFirst();
        if (isUserLocked(U))
          {
            // if already locked raise not found exception
            throw new ResourceNotAuthorizedException("User", U.getEmail());
          }

        if (firstFailure == null)
          {
            U.setFailCount(1);
            U.setFailFirstNow();
          }
        else
          {
            long minutes = firstFailure.until(ZonedDateTime.now(), ChronoUnit.MINUTES);
            if (minutes <= WebBasics.getWithinTime())
              {
                U.setFailCount(U.getFailCount() + 1);
                if (U.getFailCount() >= WebBasics.getLoginAttempts())
                  {
                    U.setFailCycleCount(U.getFailCycleCount() + 1);
                    if (U.getFailCycleCount() >= WebBasics.getLoginFailedCycle())
                      {
                        U.setLocked(DateTimeUtil.nowUTC().plusDays(WebBasics.getLockForever()));
                      }
                    else
                      {
                        U.setLocked(DateTimeUtil.nowUTC().plusMinutes(WebBasics.getLockFor()));
                      }
                    U.setFailFirstNull();
                  }
              }
            else
              {
                U.setFailCount(1);
                U.setFailFirstNow();
              }
          }
        U.write(C);
      }

    public boolean generateAppData(Connection C)
      {
        String dbNameStr = EncryptionUtil.getToken(10, true);
        String dbKeyStr = EncryptionUtil.getToken(20, true);
        byte[] shaBytes = EncryptionUtil.hash256(dbKeyStr);
        String shaHexStr = EncryptionUtil.bytesToHex(shaBytes);

        User_Data.AppData appData = new User_Data.AppData();
        appData._dbName = dbNameStr;
        appData._dbKey = shaHexStr;

        try
          {
            this.setAppData(CollectionUtil.toList(new User_Data.AppData[] { appData
            }));
            write(C);
          }
        catch (Exception e)
          {
            LOG.error("Failed to generate AppData for User. ref = ", this.getRefnum() + "\n", e);
            return false;
          }
        return true;
      }

    public String[] getAppDataJson(String aesKeyStr)
      {
        String[] resp = new String[2];
        resp[0] = this.getAppData().get(0)._dbName;
        resp[1] = EncryptionUtil.aes(this.getAppData().get(0)._dbKey, aesKeyStr);
        return resp;
      }

    public boolean isLocked()
      {
        return getLocked() != null && ChronoUnit.MILLIS.between(ZonedDateTime.now(), getLocked()) > 0;
      }
    
    public static boolean isUserLocked(User_Data U)
      {
        return U.isLocked();
      }

    public boolean isSuperAdmin()
      {
        return hasRoles(RoleHelper.SUPERADMIN);
      }

    public static boolean isUserSuperAdmin(User_Data U)
      {
        return U.isSuperAdmin();
      }
  }
