package com.fawna.dumbo;

import android.app.ListActivity;
import android.content.Intent;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListAdapter;

public class CardsActivity extends ListActivity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.cardsview);
    CardsAdapter adapter = new CardsAdapter();
    setListAdapter(adapter);
  }

  public class CardsAdapter implements ListAdapter {

    private int size = 3;
    private View headerView;
    private View description;
    private View actor;

    public CardsAdapter() {
      headerView = getLayoutInflater().inflate(R.layout.show_header, null);
      ImageButton imdbButton = (ImageButton) headerView.findViewById(R.id.imdb_button);
      imdbButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          Intent openImdb = new Intent(Intent.ACTION_VIEW);
          openImdb.setData(Uri.parse("http://www.imdb.com/title/tt0120737/"));
          startActivity(openImdb);
        }
      });

      description = getLayoutInflater().inflate(R.layout.plot_point, null);
      actor = getLayoutInflater().inflate(R.layout.actor_card, null);


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
      return size;
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
      if (position == 0)
        return headerView;
      if (position == 1)
        return description;
      return actor;
    }

    @Override
    public int getItemViewType(int position) {
      return 0;
    }

    @Override
    public int getViewTypeCount() {
      return 1;
    }

    @Override
    public boolean isEmpty() {
      return false;
    }
  }



}
