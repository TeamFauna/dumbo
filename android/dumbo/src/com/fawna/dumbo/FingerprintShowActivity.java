package com.fawna.dumbo;

import android.app.Activity;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.content.Intent;
import android.view.View;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


public class FingerprintShowActivity extends Activity 
{
    Boolean DEBUG = false;

    FingerprintListener fingerprinter;

  /**
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);
      layoutCover();

      fingerprinter = new FingerprintListener(FingerprintShowActivity.this);

      final Button button = (Button)findViewById(R.id.button_identify);
      button.setOnClickListener(new View.OnClickListener() {
          public void onClick(View view) {
              button.setText("");
              button.setEnabled(false);

              ImageView img = (ImageView)findViewById(R.id.mic_animation_red);
              img.setBackgroundResource(R.drawable.microphone_animation_red);
              AnimationDrawable frameAnimation = (AnimationDrawable) img.getBackground();
              frameAnimation.start();
              img.setVisibility(View.VISIBLE);

              Timer t = new Timer();
              final Date startDate = new Date();
              final long maxTime = 30000;
              t.schedule(new TimerTask() {
                  @Override
                  public void run() {
                      Date nowTime = new Date();
                      long timeDiff = nowTime.getTime() - startDate.getTime();

                      ImageView clip = (ImageView)findViewById(R.id.clip_button);
                      final ClipDrawable drawable = (ClipDrawable) clip.getDrawable();
                      float perc = (float)timeDiff / maxTime;
                      final int level = Math.round(perc * 10000);

                      runOnUiThread(new Runnable() {
                          public void run() {
                             drawable.setLevel(Math.min(level, 10000));
                          }
                      });

                      if (perc > 1) {
                          this.cancel();
                      }
                  }
              }, 0, 10);

              if (!DEBUG) {
                fingerprinter.startFingerprinting();
              } else {
                didNotFindMatchForCode();
              }
          }
      });
  }

  @Override
  public void onStart() {
      super.onStart();

      final Button button = (Button)findViewById(R.id.button_identify);
      button.setText(R.string.identify_button_text);
      ImageView img = (ImageView)findViewById(R.id.mic_animation);
      img.setVisibility(View.INVISIBLE);
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

  public void didFindMatchForCode(MovieInfo table) 
  {
    CardsActivity.movieInfo = table;
    Intent intent = new Intent(FingerprintShowActivity.this, CardsActivity.class);
    intent.putExtra("imdb", table.imdb);
    startActivity(intent);
  }

  public void didNotFindMatchForCode()
  {
    Intent intent = new Intent(FingerprintShowActivity.this, ShowPickerActivity.class);
    startActivity(intent);
  }
}
