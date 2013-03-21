package com.fawna.dumbo;

import android.app.ListActivity;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.DateTimeKeyListener;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.io.InputStream;
import java.net.URI;
import java.text.DateFormat;
import java.util.*;

public class CardsActivity extends ListActivity {

  private Timer timer;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.cardsview);

    String imdb = getIntent().getStringExtra("imdb");

    timer = new Timer();
    CardsAdapter adapter = new CardsAdapter(imdb);
    setListAdapter(adapter);

    final long time = System.currentTimeMillis();
    // update the clock
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            final TextView currentTime = (TextView) findViewById(R.id.current_time);
            if (currentTime != null) {
              long current = (System.currentTimeMillis() - time) / 1000;
              currentTime.setText("" + String.format("%d", current / 60) + ":" + String.format("%02d", current % 60));
            }
          }
        });
      }
    }, 1000, 1000);

  }

  private void layoutCover(View headerView) {
    Display display = getWindowManager().getDefaultDisplay();
    int screenWidth = display.getWidth();

    ImageView cover = (ImageView) headerView.findViewById(R.id.lotr_cover);
    Drawable d = cover.getDrawable();
    int intendedWidth = screenWidth;
    int originalWidth = d.getIntrinsicWidth();
    int originalHeight = d.getIntrinsicHeight();
    float scale = (float)intendedWidth / originalWidth;
    int newHeight = Math.round(originalHeight * scale);
    cover.setLayoutParams(new RelativeLayout.LayoutParams(
        RelativeLayout.LayoutParams.WRAP_CONTENT,
        RelativeLayout.LayoutParams.WRAP_CONTENT));
    cover.getLayoutParams().width = intendedWidth;
    cover.getLayoutParams().height = newHeight;
  }

  private View generateActorCard(final String name, final String photoUrl, final String imdbUrl) {
    View actor = getLayoutInflater().inflate(R.layout.actor_card, null);

    TextView nameView = (TextView) actor.findViewById(R.id.actor_name);
    nameView.setText(name);

    Button imdbView = (Button) actor.findViewById(R.id.actor_imdb);
    imdbView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent openImdb = new Intent(Intent.ACTION_VIEW);
        openImdb.setData(Uri.parse(imdbUrl));
        startActivity(openImdb);
      }
    });

    new DownloadImageTask((ImageView) actor.findViewById(R.id.actor_photo))
        .execute(photoUrl);


    return actor;
  }

  private View generatePlotCard(final String name, final String description) {
    View plotView = getLayoutInflater().inflate(R.layout.plot_point, null);
    TextView nameView = (TextView) plotView.findViewById(R.id.episode_name);
    nameView.setText(name) ;
    TextView descriptionView = (TextView) plotView.findViewById(R.id.episode_description);
    descriptionView.setText(description);
    return plotView;
  }

  private View generateHeader(final String imdbUrl) {

    // is this himym?
    final boolean isHIMYM = imdbUrl.contains("tt1777828");

    // generate the header
    View headerView = getLayoutInflater().inflate(R.layout.show_header, null);

    // create the imdb button handler
    ImageButton imdbButton = (ImageButton) headerView.findViewById(R.id.imdb_button);
    imdbButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent openImdb = new Intent(Intent.ACTION_VIEW);
        openImdb.setData(Uri.parse(isHIMYM ? "http://www.imdb.com/title/tt1777828/" : "http://www.imdb.com/title/tt0120737/"));
        startActivity(openImdb);
      }
    });

    // set the cover photo to himym if necessary
    if (isHIMYM) {
      ImageView coverPhoto = (ImageView) headerView.findViewById(R.id.lotr_cover);
      coverPhoto.setImageResource(R.drawable.himym_cover);

      TextView title = (TextView) headerView.findViewById(R.id.show_title);
      title.setText("How I met your Mother");

      TextView totalTime = (TextView) headerView.findViewById(R.id.total_time);
      totalTime.setText("of 22:45");

      TextView episode = (TextView) headerView.findViewById(R.id.episode);
      episode.setText("Season 6 episode 10");
    }

    TextView tv = (TextView) headerView.findViewById(R.id.show_title);
    Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/avenir_next.ttc");
    tv.setTypeface(tf);

    layoutCover(headerView);

    return headerView;
  }

  public class CardsAdapter implements ListAdapter {

    private View headerView;
    private List<View> cards;
    final static int EXTRA_VIEWS = 2;

    public CardsAdapter(String imdbUrl) {
      headerView = generateHeader(imdbUrl);

      cards = new ArrayList<View>();
      View actor1 = generateActorCard("Josh Radnor", "http://ia.media-imdb.com/images/M/MV5BMjAwNTUxMTM4OF5BMl5BanBnXkFtZTcwNjUyNzc4Mg@@._V1._SY314_CR3,0,214,314_.jpg", "http://www.imdb.com/name/nm1102140/");
      View actor2 = generateActorCard("Jason Segel", "http://ia.media-imdb.com/images/M/MV5BMTI2NTQ4MTM1MV5BMl5BanBnXkFtZTcwODEzNzQ4Mg@@._V1._SX214_CR0,0,214,314_.jpg", "http://www.imdb.com/name/nm0781981/");
      View description = generatePlotCard("Blitzgiving", "When Ted leaves the bar early to prepare a Thanksgiving feast for his friends, the gang winds up partying all night with The Blitz, an old friend from college who has bad luck. As a result, Ted is forced to spend Thanksgiving with Zoey.");

      cards.add(description);
      cards.add(actor1);
      cards.add(actor2);
    }

    @Override
    public boolean areAllItemsEnabled() {
      return true;
    }

    @Override
    public boolean isEnabled(int position) {
      return true;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
    }

    @Override
    public int getCount() {
      return cards.size() + EXTRA_VIEWS;
    }

    @Override
    public Object getItem(int position) {
      return null;
    }

    @Override
    public long getItemId(int position) {
      return 0;
    }

    @Override
    public boolean hasStableIds() {
      return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      if (position == 0) {
        return headerView;
      } else if (position == 1) {
          TextView padding = new TextView(getBaseContext());
          padding.setBackgroundColor(Color.argb(255, 196, 196, 196));
          padding.setHeight(30);
          return padding;
      }
      return cards.get(position - EXTRA_VIEWS);
    }

    @Override
    public int getItemViewType(int position) {
      return position;
    }

    @Override
    public int getViewTypeCount() {
      return cards.size() + EXTRA_VIEWS;
    }

    @Override
    public boolean isEmpty() {
      return false;
    }
  }


  private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
    ImageView bmImage;

    public DownloadImageTask(ImageView bmImage) {
      this.bmImage = bmImage;
    }

    protected Bitmap doInBackground(String... urls) {
      String urldisplay = urls[0];
      Bitmap mIcon11 = null;
      try {
        InputStream in = new java.net.URL(urldisplay).openStream();
        mIcon11 = BitmapFactory.decodeStream(in);
      } catch (Exception e) {
        Log.e("Error", e.getMessage());
        e.printStackTrace();
      }
      return mIcon11;
    }

    protected void onPostExecute(Bitmap result) {
      bmImage.setImageBitmap(result);
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    timer.cancel();
    timer.purge();
  }
}
