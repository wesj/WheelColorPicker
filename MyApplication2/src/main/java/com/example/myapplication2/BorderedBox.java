package com.example.myapplication2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * Created by wesj on 12/12/13.
 */
public class BorderedBox extends Drawable {
    private final Paint mShadowPaint;
    private final Paint mBorderPaint;
    private final Paint mCenter;
    private float mRadius = 0;
    private int mBorderWidth = 0;
    private PointF mShadowOffset = new PointF(0,0);
    private int mWidth = -1;
    private int mHeight = -1;
    private boolean mShadowInset;

    public BorderedBox(Context context) {
        mShadowPaint = new Paint();
        mShadowPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mShadowPaint.setColor(context.getResources().getColor(android.R.color.background_dark));
        mShadowPaint.setAntiAlias(true);

        mBorderPaint = new Paint();
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setColor(context.getResources().getColor(android.R.color.background_light));
        mBorderPaint.setAntiAlias(true);

        mCenter = new Paint();
        mCenter.setColor(Color.RED);
        mCenter.setAntiAlias(true);
    }
    
    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        RectF b = new RectF(bounds.left, bounds.top, bounds.right, bounds.bottom);

        int w = bounds.width()/2;
        int h = bounds.height()/2;
        float r = Math.min(w, h) * mRadius;
        h -= mShadowOffset.y;

        int state = canvas.save();
        RectF shadowBounds = new RectF(b);
        shadowBounds.offset(mShadowOffset.x, mShadowOffset.y);

        if (!mShadowInset)
            canvas.drawRoundRect(shadowBounds, r, r, mShadowPaint);

        canvas.drawRoundRect(b, r, r, mCenter);

        if (mShadowInset)
            canvas.drawRoundRect(shadowBounds, r, r, mShadowPaint);

        canvas.drawRoundRect(b, r, r, mBorderPaint);
        canvas.restoreToCount(state);
    }

    public void setBorderRadius(float r) { mRadius = r; }
    public void setShadowOffset(PointF p) { mShadowOffset = p; }

    public void setBorderThickness(int r) {
        mBorderPaint.setStrokeWidth(r);
        mShadowPaint.setStrokeWidth(r);
    }

    @Override
    public void setAlpha(int alpha) { }

    @Override
    public void setColorFilter(ColorFilter cf) { }

    @Override
    public int getIntrinsicWidth() { return mWidth; }
    @Override
    public int getIntrinsicHeight() { return mHeight; }
    public void setIntrinsicWidth(int width) { mWidth = width; }
    public void setIntrinsicHeight(int height) { mHeight = height; }

    @Override
    public int getOpacity() { return 0; }

    public void setColor(int color) { mCenter.setColor(color); }
    public void setShader(Shader shader) { mCenter.setShader(shader); }
    public void setBorderColor(int color) { mBorderPaint.setColor(color); }
    public void setShadowColor(int color) { mShadowPaint.setColor(color); }
    public void setShadowShader(Shader shader) { mShadowPaint.setShader(shader); }
    public void setShadowInset(boolean inset) { mShadowInset = inset; }
}
