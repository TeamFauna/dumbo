package com.fawna.dumbo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


public class ShowPickerActivity extends Activity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    this.onCreate(savedInstanceState);
    setContentView(R.layout.show_picker);

    final EditText minuteBox = (EditText) findViewById(R.id.set_time);
    minuteBox.setInputType(InputType.TYPE_CLASS_NUMBER);


    Button lotr = (Button) findViewById(R.id.load_lotr);
    lotr.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        int minutes = getMinutes();
        //TODO WILL IMPLEMENT ME FOR OPENING LOTR
      }
    });
    Button himym = (Button) findViewById(R.id.load_himym);
    himym.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        int minutes = getMinutes();
        // TODO WILL IMPLEMENT ME FOR OPENING HIMYM
      }
    });
  }
  // TODO Will use me
  public void didFindMatchForCode(MovieInfo table) {
    Intent intent = new Intent(this, CardsActivity.class);
    intent.putExtra("imdb", table.imdb);
    startActivity(intent);
  }

  public int getMinutes() {
    final EditText minuteBox = (EditText) findViewById(R.id.set_time);
    minuteBox.setInputType(InputType.TYPE_CLASS_NUMBER);
    return Integer.parseInt(minuteBox.toString());
  }


}
