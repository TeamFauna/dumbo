package com.fawna.dumbo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.entity.StringEntity;
import org.json.JSONObject;

import android.app.Activity;
import fauna.dumbo.Fingerprinter;
import fauna.dumbo.Fingerprinter.AudioFingerprinterListener;
import android.util.Log;

public class FingerprintListener implements AudioFingerprinterListener
{
  boolean recording, resolved;
  Fingerprinter fingerprinter;
  FingerprintShowActivity act;
  boolean DEBUG = false;

  public FingerprintListener(FingerprintShowActivity acti) {
    act = acti;
  }

  public void startFingerprinting() { 
    Log.d("Fingerprinter","STARTING");
    if (!DEBUG) { 
      if(recording)
      {
        fingerprinter.stop();             
      }
      else
      {               
        if(fingerprinter == null)
          fingerprinter = new Fingerprinter(FingerprintListener.this);
        fingerprinter.fingerprint(15);
      }
    }
    else { 
      Thread getOne = new Thread() {
      @Override
      public void run() { 
        try { 
          //Get a list of movies to use as a substitute
          String urlstr = "http://www.willhughes.ca/echo/get";      
          HttpClient client = new DefaultHttpClient();
          HttpPost post = new HttpPost(urlstr);

          JSONObject requestJson = new JSONObject();

          requestJson.put("id", 2);
          StringEntity se = new StringEntity(requestJson.toString());
          post.setEntity(se);
          post.setHeader("Accept", "application/json");
          post.setHeader("Content-type", "application/json");
          // get response
          HttpResponse response = client.execute(post);                
          // Get hold of the response entity
          HttpEntity entity = response.getEntity();
          // If the response does not enclose an entity, there is no need
          // to worry about connection release

          String result = "";
          if (entity != null) 
          {
              // A Simple JSON Response Read
              InputStream instream = entity.getContent();
              result= convertStreamToString(instream);
              // now you have the string representation of the HTML request
              instream.close();
          }
          Log.d("Fingerprinter", "Result: " + result);
          JSONObject jobj = new JSONObject(result);

          if (jobj.getBoolean("success")) {
            JSONObject match = jobj.getJSONObject("match");
            final MovieInfo mov = new MovieInfo(match);
            Activity activity = (Activity) act;
            activity.runOnUiThread(new Runnable() 
            {   
              public void run() 
              {
                act.didFindMatchForCode(mov);
              }
            });
          }
        }
        catch (Exception e) { 
          Log.d("Fingerprinter", "Exception: " + e);
          throw new RuntimeException(e);
        }
      }
      };
      getOne.start();
    }
  }

  public void didFinishListening() 
  {         
    recording = false;
  }
  
  public void didFinishListeningPass()
  {}

  public void willStartListening() 
  {
    recording = true;
    resolved = false;
  }

  public void willStartListeningPass() 
  {}

  public void didGenerateFingerprintCode(String code) 
  {
  }

  public void didFindMatchForCode(final JSONObject table, String code) {
    final MovieInfo mi = new MovieInfo(table);
    //Return MovieInfo
    Activity activity = (Activity) act;
    activity.runOnUiThread(new Runnable() {
      public void run() 
      {
        act.didFindMatchForCode(mi);
      }
    });
    resolved = true;
  }

  public void didNotFindMatchForCode(String code) 
  {
    resolved = true;
    
 /*   //Get a list of movies to use as a substitute
    String urlstr = "http://www.willhughes.ca/echo/query";      
    HttpClient client = new DefaultHttpClient();
    HttpGet get = new HttpGet(urlstr);

    // get response
    HttpResponse response = client.execute(get);                
    // Get hold of the response entity
    HttpEntity entity = response.getEntity();
    // If the response does not enclose an entity, there is no need
    // to worry about connection release

    String result = "";
    if (entity != null) 
    {
        // A Simple JSON Response Read
        InputStream instream = entity.getContent();
        result= convertStreamToString(instream);
        // now you have the string representation of the HTML request
        instream.close();
    }
    Log.d("Fingerprinter", "Results fetched in: " + (System.currentTimeMillis() - time) + " millis");


    Log.d("Fingerprinter", "RESULTS: " + result);
    // parse JSON
    JSONObject jobj = new JSONObject(result);

*/

/*    Activity activity = (Activity) act;
    activity.runOnUiThread(new Runnable() 
    {   
      public void run() 
      {
        act.didNotFindMatchForCode(mi);
      }
    });*/

  }

  public void didFailWithException(Exception e) 
  {
    resolved = true;
  }

  private static String convertStreamToString(InputStream is) 
  {
      /*
       * To convert the InputStream to String we use the BufferedReader.readLine()
       * method. We iterate until the BufferedReader return null which means
       * there's no more data to read. Each line will appended to a StringBuilder
       * and returned as String.
       */
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      StringBuilder sb = new StringBuilder();

      String line = null;
      try {
          while ((line = reader.readLine()) != null) {
              sb.append(line + "\n");
          }
      } catch (IOException e) {
          e.printStackTrace();
      } finally {
          try {
              is.close();
          } catch (IOException e) {
              e.printStackTrace();
          }
      }
      return sb.toString();
  }
}
