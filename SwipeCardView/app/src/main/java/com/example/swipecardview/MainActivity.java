package com.example.swipecardview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.flingswipe.SwipeFlingAdapterView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SwipeFlingAdapterView.onFlingListener,
        SwipeFlingAdapterView.OnItemClickListener, View.OnClickListener {

	private static final String TAG = MainActivity.class.getSimpleName();

    private int cardWidth;
    private int cardHeight;

	private String mScrollDirection = null;
	private String SCROLL_LEFT = "scroll_left";
	private String SCROLL_RIGHT = "scroll_right";

    private SwipeFlingAdapterView swipeView;
    private InnerAdapter adapter;

    private ImageLoadManagerWithBitmap imageLoadManagerWithBitmap = new ImageLoadManagerWithBitmap();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        loadData();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // when app is background, will release memory cache,  or on need release, just to test
        //if (imageLoadManagerWithBitmap != null) {
            //imageLoadManagerWithBitmap.cleanMemoryCCache();
        //}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (imageLoadManagerWithBitmap != null) {
            imageLoadManagerWithBitmap.cleanMemoryCCache();
        }
    }

    private void initView() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float density = dm.density;
        cardWidth = (int) (dm.widthPixels - (2 * 18 * density));
        cardHeight = (int) (dm.heightPixels - (338 * density));

		Log.d(TAG, "density: " + density);
		Log.d(TAG, "cardWidth: " + cardWidth + " ,cardHeight: " + cardHeight);
		Log.d(TAG, "widthPixels: " + dm.widthPixels + " ,heightPixels: " + dm.heightPixels);
		
        swipeView = (SwipeFlingAdapterView) findViewById(R.id.swipe_view);
        if (swipeView != null) {
            swipeView.setIsNeedSwipe(true);
            swipeView.setFlingListener(this);
            swipeView.setOnItemClickListener(this);

            adapter = new InnerAdapter();
            swipeView.setAdapter(adapter);
        }

        View v = findViewById(R.id.swipeLeft);
        if (v != null) {
            v.setOnClickListener(this);
        }
        v = findViewById(R.id.swipeRight);
        if (v != null) {
            v.setOnClickListener(this);
        }

    }


    @Override
    public void onItemClicked(MotionEvent event, View v, Object dataObject) {
    	Log.d(TAG, "onItemClicked");
		Talent talent = (Talent)dataObject;
		String nickname = talent.nickname;
		String describe = talent.describe;
		Log.d(TAG, "nickname: " + nickname + " ,describe: " + describe);
		if (getScrollDirection() != null) {
			Log.d(TAG, "last scroll direction: " + getScrollDirection());
		} else {
			Log.d(TAG, "neither scroll left or scroll right");
		}
		
    }

    @Override
    public void removeFirstObjectInAdapter() {
    	Log.d(TAG, "removeFirstObjectInAdapter");
        adapter.remove(0);
		setScrollDirection(null);
    }

    @Override
    public void onLeftCardExit(Object dataObject) {
    	Log.d(TAG, "onLeftCardExit");
		setScrollDirection(SCROLL_LEFT);
    }

    @Override
    public void onRightCardExit(Object dataObject) {
    	Log.d(TAG, "onRightCardExit");
		setScrollDirection(SCROLL_RIGHT);
    }

    @Override
    public void onAdapterAboutToEmpty(int itemsInAdapter) {
    	Log.d(TAG, "onAdapterAboutToEmpty");
        if (itemsInAdapter == 3) {
            loadData();
        }
    }

    @Override
    public void onScroll(float progress, float scrollXProgress) {
    	Log.d(TAG, "onScroll");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.swipeLeft:
                swipeView.swipeLeft();
				setScrollDirection(SCROLL_LEFT);
                //swipeView.swipeLeft(250);
                break;
            case R.id.swipeRight:
                swipeView.swipeRight();
				setScrollDirection(SCROLL_RIGHT);
                //swipeView.swipeRight(250);
        }
    }

    private void loadData() {
        new AsyncTask<Void, Void, List<Talent>>() {
            @Override
            protected List<Talent> doInBackground(Void... params) {
            	int len = Flag.drawableIntArr.length;
                ArrayList<Talent> list = new ArrayList<>(len);
                Talent talent;
                for (int i = 0; i < len; i++) {
                    talent = new Talent();
                    talent.headerIcon = Flag.drawableIntArr[i];
                    talent.describe = "World Cup in Russia";
                    talent.nickname = Flag.drawableNameArr[i];
                    talent.cityName = "WorldCup";
                    talent.educationName = "WorldCup";
                    talent.workYearName = "WorldCup";
                    list.add(talent);
                }
                return list;
            }

            @Override
            protected void onPostExecute(List<Talent> list) {
                super.onPostExecute(list);
                adapter.addAll(list);
            }
        }.execute();
    }


    private class InnerAdapter extends BaseAdapter {

        ArrayList<Talent> objs;

        public InnerAdapter() {
            objs = new ArrayList<>();
        }

        public void addAll(Collection<Talent> collection) {
            if (isEmpty()) {
                objs.addAll(collection);
                notifyDataSetChanged();
            } else {
                objs.addAll(collection);
            }
        }

        public void clear() {
            objs.clear();
            notifyDataSetChanged();
        }

        public boolean isEmpty() {
            return objs.isEmpty();
        }

        public void remove(int index) {
            if (index > -1 && index < objs.size()) {
                objs.remove(index);
                notifyDataSetChanged();
            }
        }


        @Override
        public int getCount() {
            return objs.size();
        }

        @Override
        public Talent getItem(int position) {
            if(objs==null ||objs.size()==0) return null;
            return objs.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        // TODO: getView
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            Talent talent = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_new_item, parent, false);
                holder  = new ViewHolder();
                convertView.setTag(holder);
                convertView.getLayoutParams().width = cardWidth;
                holder.portraitView = (ImageView) convertView.findViewById(R.id.portrait);
                holder.describe = (TextView)convertView.findViewById(R.id.describe);
                //holder.portraitView.getLayoutParams().width = cardWidth;
                //holder.portraitView.getLayoutParams().height = cardHeight; // set image really height
                holder.describe.getLayoutParams().height = cardHeight - holder.portraitView.getLayoutParams().height;
                holder.nameView = (TextView) convertView.findViewById(R.id.name);
                //parentView.getLayoutParams().width = cardWidth;
                //holder.jobView = (TextView) convertView.findViewById(R.id.job);
                //holder.companyView = (TextView) convertView.findViewById(R.id.company);
                holder.cityView = (TextView) convertView.findViewById(R.id.city);
                holder.eduView = (TextView) convertView.findViewById(R.id.education);
                holder.workView = (TextView) convertView.findViewById(R.id.work_year);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            loadBitImage(talent.headerIcon, holder.portraitView);
            //holder.portraitView.setImageResource(talent.headerIcon);

            holder.describe.setText(talent.describe);

            holder.nameView.setText(String.format("%s", talent.nickname));
            //holder.jobView.setText(talent.jobName);

            final CharSequence no = "暂无";

            holder.cityView.setHint(no);
            holder.cityView.setText(talent.cityName);
            holder.cityView.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.home01_icon_location,0,0);

            holder.eduView.setHint(no);
            holder.eduView.setText(talent.educationName);
            holder.eduView.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.home01_icon_edu,0,0);

            holder.workView.setHint(no);
            holder.workView.setText(talent.workYearName);
            holder.workView.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.home01_icon_work_year,0,0);

            return convertView;
        }

    }

    private void loadBitImage(int drawableInt, ImageView imageView) {
        String keyDrawable = String.valueOf(drawableInt);
        Log.d(TAG, "keyDrawable: " + keyDrawable);
        Bitmap bm = null;
        bm = imageLoadManagerWithBitmap.getBitmapFromMemCache(keyDrawable);
        Log.d(TAG, "bm: " + bm);
        if (bm == null){
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;
            bm = BitmapFactory.decodeResource(getResources(), drawableInt, options);
            imageLoadManagerWithBitmap.addBitmapToMemoryCache(keyDrawable, bm);
        }
        imageView.setImageBitmap(bm);
    }

	public void setScrollDirection(String scrollDirection) {
		mScrollDirection = scrollDirection;
	}

	public String getScrollDirection() {
		return mScrollDirection;
	}

    private static class ViewHolder {
        ImageView portraitView;
        TextView nameView;
        TextView describe;
        TextView cityView;
        TextView eduView;
        TextView workView;
        CheckedTextView collectView;

    }

    public static class Talent {
        public int headerIcon;
        public String describe;
        public String nickname;
        public String cityName;
        public String educationName;
        public String workYearName;
    }

}

