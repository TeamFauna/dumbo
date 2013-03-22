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
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.io.InputStream;
import java.util.*;

public class CardsActivity extends ListActivity {

  private Timer timer;
  public static MovieInfo movieInfo = null;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.cardsview);

    Log.d("Fingerprinter", "IN CARDSACTIVITY");

    timer = new Timer();

    if (movieInfo == null) {
      Toast.makeText(this, "No Movie Info loaded into CardsActivity, aborting!", 5000).show();
      finish();
      return;
    }


    // test data with HIMYM
    //headerView = generateHeader("http://www.imdb.com/title/tt1777828/");
    View headerView = generateHeader(movieInfo.imdb);
    populateStatusBar(findViewById(R.id.fixed_header));
    getListView().addHeaderView(headerView);

    CardsAdapter adapter = new CardsAdapter();
    setListAdapter(adapter);
    adapter.addMovieData();


    scheduleClock((TextView) findViewById(R.id.current_time), System.currentTimeMillis() - movieInfo.time * 1000);


    final View header = findViewById(R.id.fixed_header);
    header.setVisibility(View.GONE);
    findViewById(R.id.status_drop_shadow).setVisibility(View.VISIBLE);

    getListView().setOnScrollListener(new AbsListView.OnScrollListener() {

      @Override
      public void onScrollStateChanged(AbsListView view, int scrollState) {

      }

      @Override
      public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (firstVisibleItem != 0) {
          header.setVisibility(View.VISIBLE);
        } else {
          header.setVisibility(View.GONE);
        }
      }
    });

  }

  public void onNewCard(final String message) {
    ListView list = getListView();
    if (list.getLastVisiblePosition() >= getListAdapter().getCount() - 1) {
      //list.smoothScrollToPosition(getListAdapter().getCount() - 1);
      list.smoothScrollBy(900, 1000);
    } else if (getListAdapter().getCount() > 1) {
      Toast.makeText(this, message, 3000).show();
    }
  }

  private void scheduleClock(final TextView view, final long time) {
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            long current = (System.currentTimeMillis() - time) / 1000;
            String time = "" + String.format("%d", current / 60) + ":" + String.format("%02d", current % 60);
            view.setText(time);
          }
        });
      }
    }, 1000, 1000);

  }

  private void scaleImageToFitWidth(ImageView image, int intendedWidth) {
    Drawable d = image.getDrawable();
    int originalWidth = d.getIntrinsicWidth();
    int originalHeight = d.getIntrinsicHeight();
    float scale = (float)intendedWidth / originalWidth;
    int newHeight = Math.round(originalHeight * scale);
    image.getLayoutParams().width = intendedWidth;
    image.getLayoutParams().height = newHeight;
  }

  private View generateActorCard(final String name, final String bio, final String photoUrl, final String imdbUrl) {
    View actor = getLayoutInflater().inflate(R.layout.actor_card, null);

    TextView nameView = (TextView)actor.findViewById(R.id.actor_name);
    nameView.setText(name);
    setTypeface(nameView, "fonts/avenir_heavy.otf");
    setTypeface((TextView)actor.findViewById(R.id.actor_description_header), "fonts/avenir_heavy.otf");

    TextView actorBioView = (TextView) actor.findViewById(R.id.actor_bio);
    actorBioView.setText(bio);

    Button imdbView = (Button) actor.findViewById(R.id.actor_imdb);
      if (imdbUrl != null) {
      imdbView.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          Intent openImdb = new Intent(Intent.ACTION_VIEW);
          openImdb.setData(Uri.parse(imdbUrl));
          startActivity(openImdb);
        }
      });
    } else {
        imdbView.setVisibility(View.GONE);
    }
    new DownloadImageTask((ImageView)actor.findViewById(R.id.actor_photo))
        .execute(photoUrl);

    return actor;
  }

  private View generatePlotCard(final String name, final String description, final boolean showEpDescription) {

    View plotView = getLayoutInflater().inflate(R.layout.plot_point, null);
    TextView nameView = (TextView) plotView.findViewById(R.id.episode_name);
    nameView.setText(name) ;
    if (!showEpDescription) {
       TextView descHeader = (TextView) plotView.findViewById(R.id.ep_description_header);
       descHeader.setText(name);
       setTypeface(descHeader, "fonts/avenir_heavy.otf");
       nameView.setVisibility(View.GONE);
    }

    TextView descriptionView = (TextView) plotView.findViewById(R.id.episode_description);
    descriptionView.setText(description);
    return plotView;
  }

  private void populateStatusBar(final View statusBar) {
    final boolean isHIMYM = movieInfo.imdb.contains("tt1777828");

    // create the imdb button handler
    ImageButton imdbButton = (ImageButton) statusBar.findViewById(R.id.imdb_button);
    imdbButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent openImdb = new Intent(Intent.ACTION_VIEW);
        openImdb.setData(Uri.parse(isHIMYM ? "http://www.imdb.com/title/tt1777828/" : "http://www.imdb.com/title/tt0584441/"));
        startActivity(openImdb);
      }
    });

    // set the cover photo to himym if necessary
    if (isHIMYM) {
      TextView totalTime = (TextView) statusBar.findViewById(R.id.total_time);
      totalTime.setText("of 21:03");
      setTypeface(totalTime, "fonts/avenir_light.otf");

      TextView episode = (TextView) statusBar.findViewById(R.id.episode);
      episode.setText("Se. 6 Ep. 10");
      setTypeface(episode, "fonts/avenir_light.otf");
    }
  }

  private View generateHeader(final String imdbUrl) {

    // is this himym?
    final boolean isHIMYM = imdbUrl.contains("tt1777828");

    // generate the header
    View headerView = getLayoutInflater().inflate(R.layout.show_header, null);

    // set the cover photo to himym if necessary
    if (isHIMYM) {
      ImageView coverPhoto = (ImageView) headerView.findViewById(R.id.show_cover);
      coverPhoto.setImageResource(R.drawable.himym_cover);

      TextView title = (TextView) headerView.findViewById(R.id.show_title);
      title.setText("How I Met Your Mother");
    }

    setTypeface((TextView)headerView.findViewById(R.id.show_title), "fonts/avenir_next.ttc");

    Display display = getWindowManager().getDefaultDisplay();
    ImageView cover = (ImageView)headerView.findViewById(R.id.show_cover);
    scaleImageToFitWidth(cover, display.getWidth());
    return headerView;
  }

  private void setTypeface(TextView tv, String face) {
    Typeface tf = Typeface.createFromAsset(getAssets(), face);
    tv.setTypeface(tf);
  }

  public class CardsAdapter implements ListAdapter {

    final private List<View> cards;
    private View statusBar;
    private View summary;
    final static int EXTRA_VIEWS = 3;
    final HashSet<String> seenActors;
    DataSetObserver list;

    public CardsAdapter() {
      statusBar = getLayoutInflater().inflate(R.layout.status_bar, null);
      populateStatusBar(statusBar);

      summary = generatePlotCard(movieInfo.name, movieInfo.summary, true);


      scheduleClock((TextView)statusBar.findViewById(R.id.current_time), System.currentTimeMillis() - movieInfo.time * 1000);
      setTypeface((TextView)statusBar.findViewById(R.id.current_time), "fonts/avenir_heavy.otf");

      cards = new ArrayList<View>();
      seenActors = new HashSet<String>();

      //test cards
      //View actor1 = generateActorCard("Josh Radnor", "http://ia.media-imdb.com/images/M/MV5BMjAwNTUxMTM4OF5BMl5BanBnXkFtZTcwNjUyNzc4Mg@@._V1._SY314_CR3,0,214,314_.jpg", "http://www.imdb.com/name/nm1102140/");
      //View actor2 = generateActorCard("Jason Segel", "http://ia.media-imdb.com/images/M/MV5BMTI2NTQ4MTM1MV5BMl5BanBnXkFtZTcwODEzNzQ4Mg@@._V1._SX214_CR0,0,214,314_.jpg", "http://www.imdb.com/name/nm0781981/");
      //View description = generatePlotCard("Blitzgiving", "When Ted leaves the bar early to prepare a Thanksgiving feast for his friends, the gang winds up partying all night with The Blitz, an old friend from college who has bad luck. As a result, Ted is forced to spend Thanksgiving with Zoey.");

      //cards.add(description);
      //cards.add(actor1);
      //cards.add(actor2);
    }

    public void addMovieData() {
      for (MovieEvent event: movieInfo.events) {
        if (event.type.equals(MovieEvent.TYPE_PLOT)) {
          AddPlotEvent addPlotEvent = new AddPlotEvent(event, cards, list);
          long fireTime = (event.time - movieInfo.time) * 1000;
          timer.schedule(addPlotEvent,  Math.max(0, fireTime));
        } else if (event.type.equals(MovieEvent.TYPE_ACTOR)
            && !seenActors.contains(event.actor_name)
            && event.time >= movieInfo.time) {
          seenActors.add(event.actor_name);
          AddActorEvent addActorEvent = new AddActorEvent(event, cards, list);
          long fireTime = (event.time - movieInfo.time) * 1000;
          timer.schedule(addActorEvent, Math.max(0, fireTime));
        }
      }
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
      list = observer;

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
      list = null;
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
      return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      if (position == 0) {
        return statusBar;
      } else if (position == 1) {
          TextView padding = new TextView(getBaseContext());
          padding.setBackgroundColor(Color.argb(255, 196, 196, 196));
          padding.setHeight(30);
          return padding;
      }  else if (position == 2) {
        return summary;
      }
      return cards.get(position - EXTRA_VIEWS);
    }

    @Override
    public int getItemViewType(int position) {
      return IGNORE_ITEM_VIEW_TYPE;
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

  public class AddPlotEvent extends TimerTask {
    final MovieEvent event;
    final List<View> cards;
    final DataSetObserver observer;
    final Random choose = new Random();

    public AddPlotEvent(MovieEvent event, final List<View> cards, final DataSetObserver observer) {
      this.event = event;
      this.cards = cards;
      this.observer = observer;
    }

    @Override
    public void run() {

      runOnUiThread(new Runnable() {
        @Override
        public void run() {
        String[] funnyHeaders = {"Did you know?", "Fun fact", "Trivia Time", "Were you aware?", "Check this out",
            "Knowledge bomb!", "Impress your friends", "Here's a trivia gem", "A wild factoid appeared!"};
        int choice = choose.nextInt(funnyHeaders.length);
        cards.add(generatePlotCard(funnyHeaders[choice], event.text, false));
        observer.onChanged();
        onNewCard("New triva appeared!");
        }
      });
    }
  }

  public class AddActorEvent extends TimerTask {
    final MovieEvent event;
    final List<View> cards;
    final DataSetObserver observer;

    public AddActorEvent(MovieEvent event, final List<View> cards, final DataSetObserver observer) {
      this.event = event;
      this.cards = cards;
      this.observer = observer;
    }

    @Override
    public void run() {

      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          cards.add(generateActorCard(event.actor_name, event.actor_bio, event.actor_picture, event.actor_imdb));
          observer.onChanged();
          onNewCard(event.actor_name + " appeared!");
        }
      });
    }
  }


  private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
    ImageView bmImage;

    public DownloadImageTask(ImageView bmImage) {
      this.bmImage = bmImage;
    }

    protected Bitmap doInBackground(String... urls) {
      String urldisplay = urls[0];
      Log.d("lololol", urldisplay);
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
      scaleImageToFitWidth(this.bmImage, 240);
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    timer.cancel();
    timer.purge();
  }
}
