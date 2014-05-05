package com.example.myapplication2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;

/**
 * View group that lays out items in a cirlce. Dragging on the view will rotate the items around the circle
 */
@RemoteViews.RemoteView
public class Wheel extends ViewGroup {
    private static final String LOGTAG = "Wheel";

    // TODO: Make these DPI independent
    private float MAX_OFFSET = 40; // Number of pixels that selected view is "offset" by the path builder

    // The center of the wheel in percent of the wheel's width and height.
    private float mCy = 0.5f;
    private float mCx = 0.5f;

    private float mRotation = -180.0f + 360/20; // Current rotation
    private WheelListener mListener; // Listener to be notified when the wheel changes

    // TODO: These is pretty specific to the color picker.. Need a better solution
    private Paint mCenterPaint;// Paint a dot in the center of the view
    private int mInnerWidth;    // Width of an inner circle. Used to draw inner circle

    private double mStartDragRotation = 0; // The initial rotation when a user starts dragging
    private int mRadius; // The radius of slices. Determined by where the center is positioned

    private int mSliceAngle; // Cached total angle covered by a sliced
    private int mSelectedAngle; // Cached angle to show as "selected"
    private int mSliceHeight; // Cached height of a slice.

    private Animator mAnimator; // An animtor to use when snapping back to a selected position
    private PathBuilder mPathBuilder; // Builds a clip path for the view. By default views are clipped to a wedge

    // Listener for changes to the selected wedge
    public static interface WheelListener {
        public void onChange(int index);
    }

    // Interface for setting a clip on views.
    public static abstract class PathBuilder {
        protected float mCx;
        protected float mCy;

        public PathBuilder(float cx, float cy) {
            mCx = cx;
            mCy = cy;
        }

        public abstract Path getPath(float angle, int w, int h, float offset);

        public void setCenter(float cx, float cy) {
            mCx = cx;
            mCy = cy;
        }
    }

    // Implementation of a PathBuilder that creates a wedge
    public static class WedgeBuilder extends PathBuilder {
        private Path mPath;
        private Path mBasePath;
        private int mInnerRadius = 80;

        public WedgeBuilder(int innerRadius, int cx, int cy) {
            super(cx, cy);
            mInnerRadius = innerRadius;
        }

        public Path getPath(float angle, int w, int h, float offset) {
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
            int iw = (int) (mInnerRadius * 1.5f - offset);

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

            mBasePath.offset(mCx, mCy);
            return mBasePath;
        }
    }

    public Wheel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Wheel(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    private void init() {
        mCenterPaint = new Paint();
    }

    /**
     * Ask all children to measure themselves and compute the measurement of this
     * layout based on the children.
     */
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
                if (Build.VERSION.SDK_INT >= 11) {
                    childState = combineMeasuredStates(childState, child.getMeasuredState());
                }
            }
        }

        int w = 0;
        int h = 0;
        if (Build.VERSION.SDK_INT >= 16) {
            w = getMinimumWidth();
            h = getMinimumHeight();
        }

        // Check against our minimum height and width
        maxHeight = Math.max(maxHeight, h);
        maxWidth  = Math.max(maxWidth, w);
        setupRadius(maxWidth, maxHeight);
        maxHeight = maxWidth = mRadius;

         // Report our final dimensions.
        if (Build.VERSION.SDK_INT >= 11) {
            setMeasuredDimension(resolveSizeAndState(maxWidth,  widthMeasureSpec,  childState),
                                 resolveSizeAndState(maxHeight, heightMeasureSpec, childState << MEASURED_HEIGHT_STATE_SHIFT));
        } else {
            setMeasuredDimension(resolveSize(maxWidth, widthMeasureSpec),
                                 resolveSize(maxHeight, heightMeasureSpec));
        }
    }

    private void setupRadius(int width, int height) {
        int cx = (int) (mCx * width);
        int cy = (int) (mCy * height);

        int x = cx;
        if (mCx < 0.5) x = (int) ((1-mCx) * width);

        int y = cy;
        if (mCy < 0.5) y = (int) ((1-mCy) * height);
        mRadius = Math.min(x, y);
        MAX_OFFSET = mRadius*0.1f;
    }

    /**
     * Position all children within this layout.
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int count = getChildCount();

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
        mPathBuilder = new WedgeBuilder(mInnerWidth, cx, cy);

        mSliceHeight = (int) (mRadius/Math.atan(mSliceAngle/2));
        cy -= mSliceHeight;

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                child.layout(cx, cy, cx + mRadius, cy + 2*mSliceHeight);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                return startDrag(event);
            case MotionEvent.ACTION_MOVE:
                return drag(event);
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                return endDrag(event);
        }
        return false;
    }

    private boolean endDrag(MotionEvent event) {
        drag(event);

        // animate to the nearest color...
        int r = Math.round((mRotation - mSliceAngle/2)/mSliceAngle);
        mAnimator = new WheelAnimator(r * mSliceAngle + (mSelectedAngle % mSliceAngle) % 360);

        return true;
    }

    public void setSelectedSegment(int i) {
        int count = getChildCount();
        int angle = 360 / count;
        if (mAnimator != null)
            mAnimator.endAnimation();

        if (Build.VERSION.SDK_INT >= 11) {
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
        mAnimator = new WheelAnimator((180 - angle * i + angle / 2)%360);
    }

    public void setColor(int color) {
        mCenterPaint.setColor(color);
    }

    public void setCenter(float x, float y) {
        mCx = x;
        mCy = y;
        if (mPathBuilder != null) {
            mPathBuilder.setCenter(mCx, mCy);
        }
    }

    public void setPathBuilder(PathBuilder builder) {
        mPathBuilder = builder;
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

    private class WheelAnimator extends Animator<Float> {
        public WheelAnimator(float end) {
            super(Wheel.this, mRotation, end);
        }

        @Override
        protected void endAnimation() {
            mRotation = mEnd;
            notifyListeners();
            mAnimator = null;
            if (Build.VERSION.SDK_INT >= 11) {
                setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }
        }

        @Override
        protected void stepAnimation(float dt) {
            mRotation = mStart + (mEnd - mStart)* dt / 1000f;
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
        if (mAnimator != null)
            mAnimator.endAnimation();

        if (Build.VERSION.SDK_INT >= 11) {
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

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
        float a = (mRotation + mSliceAngle*i) % 360;
        canvas.rotate(mRotation + mSliceAngle * i, getMeasuredWidth() * mCx, getMeasuredHeight() * mCy);

        // TODO: This interface is too specific to our wedges
        if (mPathBuilder != null) {
            float offset = Math.max(0, MAX_OFFSET - (a- mSelectedAngle)*(a- mSelectedAngle)/mSliceAngle*4f);
            canvas.clipPath(mPathBuilder.getPath(mSliceAngle, (int) (mRadius - MAX_OFFSET), mSliceHeight, offset));
        }

        super.drawChild(canvas, child, drawingTime);

        canvas.restoreToCount(state);
        if (mAnimator != null) {
            mAnimator.step();
        }

        return true;
    }

}