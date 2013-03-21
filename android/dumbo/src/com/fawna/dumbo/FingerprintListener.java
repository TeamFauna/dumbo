package com.fawna.dumbo;

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

public class FingerprintListener implements AudioFingerprinterListener
{
  boolean recording, resolved;
  Fingerprinter fingerprinter;
  FingerprintShowActivity act;

  public FingerprintListener(FingerprintShowActivity acti) {
    act = acti;
  }

  public void startFingerprinting() { 
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
}
