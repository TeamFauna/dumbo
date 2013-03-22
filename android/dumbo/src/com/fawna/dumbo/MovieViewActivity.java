package com.fawna.dumbo;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

public class MovieViewActivity extends Activity {

  ViewPager mViewPager;
  FragmentPagerAdapter mPagerAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.movie_view);
    mViewPager = (ViewPager) findViewById(R.id.pager);

    mPagerAdapter = new MoviesFragmentPagerAdapter(getFragmentManager());

    mViewPager.setAdapter(mPagerAdapter);
  }


  public class MoviesFragmentPagerAdapter extends FragmentPagerAdapter {

    public MoviesFragmentPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public Fragment getItem(int i) {
      return new CardsFragment();
    }

    @Override
    public int getCount() {
      return 1;
    }
  }
}
