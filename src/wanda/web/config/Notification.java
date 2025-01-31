package wanda.web.config;

import com.google.gson.annotations.SerializedName;

public class Notification
  {
    /*@formatter:off*/
    @SerializedName("accounts"       ) public String[] _accounts        = new String[] { }  ;
    @SerializedName("scheduleMinutes") public int      _scheduleMinutes = 240  ;
    @SerializedName("alertMinutes"   ) public int      _alertMinutes    = 480  ;
    /*@formatter:on*/
  }
