package com.fawna.dumbo;

import android.app.ListActivity;
import android.app.ListFragment;
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
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;

import java.io.InputStream;
import java.util.*;

public class CardsFragment extends ListFragment {

  private Timer timer;
  private Handler handler;
  private LayoutInflater inflater;
  public static MovieInfo movieInfo = null;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    return inflater.inflate(R.layout.cardsview, container, false);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    this.inflater = getActivity().getLayoutInflater();
    timer = new Timer();
    handler = new Handler();

    if (movieInfo == null) {
      Toast.makeText(getActivity(), "No Movie Info loaded into CardsFragment, aborting!", 5000).show();
    }

    // test data with HIMYM
    //headerView = generateHeader("http://www.imdb.com/title/tt1777828/");
    View headerView = generateHeader(movieInfo.imdb);
    populateStatusBar(getView().findViewById(R.id.fixed_header));
    getListView().addHeaderView(headerView);

    CardsAdapter adapter = new CardsAdapter();
    setListAdapter(adapter);
    adapter.addMovieData();


    scheduleClock((TextView) getActivity().findViewById(R.id.current_time), System.currentTimeMillis() - movieInfo.time * 1000);


    final View header = getActivity().findViewById(R.id.fixed_header);
    header.setVisibility(View.GONE);
    getActivity().findViewById(R.id.status_drop_shadow).setVisibility(View.VISIBLE);

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
      list.smoothScrollBy(1200, 1400);
    } else if (getListAdapter().getCount() > 1) {
      Toast.makeText(list.getContext(), message, 3000).show();
    }
  }

  private void scheduleClock(final TextView view, final long time) {
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        handler.post(new Runnable() {
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
    View actor = inflater.inflate(R.layout.actor_card, null);

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
    UrlImageLoader.loadImage((ImageView) actor.findViewById(R.id.actor_photo), photoUrl, 240);
    return actor;
  }

  private View generateCommentCard(final String commenter, final String comment) {
    View commentView = inflater.inflate(R.layout.comment, null);

    TextView commenterView = (TextView) commentView.findViewById(R.id.commenter);
    commenterView.setText(commenter);
    setTypeface(commenterView, "fonts/avenir_heavy.otf");

    TextView commentContentView = (TextView) commentView.findViewById(R.id.comment_content);
    commentContentView.setText(comment);

    //munn
    String photoUrl = "https://fbcdn-sphotos-f-a.akamaihd.net/hphotos-ak-ash4/401335_10150475831548909_1801597514_n.jpg";
    if ("Will Hughes".equals(commenter)) {
      photoUrl = "https://fbcdn-sphotos-f-a.akamaihd.net/hphotos-ak-prn1/30983_10151248393758941_974643920_n.jpg";
    } else if ("Fravic Fernando".equals(commenter)) {
      photoUrl = "https://fbcdn-sphotos-g-a.akamaihd.net/hphotos-ak-ash4/392990_10200235523073002_283762175_n.jpg";
    } else if ("Andrew Russell".equals(commenter)) {
      photoUrl = "https://fbcdn-sphotos-e-a.akamaihd.net/hphotos-ak-prn1/856215_10200626685291813_1040285666_o.jpg";
    } else if ("Noah Sugarman".equals(commenter)) {
      photoUrl = "https://fbcdn-sphotos-a-a.akamaihd.net/hphotos-ak-prn1/16593_10200352135392016_544194112_n.jpg";
    }
    UrlImageLoader.loadImage((ImageView) commentView.findViewById(R.id.comment_photo), photoUrl, 100);

    return commentView;
  }

  private View generatePlotCard(final String name, final String description, final boolean showEpDescription) {

    View plotView = inflater.inflate(R.layout.plot_point, null);
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
    View headerView = inflater.inflate(R.layout.show_header, null);

    // set the cover photo to himym if necessary
    if (isHIMYM) {
      ImageView coverPhoto = (ImageView) headerView.findViewById(R.id.show_cover);
      coverPhoto.setImageResource(R.drawable.himym_cover);

      TextView title = (TextView) headerView.findViewById(R.id.show_title);
      title.setText("How I Met Your Mother");
    }

    setTypeface((TextView)headerView.findViewById(R.id.show_title), "fonts/avenir_next.ttc");

    Display display = getActivity().getWindowManager().getDefaultDisplay();
    ImageView cover = (ImageView)headerView.findViewById(R.id.show_cover);
    scaleImageToFitWidth(cover, display.getWidth());
    return headerView;
  }

  private void setTypeface(TextView tv, String face) {
    Typeface tf = Typeface.createFromAsset(getActivity().getAssets(), face);
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
      statusBar = inflater.inflate(R.layout.status_bar, null);
      populateStatusBar(statusBar);

      summary = generatePlotCard(movieInfo.name, movieInfo.summary, true);


      scheduleClock((TextView)statusBar.findViewById(R.id.current_time), System.currentTimeMillis() - movieInfo.time * 1000);
      setTypeface((TextView)statusBar.findViewById(R.id.current_time), "fonts/avenir_heavy.otf");

      cards = new ArrayList<View>();
      seenActors = new HashSet<String>();
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
        } else if (event.type.equals(MovieEvent.TYPE_COMMENT) && event.time >= movieInfo.time) {
          AddCommentEvent addCommentEvent = new AddCommentEvent(event, cards, list);
          long fireTime = (event.time - movieInfo.time) * 1000;
          timer.schedule(addCommentEvent, StrictMath.max(0, fireTime));
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
          TextView padding = new TextView(getActivity());
          padding.setBackgroundColor(Color.argb(255, 196, 196, 196));
          padding.setHeight(30);
          return padding;
      }  else if (position == 2) {
        return summary;
      }
      View v = cards.get(position - EXTRA_VIEWS);
      if (position == cards.size() - 1) {
          Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.fadein);
          v.startAnimation(animation);
      }
      return v;
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

      handler.post(new Runnable() {
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

      handler.post(new Runnable() {
        @Override
        public void run() {
          cards.add(generateActorCard(event.actor_name, event.actor_bio, event.actor_picture, event.actor_imdb));
          observer.onChanged();
          onNewCard(event.actor_name + " appeared!");
        }
      });
    }
  }

  public class AddCommentEvent extends TimerTask {
    final MovieEvent event;
    final List<View> cards;
    final DataSetObserver observer;

    public AddCommentEvent(MovieEvent event, final List<View> cards, final DataSetObserver observer) {
      this.event = event;
      this.cards = cards;
      this.observer = observer;
    }

    @Override
    public void run() {
      handler.post(new Runnable() {
        @Override
        public void run() {
          Log.d("lololol", "adding comment from" + event.commenter);
          cards.add(generateCommentCard(event.commenter, event.text));
          observer.onChanged();
          onNewCard(event.commenter + " says \"" + event.text + "\"");
        }
      });
    }
  }
  @Override
  public void onDestroy() {
    super.onDestroy();
    timer.cancel();
    timer.purge();
  }
}
