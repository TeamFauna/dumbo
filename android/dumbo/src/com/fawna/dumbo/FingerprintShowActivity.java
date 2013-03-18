package com.fawna.dumbo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;


public class FingerprintShowActivity extends Activity {

  /**
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    Button identifyButton = (Button)findViewById(R.id.button_identify);

  }
}
