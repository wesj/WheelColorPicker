package com.example.myapplication2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by wesj on 12/12/13.
 */
public class Slice extends View {

    private static final String LOGTAG = "Slice";
    private static Path mPath;
    private static Paint mPaint = new Paint();
    private static Paint mInnerRing = new Paint();
    private static int mStrokeWidth = 40;
    private static int mInnerWidth = 120;
    private float mAngle;
    private float mRadius;
    private float mOffset;

    public Slice(Context context) {
        super(context);
        init(context);
    }

    public Slice(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public Slice(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mInnerRing.setColor(Color.argb(20, 0, 0, 0));
        mInnerRing.setStrokeWidth(mStrokeWidth);
        mInnerRing.setStyle(Paint.Style.FILL);
    }

    protected void onConfigurationChanged (Configuration newConfig) {
        mPath = null;
    }

    @SuppressLint("NewApi")
    public void draw(Canvas canvas) {
        Drawable d = getBackground();
        if (d instanceof ColorDrawable) {
            mPaint.setColor( ((ColorDrawable) d).getColor() );
        }

        int w = getWidth();

        int save = canvas.save();
        canvas.drawRect(0, 0, getMeasuredWidth(), getMeasuredHeight(), mPaint);
        canvas.drawCircle(0, getHeight()/2, mInnerWidth + mStrokeWidth, mInnerRing);

        canvas.restoreToCount(save);
    }

    @SuppressLint("NewApi")
    public void setParams(float angle, float r, float offset) {
        mAngle = angle;
        mRadius = r;
        if (mOffset != offset) {
            mOffset = offset;
        }
    }
}
