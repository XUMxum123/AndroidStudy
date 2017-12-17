package com.example.xum.tasks;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * Created by meng.xu on 2017/12/9.
 */

public class SelectionLayout extends LinearLayout {
    private float scale = 1.0f;
    private float opacity = 1.0f;

    public SelectionLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SelectionLayout(Context context) {
        super(context);
    }

    public void setScaleBoth(float scale)
    {
        this.scale = scale;
        this.invalidate();
    }

    public void setOpacity(float opacity) {
        this.opacity = opacity;
    }

}

