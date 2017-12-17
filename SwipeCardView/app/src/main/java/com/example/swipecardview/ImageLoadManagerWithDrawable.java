package com.example.swipecardview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.LruCache;

public class ImageLoadManagerWithDrawable {

    private LruCache<String, Drawable> mMemoryCache;

    public ImageLoadManagerWithDrawable() {
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int mCacheSize = maxMemory / 8;
        mMemoryCache = new LruCache<String, Drawable>(mCacheSize) {
            @Override
            protected int sizeOf(String key, Drawable value) {
                if (value instanceof BitmapDrawable) {
                    Bitmap bitmap = ((BitmapDrawable) value).getBitmap();
                    return bitmap == null ? 0 : bitmap.getByteCount();
                }
                return super.sizeOf(key, value);
            }
        };
    }

    /**
     *
     * @param key
     * @param drawable
     */
    public void addDrawableToMemoryCache(String key, Drawable drawable) {
        if (getDrawableFromMemCache(key) == null && drawable != null) {
            mMemoryCache.put(key, drawable);
        }
    }

    /**
     *
     * @param key
     * @return
     */
    public Drawable getDrawableFromMemCache(String key) {
        return mMemoryCache.get(key);
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
        mMemoryCache.evictAll();
    }

}
