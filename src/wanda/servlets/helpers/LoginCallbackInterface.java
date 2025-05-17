package wanda.servlets.helpers;

import wanda.data.User_Data;

public interface LoginCallbackInterface
  {
    void onLoginSuccess(User_Data u)
    throws Exception;

    void onLoginFailure(User_Data u)
    throws Exception;

  }
