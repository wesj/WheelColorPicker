package com.example.myapplication2;

import android.content.Context;
import android.graphics.LinearGradient;
import android.graphics.PointF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.util.AttributeSet;

public class SeekBar extends android.widget.SeekBar {
    private BorderedBox mThumb;
    private int[] mBackgroundColors;
    private int[] mGradientColors;

    public SeekBar(Context context) {
        super(context);
        init(context);
    }

    public SeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mThumb = new BorderedBox();
        mThumb.setBorderThickness(5);
        mThumb.setBorderRadius(1f);
        mThumb.setShadowOffset(new PointF(0, 2));
        mThumb.setIntrinsicWidth(65);
        mThumb.setIntrinsicHeight(65);
        setThumb(mThumb);
    }
    
    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh) {
    	super.onSizeChanged(w, h, oldw, oldh);
    	updateGradient();
    	updateBackgroundGradient();
    }

    public void setGradientColors(int[] colors) {
        mGradientColors = colors;
        updateGradient();
    }
    
    private void updateGradient() {
        if (mGradientColors == null || mGradientColors.length == 0)
            return;
        setGrad(getProgressDrawable(), mGradientColors);
        invalidate();
    }
    
    public void setBackgroundGradientColors(int[] colors) {
    	mBackgroundColors = colors;
    	updateBackgroundGradient();
    }
    
    private void updateBackgroundGradient() {
        if (mBackgroundColors == null || mBackgroundColors.length == 0)
            return;
        setGrad(getBackground(), mBackgroundColors);
        invalidate();
    }

    private void setGrad(Drawable d, int[] colors) {
        if (d == null)
            return;

        LinearGradient lg = new LinearGradient(0f, 0f, getWidth(), 0f, colors, null, Shader.TileMode.CLAMP);
        if (d instanceof BorderedBox)
            ((BorderedBox)d).setShader(lg);
        else if (d instanceof ShapeDrawable)
            ((ShapeDrawable)d).getPaint().setShader(lg);
    }

    public void setThumbColor(int color) {
        mThumb.setColor(color);
    }

}
