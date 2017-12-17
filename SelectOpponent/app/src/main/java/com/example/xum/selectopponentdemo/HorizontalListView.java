package com.example.xum.selectopponentdemo;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Transformation;
import android.widget.ListAdapter;
import android.widget.Scroller;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by meng.xu on 2017/12/9.
 */
public class HorizontalListView extends ViewGroup implements GestureDetector.OnGestureListener {

    public static final int DEFAULT_SETTLEING_DURATION = 500;
    /*
    * Maximum rotation angle for handling z-axis translation
    */
    private static final int MAX_ROTATION_ANGLE = 60;
    private static final float PERCENTAGE = 100f;
    private static final int MAX_VISIBILITY = 3;
    private static final float MIN_ZOOM = 2.0f;
    private static final float MAX_ZOOM = 2.2f;
    private static final float VIEW_ALPHA = 0.3f;
    private static final OnItemSelectionListener DUMMY_SEL_LISTENER = new OnItemSelectionListener() {
        @Override
        public void onItemSelected(final ViewGroup parent, final View child, final int position, final long id) {
        }

        @Override
        public boolean isHorizontalScrollAllowed() {
            return true;
        }
    };
    private static final OnScrollListener DUMMY_SCROLL_LISTENER = new OnScrollListener() {
        @Override
        public void onScrollPositionChanged(final int position, final int min, final int max) {
        }
    };
    /*
     * Graphics class to handle matrix transformation
     */
    private final Camera mCamera;
    private final ViewRecycler mViewRecycler;
    private final Scroller mScroller;
    private final GestureDetector mGestureDetector;
    /*
     * Item selected listener
     */
    private OnItemSelectionListener mOnItemSelected;
    private OnScrollListener mOnScrollListener;
    /*
     * duration in which view will settle down
     */
    private int mSettleDownDuration;
    private boolean mIsScrolling = false;
    /*
     * Group adapter responsible for providing view
     */
    private ListAdapter mAdapter = null;
    /*
     * Index of right most view in view group
     */
    private int mRightViewIndex = 0;
    /*
     * New x-position after scroll
     */
    private int mNextXPosition = 0;
    /*
     * Visibility width of peripheral child
     */
    private int mWidthOfPeripheralChild = 0;
    /*
     * Maximum allowed scroll to right
     */
    private int mMaxX = Integer.MAX_VALUE;
    private int mLastSent = -1;
    private int mLastFlingX = 0;
    /*
     * Center of view group
     */
    private int mCenterOfGroup = 0;
    /*
     * Child view width
     */
    private int mChildWidth = 0;
    /*
     * Whether notify data set change called
     */
    private boolean mDataChanged = false;
    /**
     * Monitor data set changes
     */
    private final DataSetObserver mDataObserver = new DataSetObserver() {

        @Override
        public void onChanged() {
            mDataChanged = true;
            invalidate();
            requestLayout();
        }

        @Override
        public void onInvalidated() {
            reset();
            invalidate();
            requestLayout();
        }
    };
    private boolean mIsFreeScrollEnabled = false;

    private boolean mIsOnLayout = false;
    private boolean mCanSettle = false;
    private boolean mFirstTime = true;

    /**
     * Item Selection change listener interface
     */
    public interface OnItemSelectionListener {
        void onItemSelected(ViewGroup parent, View child, int position, long id);

        boolean isHorizontalScrollAllowed();
    }

    public interface OnScrollListener {
        void onScrollPositionChanged(final int position, final int min, final int max);
    }

    /**
     * This class is responsible for saving scroll indices to handle orientation
     * change
     */
    static class SavedState extends BaseSavedState {
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(final Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(final int size) {
                return new SavedState[size];
            }
        };
        private int mRightIndex = 0;
        private int mLeftIndex = 0;

        SavedState(final Parcelable superState) {
            super(superState);
        }

        private SavedState(final Parcel in) {
            super(in);
            mRightIndex = in.readInt();
            mLeftIndex = in.readInt();
        }

