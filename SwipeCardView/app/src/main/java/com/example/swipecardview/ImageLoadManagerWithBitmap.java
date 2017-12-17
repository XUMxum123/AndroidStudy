package com.example.swipecardview;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.util.LruCache;

public class ImageLoadManagerWithBitmap {

    private static final String TAG = ImageLoadManagerWithBitmap.class.getSimpleName();

    private LruCache<String, Bitmap> mMemoryCache;

    public ImageLoadManagerWithBitmap() {
        final int maxMemory = (int)(Runtime.getRuntime().maxMemory());
        int cacheSize = maxMemory / 8;// 1/8th of the RAM only
        Log.d(TAG, "cache size: " + cacheSize);
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(final String key, final Bitmap value) {
                final int size = getBitmapSize(value);
                Log.d(TAG, "bitmap size : " + size);
                return size;
            }

            @Override
            protected void entryRemoved(final boolean evicted, final String key, final Bitmap oldValue,
                                        final Bitmap newValue) {
                Log.d(TAG, "entry removed: " + key);
                if ((oldValue != null) && !oldValue.isRecycled()) {
                    oldValue.recycle();
                }
            }
        };
    }

    /**
     * Get the size in bytes of a bitmap in a BitmapDrawable.
     *
     * @param bitmap
     * @return size in bytes
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static int getBitmapSize(final Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return bitmap.getAllocationByteCount();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            return bitmap.getByteCount();
        }
        return bitmap.getRowBytes() * bitmap.getHeight();
    }

    /**
     *
     * @param key
     * @param bitmap
     */
    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if ((key != null) && (bitmap != null)) {
            Log.d(TAG, "addBitmapToMemoryCache: " + key);
            synchronized (mMemoryCache) {
                if (mMemoryCache.get(key) != null) {
                    Log.d(TAG, "already in cache: " + key);
                    final Bitmap oldBitmap = mMemoryCache.remove(key);
                    if ((oldBitmap != null) && !oldBitmap.isRecycled()) {
                        oldBitmap.recycle();
                    }
                }
                mMemoryCache.put(key, bitmap);
            }
        } else {
            Log.w(TAG, "Warning!! key : " + key + " bitmap: " + bitmap);
        }
    }

    /**
     *
     * @param key
     * @return
     */
    public Bitmap getBitmapFromMemCache(String key) {
        if (key != null) {
            Bitmap bit = null;
            synchronized (mMemoryCache) {
                bit = mMemoryCache.get(key);
                if ((bit != null) && bit.isRecycled()) {
                    mMemoryCache.remove(key);
                    bit = null;
                }
            }
            return bit;
        }
        return null;
    }

    public int getMemCacheSize() {
        int size = -1;
        if (mMemoryCache != null) {
            size = mMemoryCache.size();
        }
        return size;
    }

    /**
     *
     * @param key
     */
    public void removeCacheFromMemory(String key) {
        mMemoryCache.remove(key);
    }

    /**
     * clear Memory cache
     */
    public void cleanMemoryCCache() {
        Log.d(TAG, "cleanMemoryCCache");
        mMemoryCache.evictAll();
    }

}
