package wanda.web;

import tilda.db.Connection;
import wanda.data.UserView_Data;
import wanda.data.UserView_Factory;
import wanda.data.User_Data;

public interface LoginSyncService
  {
    default void syncUser(Connection C, User_Data U)
    throws Exception
      {
        UserView_Data u = UserView_Factory.lookupByRefnum(U.getRefnum());
        if (u.read(C) == false)
          throw new Exception("Cannot lookup UserView by refnum " + U.getRefnum());
        syncUser(C, u);
      }

    public void syncUser(Connection C, UserView_Data U) throws Exception;

  }
