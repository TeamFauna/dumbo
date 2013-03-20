package com.fawna.dumbo;

import org.json.JSONObject;

public class MovieInfo 
{
  public String imdb, name;
  public long time;
  
  public MovieInfo(JSONObject resp) {
    try {
      JSONObject offset = resp.getJSONObject("offset");
      time = (int)((offset.getDouble("time") + 30) * 1000);

      JSONObject metadata = resp.getJSONObject("metadata");
      name = metadata.getString("name");
      imdb = metadata.getString("imdb_url");

      //TODO parse the roles

    }
    catch (Exception e) { 
      throw new RuntimeException(e);
    }
  }

}
