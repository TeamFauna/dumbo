package com.fawna.dumbo;

import android.app.Activity;
import org.json.JSONObject;
import fauna.dumbo.Fingerprinter;
import fauna.dumbo.Fingerprinter.AudioFingerprinterListener;

public class FingerprintListener implements AudioFingerprinterListener
{
  boolean recording, resolved;
  Fingerprinter fingerprinter;
  FingerprintShowActivity act;

  public FingerprintListener(FingerprintShowActivity acti) {
    act = acti;

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

  public void didFindMatchForCode(final JSONObject table,
      String code) 
  {
    final MovieInfo mi = new MovieInfo(table);
    //Return MovieInfo
    Activity activity = (Activity) act;
    activity.runOnUiThread(new Runnable() 
    {   
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
  }

  public void didFailWithException(Exception e) 
  {
    resolved = true;
  }
}
