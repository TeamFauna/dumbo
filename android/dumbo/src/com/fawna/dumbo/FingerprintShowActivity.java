package com.fawna.dumbo;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;


public class FingerprintShowActivity extends Activity {

  /**
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);

      Display display = getWindowManager().getDefaultDisplay();
      int screenWidth = display.getWidth();

      // Scale cover correctly
      ImageView cover = (ImageView)findViewById(R.id.coverphoto);
      Drawable d = cover.getDrawable();
      int intendedWidth = screenWidth;
      int originalWidth = d.getIntrinsicWidth();
      int originalHeight = d.getIntrinsicHeight();
      float scale = (float)intendedWidth / originalWidth;
      int newHeight = Math.round(originalHeight * scale);
      cover.setLayoutParams(new RelativeLayout.LayoutParams(
              FrameLayout.LayoutParams.WRAP_CONTENT,
              FrameLayout.LayoutParams.WRAP_CONTENT));
      cover.getLayoutParams().width = intendedWidth;
      cover.getLayoutParams().height = newHeight;
  }
}
