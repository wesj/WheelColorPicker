package com.example.myapplication2;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;

/**
 * Example of writing a custom layout manager.  This is a fairly full-featured
 * layout manager that is relatively general, handling all layout cases.  You
 * can simplify it for more specific cases.
 */
@RemoteViews.RemoteView
public class Wheel extends ViewGroup {
    private static final String LOGTAG = "Wheel";
    private float MAX_OFFSET = 40;
    private static final int MIN_RADIUS = 100;

    private int mInnerWidth = 80;
    private Path mPath;
    private float mCy = 0.5f;
    private float mCx = 0.5f;
    private float mRotation = -180.0f + 360/20;
    private WheelListener mListener;
    private Paint mCenterPaint;
    private Path mBasePath;

    private double mStartDragRotation = 0;
    private int mRadius;
    private int mSliceAngle;
    private int mSelectedAngle;
    private int mSliceHeight;

    private float endRotation = 0;
    private float angVelocity = 0;
    private long prevTime = 0;

    public Wheel(Context context) {
        this(context, null);
    }

    public Wheel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Wheel(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    /**
     * Any layout manager that doesn't scroll will want this.
     */
    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @SuppressLint("NewApi")
    private void init(Context context) {
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mCenterPaint = new Paint();
    }

    /**
     * Ask all children to measure themselves and compute the measurement of this
     * layout based on the children.
     */
    @SuppressLint("NewApi")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = getChildCount();

        // Measurement will ultimately be computing these values.
        int maxHeight = 0;
        int maxWidth = 0;
        int childState = 0;

        // Iterate through all children, measuring them and computing our dimensions
        // from their size.
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                // Measure the child.
                measureChild(child, widthMeasureSpec, heightMeasureSpec);
                maxWidth = Math.max(maxWidth, child.getMeasuredWidth());
                maxHeight = Math.max(maxHeight, child.getMeasuredHeight());
                childState = combineMeasuredStates(childState, child.getMeasuredState());
            }
        }

        // Check against our minimum height and width
        maxHeight = Math.max(maxHeight, getMinimumHeight());
        maxWidth  = Math.max(maxWidth, getMinimumWidth());
        setupRadius(maxWidth, maxHeight);
        maxHeight = maxWidth = mRadius;

         // Report our final dimensions.
        setMeasuredDimension(resolveSizeAndState(maxWidth,  widthMeasureSpec,  childState),
                             resolveSizeAndState(maxHeight, heightMeasureSpec, childState << MEASURED_HEIGHT_STATE_SHIFT));
    }

    private void setupRadius(int width, int height) {
        int cx = (int) (mCx * width);
        int cy = (int) (mCy * height);

        int x = cx;
        if (mCx < 0.5) x = (int) ((1-mCx) * width);

        int y = cy;
        if (mCy < 0.5) y = (int) ((1-mCy) * height);
        mRadius = Math.max(Math.min(x, y), MIN_RADIUS);
        MAX_OFFSET = mRadius*0.1f;
    }

    /**
     * Position all children within this layout.
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        mPath = null;
        mBasePath = null;

        final int count = getChildCount();

        // These are the far left and right edges in which we are performing layout.
        int leftPos = getPaddingLeft();
        int rightPos = right - left - getPaddingRight();

        // This is the middle region inside of the gutter.
        final int middleLeft = leftPos;
        final int middleRight = rightPos;

        // These are the top and bottom edges in which we are performing layout.
        final int parentTop = getPaddingTop();
        final int parentBottom = bottom - top - getPaddingBottom();

        mSliceAngle = 360 / count;
        int w = getMeasuredWidth();
        int h = getMeasuredHeight();
        int cx = (int) (mCx * w);
        int cy = (int) (mCy * h);
        setupRadius(w, h);

        mSelectedAngle = (int) (Math.atan((mCy-0.5)/(mCx-0.5))*-180/Math.PI);
        while (mSelectedAngle < 0) mSelectedAngle += 360;
        if (mCx > 0.5) mSelectedAngle += 180;

        mInnerWidth = (int) (mRadius * 0.2f);

        mSliceHeight = (int) (mRadius/Math.atan(mSliceAngle/2));
        cy -= (int) (mSliceHeight);

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                child.layout(cx, cy, cx + mRadius, cy + 2*mSliceHeight);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN) {
            return startDrag(event);
        } else if (action == MotionEvent.ACTION_MOVE) {
            return drag(event);
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            return endDrag(event);
        }
        return false;
    }

    private boolean endDrag(MotionEvent event) {
        drag(event);

        // animate to the nearest color...
        int r = Math.round((mRotation - mSliceAngle/2)/mSliceAngle);
        animateTo(r * mSliceAngle + (mSelectedAngle % mSliceAngle));

        return true;
    }

    private Path getPath(float angle, int w, int h, float offset) {
        if (offset == 0) {
            if (mPath == null) {
                mPath = new Path(buildPath(angle, w, h, offset));
            }
            return mPath;
        }

        Path p = buildPath(angle, w, h, offset);
        p.offset(offset, 0);
        return p;
    }

    private Path buildPath(float angle, int w, int h, float offset) {
        if (mBasePath == null) {
            mBasePath = new Path();
        }

        mBasePath.rewind();
        angle /= 2;
        double a = angle * Math.PI/180f;
        int iw = (int) (mInnerWidth * 1.5f - offset);

        double cos = Math.cos(a);
        double sin = Math.sin(a);
        mBasePath.moveTo((int) (iw * cos),
                         (int) (iw * sin));
        mBasePath.lineTo((int) (w * cos),
                         (int) (w * sin));
        mBasePath.arcTo(new RectF(-w, -w, w, w), angle, -2f * angle);
        mBasePath.lineTo((int)     (iw * cos),
                         (int) (-1* iw * sin));
        mBasePath.arcTo(new RectF(-iw, -iw, iw, iw), -angle, 2f * angle);
        mBasePath.close();

        mBasePath.offset((int) (mCx * getMeasuredWidth()),
                         (int) (mCy * getMeasuredHeight()));
        return mBasePath;
    }

    @SuppressLint("NewApi")
    private void animateTo(float rotation) {
        endRotation = rotation % 360;
        angVelocity = ((mRotation - rotation > 0) ? -1f : 1f) * 50;
        prevTime = System.currentTimeMillis();

        postInvalidateOnAnimation();
    }

    private void endAnimation() {
        endRotation = 0;
        angVelocity = 0;
        prevTime = 0;
    }

    public void setSelectedSegment(int i) {
        int count = getChildCount();
        int angle = 360 / count;
        animateTo(180 - angle * i + angle / 2);
    }

    public void setColor(int color) {
        mCenterPaint.setColor(color);
    }

    public void setCenter(float x, float y) {
        mCx = x;
        mCy = y;
    }

    public static interface WheelListener {
        public void onChange(int index);
    }

    public void addListener(WheelListener listener) {
        mListener = listener;
    }

    public int getSelectedIndex() {
        int count = getChildCount();
        int index = (int) (Math.floor((mSelectedAngle - mRotation)/mSliceAngle));
        while (index < 0) index += count;
        while (index >= count) index -= count;
        return index;
    }

    public void notifyListeners() {
        if (mListener != null) {
            mListener.onChange(getSelectedIndex());
        }
    }

    @SuppressLint("NewApi")
    private void animateStep() {
        if (angVelocity != 0f) {
            long now = System.currentTimeMillis();
            long dt = now - prevTime;
            mRotation += angVelocity * dt / 1000f;
            if ((mRotation-endRotation) * angVelocity >= 0) {
                mRotation = endRotation;
                endAnimation();
                notifyListeners();
            } else {
                prevTime = now;
            }
            postInvalidateOnAnimation();
        }
    }

    private boolean drag(MotionEvent event) {
        double a = getAngle(event);
        mRotation += mStartDragRotation - a;
        while (mRotation < 0) {
            mRotation += 360;
        }
        while (mRotation > 360) {
            mRotation -= 360;
        }
        mStartDragRotation = a;
        invalidate();
        return true;
    }

    private boolean startDrag(MotionEvent event) {
        endAnimation();
        mStartDragRotation = getAngle(event);
        return true;
    }

    private double getAngle(MotionEvent event) {
        double val = Math.atan((event.getX() - getMeasuredWidth() * mCx) / (event.getY() - getMeasuredHeight() * mCy)) * 180/ Math.PI;
        if (event.getY() > getHeight() * mCy) {
            return val - 180;
        }
        return val;
    }

    @Override
    public void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        int state = canvas.save();
        canvas.drawCircle((getMeasuredWidth() * mCx), (getMeasuredHeight() * mCy), mInnerWidth, mCenterPaint);
        canvas.restoreToCount(state);
    }

    public boolean drawChild(Canvas canvas, View child, long drawingTime) {
        int i = 0;
        int count = getChildCount();
        for (i = 0; i < count; i++) {
            if (getChildAt(i) == child) {
                break;
            }
        }

        int state = canvas.save();

        // TODO: Angle, W, and H are constants. Move them out of here
        float a = (mRotation + mSliceAngle*i) % 360;
        float offset = Math.max(0, MAX_OFFSET - (a- mSelectedAngle)*(a- mSelectedAngle)/mSliceAngle*4f);

        canvas.rotate(mRotation + mSliceAngle * i, getMeasuredWidth() * mCx, getMeasuredHeight() * mCy);
        canvas.clipPath(getPath(mSliceAngle, (int) (mRadius - MAX_OFFSET), mSliceHeight, offset));

        super.drawChild(canvas, child, drawingTime);

        canvas.restoreToCount(state);
        animateStep();

        return true;
    }

}