        @Override
        public void writeToParcel(@NonNull final Parcel out, final int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(mRightIndex);
            out.writeInt(mLeftIndex);
        }
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(final ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(final AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(final ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    public static class LayoutParams extends ViewGroup.LayoutParams {
        private int mViewType = 0;

        public LayoutParams(final Context c, final AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(final int w, final int h) {
            super(w, h);
        }

        public LayoutParams(final int w, final int h, final int viewType) {
            super(w, h);
            mViewType = viewType;
        }

        public LayoutParams(final ViewGroup.LayoutParams source) {
            super(source);
        }

        public int getViewType() {
            return mViewType;
        }
    }

    private static class ViewRecycleBinGroup {
        private final List<View> mScrapedViewList = new ArrayList<View>();

        public void clear() {
            mScrapedViewList.clear();
        }

        public View getView() {
            final int size = mScrapedViewList.size();
            if (size > 0) {
                return mScrapedViewList.remove(size - 1);
            }
            return null;
        }

        public void recycleView(final View view) {
            mScrapedViewList.add(view);
        }
    }

    private class ViewRecycler {
        private ViewRecycleBinGroup[] mScrapViews = null;

        /**
         * Number of view types in the list
         */
        @SuppressWarnings("unchecked")
        public void setViewTypeCount(final int viewTypeCount) {
            int typeCount = viewTypeCount;
            if (typeCount <= 0) {
                typeCount = 1;
            }
            final ViewRecycleBinGroup[] scrapViews = new ViewRecycleBinGroup[typeCount];
            for (int i = 0; i < typeCount; i++) {
                scrapViews[i] = new ViewRecycleBinGroup();
            }
            mScrapViews = scrapViews;
        }

        private View getView(final int position) {
            final int whichScrap = mAdapter.getItemViewType(position);
            if ((whichScrap >= 0) && (whichScrap < mScrapViews.length)) {
                return mScrapViews[whichScrap].getView();
            }
            return null;
        }

        private void addView(final View v) {
            if (mScrapViews != null) {
                final LayoutParams param = (LayoutParams)v.getLayoutParams();
                final int type = param.mViewType;
                mScrapViews[type].recycleView(v);
            }
        }

        private void clear() {
            if (mScrapViews != null) {
                for (final ViewRecycleBinGroup mScrapView : mScrapViews) {
                    mScrapView.clear();
                }
            }
        }
    }

    public HorizontalListView(final Context context) {
        super(context);
    }

    public HorizontalListView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public HorizontalListView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    {
        mCamera = new Camera();
        mViewRecycler = new ViewRecycler();
        mGestureDetector = new GestureDetector(getContext(), this);
        mScroller = new Scroller(getContext());
        mOnItemSelected = DUMMY_SEL_LISTENER;
        mOnScrollListener = DUMMY_SCROLL_LISTENER;
        mSettleDownDuration = DEFAULT_SETTLEING_DURATION;
        setStaticTransformationsEnabled(true);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static void setAlphaForView(final View view, final float zoomAmount) {
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
            float alpha = 1.0F - (zoomAmount / PERCENTAGE);
            if (alpha < VIEW_ALPHA) {
                alpha = VIEW_ALPHA;
            }
            view.setAlpha(alpha);
        }
    }

    /**
     * reset all the parameters used
     */
    private void resetParams() {
        mRightViewIndex = 0;
        mNextXPosition = mWidthOfPeripheralChild;
        mMaxX = Integer.MAX_VALUE;
    }

    /**
     * Set the selection change monitor
     *
     * @param listener - listener for selection change
     */
    public void setOnItemSelectedListener(final OnItemSelectionListener listener) {
        if (listener == null) {
            mOnItemSelected = DUMMY_SEL_LISTENER;
        } else {
            mOnItemSelected = listener;
        }
    }

    public void setOnScrollListener(final OnScrollListener scrollListener) {
        if (scrollListener == null) {
            mOnScrollListener = DUMMY_SCROLL_LISTENER;
        } else {
            mOnScrollListener = scrollListener;
        }
    }

    /**
     * Get the adapter
     *
     * @return - adapter set to this group
     */
    public ListAdapter getAdapter() {
        return mAdapter;
    }

    /**
     * Set the adapter to this view group
     *
     * @param adapter - to be set
     */
    public void setAdapter(final ListAdapter adapter) {
        if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(mDataObserver);
        }
        mAdapter = adapter;
        mViewRecycler.setViewTypeCount(mAdapter.getCount());
        mAdapter.registerDataSetObserver(mDataObserver);
        reset();
    }

    public void setSettleDownDuration(final int settleDownDuration) {
        mSettleDownDuration = settleDownDuration;
    }

    /**
     * Enable free scrolling that is, Fling
     */
    public void enableFreeScroll() {
        mIsFreeScrollEnabled = true;
    }

    /**
     * reset the view group
     */
    private void reset() {
        resetParams();
        removeAllViewsInLayout();
        requestLayout();
    }

    /**
     * Measure child's size specification
     *
     * @param child   - view
     * @param viewPos - index where to be added
     */
    private void addAndMeasureChild(final View child, final int viewPos, final int actualPosition) {
        LayoutParams params = (LayoutParams)child.getLayoutParams();
        if (params == null) {
            params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            params.mViewType = mAdapter.getItemViewType(actualPosition);
        }

        child.measure(MeasureSpec.makeMeasureSpec(mChildWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(
                getHeight() - child.getPaddingTop() - child.getPaddingTop(), MeasureSpec.EXACTLY));
        addViewInLayout(child, viewPos, params, true);
    }

    private void handleExpansion() {
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            int edge = child.getLeft();
            for (int i = 0; i < getChildCount(); i++) {
                child = getChildAt(i);
                child.measure(MeasureSpec.makeMeasureSpec(mChildWidth, MeasureSpec.EXACTLY), MeasureSpec
                        .makeMeasureSpec(getHeight() - child.getPaddingTop() - child.getPaddingTop(),
                                MeasureSpec.EXACTLY));
                child.layout(edge, child.getPaddingTop(), edge + child.getMeasuredWidth(), child.getPaddingTop()
                        + child.getMeasuredHeight());
                edge += child.getMeasuredWidth();
            }
        }
    }

    public void reMeasure() {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            child.measure(MeasureSpec.makeMeasureSpec(mChildWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(
                    getHeight() - child.getPaddingTop() - child.getPaddingTop(), MeasureSpec.EXACTLY));
        }
    }

    /**
     * Populate the views, until its visibility
     */
    private void populateList() {
        int edge = mWidthOfPeripheralChild;
        final View child = getChildAt(getChildCount() - 1);
        if (child != null) {
            edge = child.getRight();
        }
        populateListRight(edge);
        int peripheralChildWidth = mWidthOfPeripheralChild;
        final View childAt = getChildAt(0);
        if (childAt != null) {
            peripheralChildWidth = childAt.getLeft();
        }
        populateListLeft(peripheralChildWidth);
    }

    /**
     * populate child to right if there is gap in right side while moving to
     * left
     *
     * @param edge - current right edge
     */
    private void populateListRight(final int edge) {
        int rightEdge = edge;
        final int width = getWidth();
        final int count = mAdapter.getCount();
        final int childCount = getChildCount();
        while ((rightEdge < width) && (mRightViewIndex < count) && (childCount < MAX_VISIBILITY)) {
            final View child = mAdapter.getView(mRightViewIndex, mViewRecycler.getView(mRightViewIndex), this);
            addAndMeasureChild(child, -1, mRightViewIndex);
            child.layout(rightEdge, child.getPaddingTop(), rightEdge + child.getMeasuredWidth(), child.getPaddingTop()
                    + child.getMeasuredHeight());
            rightEdge += child.getMeasuredWidth();
            mRightViewIndex++;
        }
    }

    /**
     * populate child to left if there is gap in left side while moving to right
     *
     * @param edge - current left edge
     */
    private void populateListLeft(final int edge) {
        int leftEdge = edge;
        final int correction = getChildCount() + 1;
        while ((leftEdge > 0) && ((mRightViewIndex - correction) < getAdapter().getCount())
                && ((mRightViewIndex - correction) >= 0) && (getChildCount() < MAX_VISIBILITY)) {
            final View child = mAdapter.getView(mRightViewIndex - correction,
                    mViewRecycler.getView(mRightViewIndex - correction), this);
            addAndMeasureChild(child, 0, mRightViewIndex - correction);
            child.layout(leftEdge - child.getMeasuredWidth(), child.getPaddingTop(), leftEdge, child.getPaddingTop()
                    + child.getMeasuredHeight());
            leftEdge -= child.getMeasuredWidth();
        }
    }

    /**
     * Remove views if not visible
     */
    private void removeInvisibleViews() {
        View child = getChildAt(0);
        while ((child != null) && (child.getRight() <= 0)) {
            mViewRecycler.addView(child);
            removeViewInLayout(child);
            child = getChildAt(0);
        }
        View childAt = getChildAt(getChildCount() - 1);
        while ((childAt != null) && (childAt.getLeft() >= getRight())) {
            mViewRecycler.addView(childAt);
            removeViewInLayout(childAt);
            mRightViewIndex--;
            childAt = getChildAt(getChildCount() - 1);
        }
    }

    @Override
    public boolean dispatchTouchEvent(@NonNull final MotionEvent ev) {
        if ((ev.getAction() == MotionEvent.ACTION_UP) || (ev.getAction() == MotionEvent.ACTION_CANCEL)) {
            mCanSettle = true;
            settleDownViews();
        } else if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            mIsScrolling = false;
            mCanSettle = false;
        }
        boolean handled = false;
        if (mIsScrolling) {
            final MotionEvent newEvent = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_CANCEL, ev.getX(), ev.getY(), 21);
            super.dispatchTouchEvent(newEvent);
            newEvent.recycle();
            handled = mGestureDetector.onTouchEvent(ev);
        } else {
            handled = mGestureDetector.onTouchEvent(ev);
            super.dispatchTouchEvent(ev);
        }

        return handled;
    }

    /**
     * Handle child transformation
     */
    @Override
    protected boolean getChildStaticTransformation(final View child, final Transformation t) {
        final int childCenter = child.getLeft() + (child.getMeasuredWidth() / 2);
        final int childWidth = child.getMeasuredWidth();
        t.clear();
        t.setTransformationType(Transformation.TYPE_MATRIX);
        if (childCenter == mCenterOfGroup) {
            transformChild(child, t, 0);
        } else {
            int rotationAngle = (int)(((float)(mCenterOfGroup - childCenter) / (float)childWidth) * (float)MAX_ROTATION_ANGLE);
            if (Math.abs(rotationAngle) > MAX_ROTATION_ANGLE) {
                rotationAngle = (rotationAngle < 0) ? -MAX_ROTATION_ANGLE : MAX_ROTATION_ANGLE;
            }
            transformChild(child, t, rotationAngle);
        }
        return true;
    }

    @Override
    protected void onLayout(final boolean changed, final int left, final int top, final int right, final int bottom) {
        if (!mIsOnLayout) {
            mIsOnLayout = true;
            if (mAdapter == null) {
                return;
            }
            if (mDataChanged) {
                resetParams();
                mViewRecycler.clear();
                removeAllViewsInLayout();
                mDataChanged = false;
            }
            handleExpansion();
            removeInvisibleViews();
            populateList();
        }
        mIsOnLayout = false;
        if (mFirstTime) {
            mFirstTime = false;
            checkSelectionUpdate();
        }
    }

    private void settleDownViews() {
        if (!isRunning()) {
            int settlingDistance = 0;

            if (getChildCount() == MAX_VISIBILITY) {
                /*
                 * Middle child has occupied maximum visible area
                 */
                final View child = getChildAt(1);
                settlingDistance = mWidthOfPeripheralChild - child.getLeft();
            } else if (getChildCount() == 2) {
                final View child = getChildAt(1);
                final int childLeft = child.getLeft();
                if (childLeft < (getMeasuredWidth() / 2)) {
                    /*
                     * Right child has occupied maximum visible area
                     */
                    settlingDistance = mWidthOfPeripheralChild - childLeft;
                } else {
                    /*
                     * Left child has occupied maximum visible area
                     */
                    settlingDistance = (getMeasuredWidth() - mWidthOfPeripheralChild) - childLeft;
                }
            } else if (getChildCount() == 1) {
                final View child = getChildAt(0);
                final int childLeft = child.getLeft();
                if (childLeft < (getMeasuredWidth() / 2)) {
                    /*
                     * Right child has occupied maximum visible area
                     */
                    settlingDistance = mWidthOfPeripheralChild - childLeft;
                } else {
                    /*
                     * Left child has occupied maximum visible area
                     */
                    settlingDistance = (getMeasuredWidth() - mWidthOfPeripheralChild) - childLeft;
                }
            }
            if (settlingDistance != 0) {
                if (settlingDistance < 0) {
                    if (mNextXPosition != mMaxX) {
                        startScroll(settlingDistance, mSettleDownDuration);
                    } else {
                        final int childCount = getChildCount();
                        for (int i = 0; i < childCount; i++) {
                            final View child = getChildAt(i);
                            child.offsetLeftAndRight(settlingDistance);
                        }
                        removeInvisibleViews();
                        populateList();
                    }
                } else if (settlingDistance > 0) {
                    if (mNextXPosition != mWidthOfPeripheralChild) {
                        startScroll(settlingDistance, mSettleDownDuration);
                    } else {
                        final int childCount = getChildCount();
                        for (int i = 0; i < childCount; i++) {
                            final View child = getChildAt(i);
                            child.offsetLeftAndRight(settlingDistance);
                        }
                        removeInvisibleViews();
                        populateList();
                    }
                } else {
                    startScroll(settlingDistance, mSettleDownDuration);
                }
            }
        }
    }

    public void checkSelectionUpdate() {
        if (mChildWidth > 0) {
            final int selectedPos = mNextXPosition / mChildWidth;
            if (mLastSent != selectedPos) {
                View child = null;
                if (selectedPos == 0) {
                    child = getChildAt(0);
                } else {
                    child = getChildAt(1);
                }
                if (child != null) {
                    mLastSent = selectedPos;
                    mOnItemSelected.onItemSelected(this, child, mLastSent, -1L);
                }
            }
        }
    }

    public void resetSelectedPosition() {
        mLastSent = -1;
    }

    /**
     * Handle scrolling and populate views in gap
     *
     * @param delta - scrolled distance
     */
    private void handleScrolling(final int delta) {

        int deltaValue = delta;
        mNextXPosition += deltaValue;
        if (deltaValue < 0) {
            if (mNextXPosition < mWidthOfPeripheralChild) {
                /*
                 * Reached left boundary
                 */
                final int overScroll = (mWidthOfPeripheralChild - mNextXPosition);
                /*
                 * Over scrolled to left ,add to over scrolled next x
                 */
                mNextXPosition = mWidthOfPeripheralChild;
                deltaValue += overScroll;
            } else if (mNextXPosition == -mWidthOfPeripheralChild) {
                deltaValue = 0;
            }
        } else {
            if (mNextXPosition > mMaxX) {
                /*
                 * Reached right boundary
                 */
                final int overScroll = (mNextXPosition - mMaxX);
                /*
                 * Over scrolled to right ,deduct from over scrolled next x
                 */
                mNextXPosition = mMaxX;
                deltaValue -= overScroll;
            } else if (mNextXPosition == mMaxX) {
                deltaValue = 0;
            }
        }
        mOnScrollListener.onScrollPositionChanged(mNextXPosition, mWidthOfPeripheralChild, mMaxX);
        if (deltaValue != 0) {
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                child.offsetLeftAndRight(-deltaValue);
            }
            removeInvisibleViews();
            populateList();
        }
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            getChildAt(i).invalidate();
        }
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
        mCenterOfGroup = ((getWidth() - getPaddingLeft() - getPaddingRight()) / 2) + getPaddingLeft();
        super.onSizeChanged(w, h, oldw, oldh);

