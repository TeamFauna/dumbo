package com.fawna.dumbo;

import org.json.JSONObject;

public class MovieStub 
{
  public int id;
  public String name;
  public String imdb;
  
  public MovieStub(JSONObject movie) {
    try {
      id = movie.getInt("id");
      name = movie.getString("name");
      imdb = movie.getString("imdb_url");
    }
    catch (Exception e) { 
      throw new RuntimeException(e);
    }
  }

}
