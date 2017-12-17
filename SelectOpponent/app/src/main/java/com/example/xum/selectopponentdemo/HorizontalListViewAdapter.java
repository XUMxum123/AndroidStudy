package com.example.xum.selectopponentdemo;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.example.xum.tasks.FlipAnimator;
import com.example.xum.tasks.ImageLoadManager;

import java.io.InputStream;
import java.util.ArrayList;

public class HorizontalListViewAdapter extends BaseAdapter {

    private static final String TAG = HorizontalListViewAdapter.class.getSimpleName();

    private ArrayList<Country> mList;
    private final MainActivity mContext;
	private HorizontalListView mHorizontalListView;
	private int mHomeCurrentIndex;
	private int mAwayCurrentIndex;

    private final static ImageLoadManager mImageLruCacheManager = new ImageLoadManager();

    public void setList(ArrayList<Country> list) {
        mList = list;
    }

    public ArrayList<Country> getList() {
        return mList;
    }

	public void setHorizontalListView(HorizontalListView horizontalListView) {
		mHorizontalListView = horizontalListView;
	}

	public HorizontalListView getHorizontalListView() {
		return mHorizontalListView;
	}

	public void setHomeCurrentIndex(int homeCurrentIndex) {
		mHomeCurrentIndex = homeCurrentIndex;
	}

	public int getHomeCurrentIndex() {
		return mHomeCurrentIndex;
	}

	public void setAwayCurrentIndex(int awayCurrentIndex) {
		mAwayCurrentIndex = awayCurrentIndex;
	}

	public int getAwayCurrentIndex() {
		return mAwayCurrentIndex;
	}

    public HorizontalListViewAdapter(MainActivity context, ArrayList<Country> list) {
        mContext = context;
        setList(list);
    }

    @Override
    public Object getItem(int position) {
        return getList().get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            convertView = (LinearLayout) mContext.getLayoutInflater().inflate(R.layout.selection_layout, parent, false);
            holder = new ViewHolder();
            holder.flipper = (ViewFlipper)convertView.findViewById(R.id.content);
            holder.firstLayout = (LinearLayout)convertView.findViewById(R.id.firstLayout);
            holder.secondLayout = (LinearLayout)convertView.findViewById(R.id.secondLayout);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }

//		Log.d(TAG, "**************************************start************************************************");
//		Country country = getList().get(position);
//		Log.d(TAG, "position: " + position + " ,name: " + country.getName());
//		if (position > 0 && position < (getCount() - 1)) {
//			if ((CountryConstant.HOME_FLAG_SCROLL).equalsIgnoreCase(country.getScrollMark())) {
//				country = getList().get(getHomeCurrentIndex());
//			} else if((CountryConstant.AWAY_FLAG_SCROLL).equalsIgnoreCase(country.getScrollMark())) {
//				country = getList().get(getAwayCurrentIndex());
//			}
//		}

        final Country country = getList().get(position);
        holder.flipper.setBackgroundColor(Color.parseColor("#3a424c"));

//		int homeCurrentIndex = 0;
//		int awayCurrentIndex = 0;
//		if (getHorizontalListView() != null) {
//			Log.d(TAG, "enter: scrollMark: " + country.getScrollMark());
//			if ((CountryConstant.HOME_FLAG_SCROLL).equalsIgnoreCase(country.getScrollMark())) {
//				homeCurrentIndex = getHorizontalListView().getCurrentIndex();
//			} else if((CountryConstant.AWAY_FLAG_SCROLL).equalsIgnoreCase(country.getScrollMark())) {
//				awayCurrentIndex = getHorizontalListView().getCurrentIndex();
//			}		
//		}
//		Log.d(TAG, "homeCurrentIndex: " + homeCurrentIndex + " ,awayCurrentIndex: " + awayCurrentIndex);
//		Log.d(TAG, "home index: " + getHomeCurrentIndex()+ " ,away index: " + getAwayCurrentIndex());
//		Log.d(TAG, "home name: " + getList().get(getHomeCurrentIndex()).getName());
//		Log.d(TAG, "away name: " + getList().get(getAwayCurrentIndex()).getName());
//		Log.d(TAG, "**************************************end************************************************");

        try {
            String firstImageKey = country.getName() + "_first";
            Log.d(TAG, "firstImageKey: " + firstImageKey);
            Drawable dr_first = mImageLruCacheManager.getDrawableFromMemCache(firstImageKey);
            if (dr_first != null) {
                setBackgroundDrawable(dr_first, holder.firstLayout);
            } else {
                InputStream firstImageStream = openFile(country.getFirstImageName());
                Log.d(TAG, "firstImageStream: " + firstImageStream);
                Drawable dr_create_first = Drawable.createFromStream(firstImageStream, null);
                mImageLruCacheManager.addDrawableToMemoryCache(firstImageKey, dr_create_first);
                setBackgroundDrawable(dr_create_first, holder.firstLayout);              
            }
        } catch (Exception e) {
            Toast.makeText(mContext, "Could not load image", Toast.LENGTH_SHORT).show();
        }

        try {
            String secondImageKey = country.getName() + "_second";
            Log.d(TAG, "secondImageKey: " + secondImageKey);
            Drawable dr_second = mImageLruCacheManager.getDrawableFromMemCache(secondImageKey);
            if (dr_second != null) {
                setBackgroundDrawable(dr_second, holder.secondLayout);
            } else {
                InputStream secondImageStream = openFile(country.getSecondImageName());
                Log.d(TAG, "secondImageStream: " + secondImageStream);
                Drawable dr_create_second = Drawable.createFromStream(secondImageStream, null);
                mImageLruCacheManager.addDrawableToMemoryCache(secondImageKey, dr_create_second);
                setBackgroundDrawable(dr_create_second, holder.secondLayout);
            }  
        } catch (Exception e) {
            Toast.makeText(mContext, "Could not load image", Toast.LENGTH_SHORT).show();
        }
        //holder.firstLayout.setBackgroundResource(country.getDrawableInt());
        //holder.secondLayout.setBackgroundResource(country.getDrawableSwitchInt());

        ViewGroup.LayoutParams layoutParams = holder.flipper.getLayoutParams();
        layoutParams.width = MainActivity.layoutWidth;
        layoutParams.height = (int) Math.round(MainActivity.layoutWidth * 0.75);
        holder.flipper.setLayoutParams(layoutParams);
        holder.flipper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FlipAnimator animator = new FlipAnimator(holder.flipper, holder.flipper.getWidth() / 2, holder.flipper.getHeight() / 2);
                holder.flipper.startAnimation(animator);
            }
        });

        return convertView;
    }

    @Override
    public int getCount() {
        return getList().size();
    }

    public static void setBackgroundDrawable(Drawable image, View layout) {
        if (Build.VERSION.SDK_INT >= 16) {
            layout.setBackground(image);
        } else {
            layout.setBackgroundDrawable(image);
        }
    }

    private InputStream openFile(String file) throws Exception {
        String fileName = "";
        return mContext.getAssets().open("vlaggen/" + file);
    }

    private final static class ViewHolder {
        private ViewFlipper flipper;
        private LinearLayout firstLayout;
        private LinearLayout secondLayout;
    }

}