        final int currentIndex = getCurrentIndex();
        if ((oldw != 0) && (oldw != w) && (currentIndex != -1)) {
            scrollToIndex(currentIndex);
        }
    }

    @Override
    public void computeScroll() {
        final boolean more = mScroller.computeScrollOffset();
        final int currX = mScroller.getCurrX();
        final int delta = (mLastFlingX - currX) % getMeasuredWidth();
        if (more) {
            mLastFlingX = currX;
            handleScrolling(delta);
            ViewCompat.postInvalidateOnAnimation(this);
        } else {
            if (mCanSettle) {
                settleDownViews();
                checkSelectionUpdate();
            }
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        final SavedState ss = new SavedState(superState);
        int correction = 0;
        if ((mRightViewIndex >= MAX_VISIBILITY)) {
            correction = 1;
        }
        ss.mRightIndex = Math.abs(this.mRightViewIndex - (getChildCount() - correction));
        if (ss.mRightIndex < 0) {
            ss.mRightIndex = 0;
        }
        ss.mLeftIndex = (this.mRightViewIndex - MAX_VISIBILITY) + correction;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(final Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        final SavedState ss = (SavedState)state;
        super.onRestoreInstanceState(ss.getSuperState());
        this.mRightViewIndex = ss.mRightIndex;
        requestLayout();
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final Resources resource = getResources();

        mChildWidth = resource.getDimensionPixelSize(R.dimen.cc_flag_width);

        mWidthOfPeripheralChild = (getMeasuredWidth() - mChildWidth) / 2;

        if (mNextXPosition < mWidthOfPeripheralChild) {
            mNextXPosition = mWidthOfPeripheralChild;
        }
        if (!isInEditMode()) {
            mMaxX = ((mAdapter.getCount() - 1) * mChildWidth) + mWidthOfPeripheralChild;
        }
    }

    /**
     * Transform child matrix
     *
     * @param child         - child view to be transformed
     * @param t             - matrix
     * @param rotationAngle - rotation angle
     */
    private void transformChild(final View child, final Transformation t, final int rotationAngle) {
        mCamera.save();
        final Matrix imageMatrix = t.getMatrix();
        final int imageWidth = child.getMeasuredWidth();
        final int imageHeight = child.getMeasuredHeight();
        final float halfImageWidth = ((float)imageWidth / 2.0f);
        final float halfImageHeight = (float)imageHeight / 2.0f;
        final int rotation = Math.abs(rotationAngle);
        float zoom = MIN_ZOOM;
        if (rotationAngle == 0) {
            zoom = MAX_ZOOM;
        }
        final float zoomAmount = (float)rotation * zoom;
        setAlphaForView(child, zoomAmount);
        mCamera.translate(0.0f, 0.0f, zoomAmount);
        mCamera.getMatrix(imageMatrix);
        imageMatrix.preTranslate(-halfImageWidth, -halfImageHeight);
        imageMatrix.postTranslate(halfImageWidth, halfImageHeight);
        mCamera.restore();
    }

    private boolean isRunning() {
        return mScroller.computeScrollOffset();
    }

    private void startScroll(final int distance, final int duration) {
        if (distance != 0) {
            final int initialX = (distance < 0) ? Integer.MAX_VALUE : 0;
            mLastFlingX = initialX;
            mScroller.startScroll(initialX, 0, distance, 0, duration);
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    /**
     * Start the scroll using velocity
     *
     * @param initialVelocity - initial velocity
     */
    private void start(final int initialVelocity) {
        mScroller.forceFinished(true);
        final int initialX = (initialVelocity < 0) ? Integer.MAX_VALUE : 0;
        mLastFlingX = initialX;
        mScroller.fling(initialX, 0, initialVelocity, 0, 0, Integer.MAX_VALUE, 0, 0);
        ViewCompat.postInvalidateOnAnimation(this);
    }

    @Override
    public boolean onDown(final MotionEvent e) {
        mScroller.forceFinished(true);
        return true;
    }

    @Override
    public void onShowPress(final MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(final MotionEvent e) {
        return true;
    }

    @Override
    public boolean onScroll(final MotionEvent e1, final MotionEvent e2, final float distanceX, final float distanceY) {
        if (mOnItemSelected.isHorizontalScrollAllowed()) {
            final float absDistanceX = Math.abs(distanceX);
            final float absDistanceY = Math.abs(distanceY);
            if (absDistanceX > absDistanceY) {
                mIsScrolling = true;
                handleScrolling((int)distanceX);
            }
        }
        return true;
    }

    @Override
    public void onLongPress(final MotionEvent e) {
    }

    @Override
    public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float velocityX, final float velocityY) {
        if (mOnItemSelected.isHorizontalScrollAllowed()) {
            final int absVelocityX = (int)Math.abs(velocityX);
            final int absVelocityY = (int)Math.abs(velocityY);
            if (absVelocityX > absVelocityY) {
                if (mIsFreeScrollEnabled) {
                    start((int)velocityX);
                } else {
                    if (velocityX < 0) {
                        final int count = getChildCount();
                        for (int i = 0; i < count; i++) {
                            final View child = getChildAt(i);
                            final int left = child.getLeft();
                            if (left > mWidthOfPeripheralChild) {
                                final int scrollableArea = mWidthOfPeripheralChild - left;
                                mIsScrolling = true;
                                startScroll(scrollableArea, mSettleDownDuration);
                                break;
                            }
                        }
                    } else {
                        final int count = getChildCount();
                        for (int i = count - 1; i >= 0; i--) {
                            final View child = getChildAt(i);
                            final int right = child.getRight();
                            if (right < (mWidthOfPeripheralChild + mChildWidth)) {
                                final int scrollableArea = (mWidthOfPeripheralChild + mChildWidth) - right;
                                mIsScrolling = true;
                                startScroll(scrollableArea, mSettleDownDuration);
                                break;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    public void scrollToIndex(final int pos) {
        final ListAdapter adapter = getAdapter();
        if(adapter == null){
            return;
        }
        if (pos < adapter.getCount()) {
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View c = getChildAt(i);
                mViewRecycler.addView(c);
            }
            mRightViewIndex = pos;
            removeAllViewsInLayout();
            removeAllViews();
            mNextXPosition = ((pos) * mChildWidth) + mWidthOfPeripheralChild;
            populateList();
            final int count = getChildCount();
            for (int i = 0; i < count; i++) {
                getChildAt(i).invalidate();
            }
            mLastSent = -1;
            checkSelectionUpdate();
        }
    }

    public int getCurrentIndex() {
        return mNextXPosition / mChildWidth;
    }

    public boolean isIsScrolling() {
        return mIsScrolling;
    }
}
