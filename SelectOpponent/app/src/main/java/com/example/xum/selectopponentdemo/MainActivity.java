package com.example.xum.selectopponentdemo;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xum.tasks.ImageLoadManager;
import com.example.xum.tasks.SliderScroll;

import java.util.ArrayList;

/**
 * Created by meng.xu on 2017/12/9.
 */

public class MainActivity extends FragmentActivity implements SliderScroll, HorizontalListView.OnItemSelectionListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    public HorizontalListViewAdapter homeAdapter;
    public HorizontalListViewAdapter awayAdapter;

    private Country home;
    private Country away;

    public HorizontalListView mListViewHome;
    public HorizontalListView mListViewAway;

    private ImageView icon_vs;
	
	private ImageButton imageButton;
	
    private TextView textviewOpponent1;
    private TextView textviewOpponent2;

    private ArrayList<Country> countries_home;
    private ArrayList<Country> countries_away;

	private ArrayList<Country> countries_home_;
    private ArrayList<Country> countries_away_;

    private String mCurrentFlagScroll = null;

    public static int layoutWidth = 230; // in dp

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection_screen);

        textviewOpponent1 = (TextView)findViewById(R.id.textViewOpponent1);
        textviewOpponent2 = (TextView)findViewById(R.id.textViewOpponent2);
        icon_vs = (ImageView)findViewById(R.id.iconVersus);
		imageButton = (ImageButton)findViewById(R.id.imageButton1);

        addData();
		initPosition();
        Log.d(TAG, "onCreate()");
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int mHomeCurrentIndex = 0;
                int mAwayCurrentIndex = 0;
                mHomeCurrentIndex = mListViewHome.getCurrentIndex();
                mAwayCurrentIndex = mListViewAway.getCurrentIndex();
                Log.d(TAG, "mHomeCurrentIndex: " + mHomeCurrentIndex + " ,mAwayCurrentIndex: " + mAwayCurrentIndex);
                String mHomeName = countries_home.get(mHomeCurrentIndex).getName();
                String mAwayName = countries_away.get(mAwayCurrentIndex).getName();
                Log.d(TAG, "mHomeName: " + mHomeName + " ,mAwayName: " + mAwayName);
            }
        });
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed()");
    }

    public void addData() {
        countries_home = new ArrayList<Country>();
        countries_away = new ArrayList<Country>();

		countries_home_ = new ArrayList<Country>();
        countries_away_ = new ArrayList<Country>();

        int drawableLength = CountryConstant.drawableNameArr.length;
        for (int i = 0; i < drawableLength; i++) {
            Country c = new Country();
            c.setName(CountryConstant.drawableNameArr[i]);
            c.setDrawableInt(CountryConstant.drawableIntArr[i]);
            c.setDrawableSwitchInt(CountryConstant.drawableSwitchIntArr[i]);
			c.setScrollMark(CountryConstant.HOME_FLAG_SCROLL);
            c.setFirstImageName(CountryConstant.firstImageName[i]);
            c.setSecondImageName(CountryConstant.secondImageName[i]);
            countries_home.add(c);
        }
        countries_home_.addAll(countries_home);

		for (int j = 0; j < drawableLength; j++) {
			Country c = new Country();
            c.setName(CountryConstant.drawableNameArr[j]);
            c.setDrawableInt(CountryConstant.drawableIntArr[j]);
            c.setDrawableSwitchInt(CountryConstant.drawableSwitchIntArr[j]);
			c.setScrollMark(CountryConstant.AWAY_FLAG_SCROLL);
            c.setFirstImageName(CountryConstant.firstImageName[j]);
            c.setSecondImageName(CountryConstant.secondImageName[j]);
            countries_away.add(c);
		}
		countries_away_.addAll(countries_away);

        initHListView();
    }

    public void initHListView() {
        WindowManager manager = this.getWindowManager();
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        float width = outMetrics.widthPixels;
        float height = outMetrics.heightPixels;
        Log.d(TAG, "width: " + width + " ,height: " + height);

        layoutWidth = Math.round(width * 0.65f);

		/* home listview */
        mListViewHome = (HorizontalListView)findViewById(R.id.home_list_view);
        homeAdapter = new HorizontalListViewAdapter(this, countries_home);
        mListViewHome.setAdapter(homeAdapter);

		/* away listview */
        mListViewAway = (HorizontalListView)findViewById(R.id.away_list_view);
        awayAdapter = new HorizontalListViewAdapter(this, countries_away);
        mListViewAway.setAdapter(awayAdapter);

        mListViewHome.enableFreeScroll();
        mListViewAway.enableFreeScroll();

        mListViewHome.setOnScrollListener(new HorizontalListView.OnScrollListener() {
            @Override
            public void onScrollPositionChanged(int position, int min, int max) {
                mCurrentFlagScroll = CountryConstant.HOME_FLAG_SCROLL;
                listViewScrolled();
            }
        });

        mListViewAway.setOnScrollListener(new HorizontalListView.OnScrollListener() {
            @Override
            public void onScrollPositionChanged(int position, int min, int max) {
                mCurrentFlagScroll = CountryConstant.AWAY_FLAG_SCROLL;
                listViewScrolled();
            }
        });

        mListViewHome.setAdapter(homeAdapter);
        mListViewAway.setAdapter(awayAdapter);

        mListViewHome.setOnItemSelectedListener(this);
        mListViewAway.setOnItemSelectedListener(this);

		homeAdapter.setHorizontalListView(mListViewHome);
		awayAdapter.setHorizontalListView(mListViewAway);
    }

    public void initPosition() {
		mListViewHome.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mListViewHome.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    mListViewHome.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
				int initHomeindex = 0;
				int initAwayindex = 1;
                mListViewHome.scrollToIndex(initHomeindex);
                mListViewAway.scrollToIndex(initAwayindex);
                listViewScrolled();
            }
        });
    }

	public void updateFlagAndCountry() {
		int mHomeCurrentIndex = 0;
		int mAwayCurrentIndex = 0;
				
		Log.d(TAG, "mCurrentFlagScroll: " + mCurrentFlagScroll);		
		mHomeCurrentIndex = mListViewHome.getCurrentIndex();
		mAwayCurrentIndex = mListViewAway.getCurrentIndex();
		Log.d(TAG, "mHomeCurrentIndex: " + mHomeCurrentIndex + " ,mAwayCurrentIndex: " + mAwayCurrentIndex);
		
		String mCurrentName = null;
		String mHomeName = countries_home.get(mHomeCurrentIndex).getName();
		String mAwayName = countries_away.get(mAwayCurrentIndex).getName(); 
		Log.d(TAG, "mHomeName: " + mHomeName + " ,mAwayName: " + mAwayName);
				
		int mHomeSize = countries_home.size();
		int mAwaySize = countries_away.size();
		Log.d(TAG, "mHomeSize: " + mHomeSize + " ,mAwaySize: " + mAwaySize);
	
		countries_home.clear();
		countries_home.addAll(countries_home_);
		homeAdapter.setList(countries_home);
	
		countries_away.clear();
		countries_away.addAll(countries_away_);
		awayAdapter.setList(countries_away);
								
		boolean removeAwayCountry = false;
		for (int i = 1; i < (mAwaySize - 1); i++) {
			mCurrentName = countries_away.get(i).getName();
			if (mHomeName != null && mHomeName.equalsIgnoreCase(mCurrentName)) {
				awayAdapter.getList().remove(i);
				awayAdapter.notifyDataSetChanged();
				removeAwayCountry = true;
				Log.d(TAG, "[away]remove away mCurrentName: " + mCurrentName);
				break;
			}
		}
	
		boolean removeHomeCountry = false;
		for (int j = 0; j < (mHomeSize - 1); j++) {
			 mCurrentName = countries_home.get(j).getName();
			 if (mAwayName != null && mAwayName.equalsIgnoreCase(mCurrentName)) {
				 homeAdapter.getList().remove(j);
				 homeAdapter.notifyDataSetChanged();
				 removeHomeCountry = true;
				 Log.d(TAG, "[home]remove home mCurrentName: " + mCurrentName);
				 break;
			 }
		}
		Log.d(TAG, "removeAwayCountry: " + removeAwayCountry + " ,removeHomeCountry: " + removeHomeCountry);
		
		int removedHomeSize = homeAdapter.getCount();
		int removedAwaySize = awayAdapter.getCount();
		Log.d(TAG, "removedHomeSize: " + removedHomeSize + " ,removedAwaySize: " + removedAwaySize);
		for (int m = 0; m < removedHomeSize; m++) {
			if (mHomeName != null &&
				mHomeName.equalsIgnoreCase(homeAdapter.getList().get(m).getName())) {
				//mListViewHome.scrollToIndex(m);
				Log.d(TAG, "homeAdapter name: " + homeAdapter.getList().get(m).getName());
				listViewScrolled();
				break;
			}
		}
		
		for (int n = 1; n < (removedAwaySize - 1); n++) {
			if (mAwayName != null &&
				mAwayName.equalsIgnoreCase(awayAdapter.getList().get(n).getName())) {
				//mListViewAway.scrollToIndex(n);
				Log.d(TAG, "awayAdapter name: " + awayAdapter.getList().get(n).getName());
				listViewScrolled();
				break;
			}
		}	
	}

    @Override
    public void listViewScrolled() {
        int homeCurrentIndex = mListViewHome.getCurrentIndex();
        int awayCurrentIndex = mListViewAway.getCurrentIndex();
		homeAdapter.setHomeCurrentIndex(homeCurrentIndex);
		awayAdapter.setAwayCurrentIndex(awayCurrentIndex);
        home = countries_home.get(homeCurrentIndex);
        away = countries_away.get(awayCurrentIndex);

        Log.d(TAG, "mCurrentFlagScroll: " + mCurrentFlagScroll);
        Log.d(TAG, "homeCurrentIndex: " + homeCurrentIndex + " ,awayCurrentIndex：" + awayCurrentIndex);
        Log.d(TAG, "home name: " + home.getName() + " ,away name：" + away.getName());

        textviewOpponent1.setText(home.getName());
        textviewOpponent2.setText(away.getName());

        icon_vs.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.icon_versus));
        textviewOpponent1.setTextColor(ContextCompat.getColor(this, R.color.white));
        textviewOpponent2.setTextColor(ContextCompat.getColor(this, R.color.white));
    }

    @Override
    public boolean isHorizontalScrollAllowed() {
        return true;
    }

    @Override
    public void onItemSelected(ViewGroup parent, View child, int position, long id) {
        Log.d(TAG, "onItemSelected");
		//updateFlagAndCountry();
    }
}
