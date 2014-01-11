package com.example.myapplication2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by wesj on 12/12/13.
 */
public class ColorSlice extends View {
    private Paint mPaint;

    public ColorSlice(Context context) {
        super(context);
        init(context);
    }

    public ColorSlice(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ColorSlice(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mPaint = new Paint();
        mPaint.setColor(Color.RED);
    }

    @Override
    public int getMinimumWidth() { return 50; }

    @Override
    public int getMinimumHeight() { return 50; }

    @Override
    public void onDraw(Canvas canvas) {
        canvas.drawRect(0, 0, 50, 50, mPaint);
    }
}
