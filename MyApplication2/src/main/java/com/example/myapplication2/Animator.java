package com.example.myapplication2;

import android.os.Build;
import android.view.View;

/**
 * Abstract class to animate a particular set of values on a view.
 */
abstract class Animator<T> {
    protected T mStart;
    protected T mEnd;
    protected long DURATION = 500;
    private long startTime = -1;
    private View mView;

    abstract protected void endAnimation();
    abstract protected void stepAnimation(float dt);

    public Animator(View v, T start, T end) {
        mView = v;
        mStart = start;
        mEnd = end;
    }

    public void start() {
        startTime = System.currentTimeMillis();
        step();
    }

    private void stop() {
        mStart = null;
        startTime = -1;
        endAnimation();
        mEnd = null;
    }

    public void step() {
        if (mEnd == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now > startTime + DURATION) {
            stop();
            return;
        }

        float dt = (float)(now - startTime);
        stepAnimation(dt/DURATION);

        if (Build.VERSION.SDK_INT >= 16) {
            mView.postInvalidateOnAnimation();
        } else {
            mView.postInvalidateDelayed(30/1000);
        }
    }
}