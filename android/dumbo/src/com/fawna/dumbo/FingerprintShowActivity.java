package com.fawna.dumbo;

import android.app.Activity;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Display;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.content.Intent;
import android.view.View;

import java.util.Timer;
import java.util.TimerTask;

public class FingerprintShowActivity extends Activity {

  /**
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);
      layoutCover();

      final Button button = (Button)findViewById(R.id.button_identify);
      button.setOnClickListener(new View.OnClickListener() {
          public void onClick(View view) {
              button.setText("");

              ImageView img = (ImageView)findViewById(R.id.mic_animation);
              img.setBackgroundResource(R.drawable.microphone_animation);
              AnimationDrawable frameAnimation = (AnimationDrawable) img.getBackground();
              frameAnimation.start();
              img.setVisibility(View.VISIBLE);

              Timer t = new Timer();
              t.schedule(new TimerTask() {
                  @Override
                  public void run() {
                      Intent intent = new Intent(FingerprintShowActivity.this, CardsActivity.class);
                      startActivity(intent);
                  }
              }, 4000);
          }
      });
  }

  private void layoutCover() {
      Display display = getWindowManager().getDefaultDisplay();
      int screenWidth = display.getWidth();

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
