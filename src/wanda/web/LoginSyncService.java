package wanda.web;

import tilda.db.Connection;
import wanda.data.UserDetail_Data;
import wanda.data.UserDetail_Factory;
import wanda.data.User_Data;

public interface LoginSyncService
  {
    public default void syncUser(Connection C, User_Data U)
    throws Exception
      {
        if (U.getUserDetails() == null)
          {
            UserDetail_Data ud = UserDetail_Factory.lookupByUserRefnum(U.getRefnum());
            if (ud.read(C) == false)
              throw new Exception("Cannot load UserDetail for user '" + U.getRefnum() + "' from the database.");
            U.setUserDetail(ud);
          }
        syncUser(U);
      }

    public void syncUser(User_Data U)
    throws Exception;

  }
