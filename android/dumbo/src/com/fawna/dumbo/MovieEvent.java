package com.fawna.dumbo;

import android.util.Log;
import org.json.JSONObject;

public class MovieEvent 
{
  public long time;
  public String type;
  public String text;
  public String role_name;
  public String role_imdb;
  public String actor_name;
  public String actor_imdb;
  public String actor_picture;

  public static String TYPE_PLOT = "PLOT";
  public static String TYPE_ACTOR = "ROLE";

  
  public MovieEvent(JSONObject event) {
    try {
      time = event.getLong("time_stamp");
      type = event.getString("type");
      text = event.getString("text");

      if (type.equals("ROLE")) {
        JSONObject role = event.getJSONObject("role");
        role_name = role.getString("name");
        role_imdb = role.getString("imdb_url");

        JSONObject actor = event.getJSONObject("actor");
        actor_name = actor.getString("name");
        actor_imdb = actor.getString("imdb_url");
        actor_picture = actor.getString("picture_url");
      }
    }
    catch (Exception e) { 
      throw new RuntimeException(e);
    }
  }

}
