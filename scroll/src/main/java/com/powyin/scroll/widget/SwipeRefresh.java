package com.powyin.scroll.widget;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ScrollView;

import com.powyin.scroll.R;


/**
 * Created by powyin on 2016/7/10.       当swipeRefresh 子View没有足够空间滑动时 上拉加载自动关闭
 */
public class SwipeRefresh extends ViewGroup implements NestedScrollingParent, ISwipe {


    private int mTouchSlop;
    private final NestedScrollingParentHelper mParentHelper;
    private boolean mNestedScrollInProgress = false;
    private int mActivePointerId = -1;                                             //多手指移动中取值ID

    private float mDragBeginY;                                                      //DispatchTouchEvent
    private float mDragBeginDirect;                                                 //InterceptTouchEvent
    private float mDragLastY;                                                       //TouchEvent

    private boolean mIsTouchEventMode = false;                                      //DispatchTouchEvent  是否在进行TouchEvent传递
    private boolean mPreScroll;                                                     //DispatchTouchEvent  是否预滚动
    private boolean mDraggedDispatch;                                               //DispatchTouchEvent  已经打断
    private boolean mDraggedIntercept;                                              //InterceptTouchEvent 打断

    private SwipeControl mSwipeControl;                                             //刷新头部控制器
    private ValueAnimator animationReBackToRefreshing;                              //滚动 显示正在刷新状态
    private ValueAnimator animationReBackToTop;                                     //滚动 回到正常显示

    private boolean mRefreshStatusContinueRunning = false;                          //下拉刷新 正在刷新
    private FreshStatus mFreshStatus = FreshStatus.CONTINUE;                        //下拉刷新状态

    private boolean mLoadedStatusContinueRunning = false;                           //上拉加载 正在加载
    private ISwipe.LoadedStatus mLoadedStatus = LoadedStatus.CONTINUE;              //下拉刷新状态;


    private SwipeControl.SwipeModel mModel = SwipeControl.SwipeModel.SWIPE_BOTH;    //刷新模式设置

    private OnRefreshListener mOnRefreshListener;
    private OnStatusListener mOnStatusListener;

    private View mViewTop;
    private View mViewBottom;
    private View mTargetView;

    private int contentScroll;
    private int overScrollTop;
    private int overScrollBottom;

    public SwipeRefresh(Context context) {
        this(context, null);
    }

    public SwipeRefresh(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwipeRefresh(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SwipeRefresh);
            int modelIndex = a.getInt(R.styleable.SwipeRefresh_fresh_model, -1);
            if (modelIndex >= 0 && modelIndex < SwipeControl.SwipeModel.values().length) {
                mModel = SwipeControl.SwipeModel.values()[modelIndex];
            }
            a.recycle();
        }

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mParentHelper = new NestedScrollingParentHelper(this);
        initSwipeControl();
    }

//    private void ensureTarget() {
//        if (mTargetView == null) {
//            for (int i = 0; i < getChildCount(); i++) {
//                View child = getChildAt(i);
//                if (child != mViewTop && child != mViewBottom) {
//                    mTargetView = child;
//                }
//            }
//
//            mTargetView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
//                @Override
//                public void onScrollChanged() {
//                    if (!mNestedScrollInProgress && !mLoadedStatusContinueRunning &&
//                            !mDraggedDispatch && !mDraggedIntercept && mOnRefreshListener != null &&
//                            !canChildScrollUp() && canChildScrollDown()) {
//                        mLoadedStatusContinueRunning = true;
//                        mOnRefreshListener.onLoading(false);
//                    }
//                }
//            });
//
//        }
//    }


    private void initSwipeControl() {
        mSwipeControl = new SwipeControlStyleNormal(getContext());
        mViewTop = mSwipeControl.getSwipeHead();
        mViewBottom = mSwipeControl.getSwipeFoot();
        addView(mViewTop, 0);
        addView(mViewBottom, getChildCount());
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthTarget = MeasureSpec.getSize(widthMeasureSpec);
        final int heightTarget = MeasureSpec.getSize(heightMeasureSpec);

        final int childWidMeasure = MeasureSpec.makeMeasureSpec(widthTarget, MeasureSpec.EXACTLY);
        final int spHei = MeasureSpec.makeMeasureSpec(heightTarget, MeasureSpec.AT_MOST);

        for (int i = 0; i < getChildCount(); ++i) {
            final View child = getChildAt(i);
            if (child.getVisibility() == View.GONE) {
                continue;
            }
            if (mViewTop == child || mViewBottom == child) continue;

            child.measure(childWidMeasure, MeasureSpec.makeMeasureSpec(heightTarget, MeasureSpec.EXACTLY));
        }

        if (mViewTop != null) {
            mViewTop.measure(childWidMeasure, spHei);
        }
        if (mViewBottom != null) {
            mViewBottom.measure(childWidMeasure, spHei);
        }

        setMeasuredDimension(widthTarget, heightTarget);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

        if (getChildCount() > 3) {
            throw new RuntimeException("only one View is support");
        }

        contentScroll = 0;
        overScrollTop = mViewTop.getMeasuredHeight();
        overScrollBottom = mViewBottom.getMeasuredHeight();



        for(int i=0;i<getChildCount();i++){
            View child = getChildAt(i);
            if(child == mViewTop || child == mViewBottom) continue;
            child.layout(0, 0, right - left, bottom - top);
        }

        mViewTop.layout(left, -mViewTop.getMeasuredHeight(), right, 0);

        mViewBottom.layout(0, bottom - top, right - left, bottom - top + overScrollBottom);


        if (mModel == SwipeControl.SwipeModel.SWIPE_NONE || mModel == SwipeControl.SwipeModel.SWIPE_ONLY_LOADINN) {
            overScrollTop = 0;
        }

        if (mModel == SwipeControl.SwipeModel.SWIPE_NONE || mModel == SwipeControl.SwipeModel.SWIPE_ONLY_REFRESH) {
            overScrollBottom = 0;
        }

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            mDraggedIntercept = false;
            mPreScroll = false;
            mDraggedDispatch = false;
            mActivePointerId = ev.getPointerId(0);
            mDragBeginY = (int) ev.getY();
            mDragBeginDirect = 0;
            mIsTouchEventMode = true;
            mDragLastY = mDragBeginY;

            mTargetView = null;
            int x = (int) ev.getX();
            int y = (int) ev.getY();
            for (int i = 0; i < getChildCount(); i++) {
                int scrollY = getScrollY();
                View child = getChildAt(i);
                boolean isSelect = !(y < child.getTop() - scrollY || y >= child.getBottom() - scrollY || x < child.getLeft() || x >= child.getRight());
                if (isSelect) {
                    mTargetView = child;
                }
            }
        }

        if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
            mIsTouchEventMode = false;
        }

        if (mNestedScrollInProgress) {
            return super.dispatchTouchEvent(ev);
        }

        if (ev.getAction() == MotionEvent.ACTION_DOWN && getScrollY() != 0) {
            mPreScroll = true;
        }

        if (mPreScroll) {
            int action = MotionEventCompat.getActionMasked(ev);
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                    mDragBeginY = ev.getY(ev.findPointerIndex(mActivePointerId));
                    mDragLastY = mDragBeginY;
                    break;
                case MotionEventCompat.ACTION_POINTER_DOWN: {
                    int pointerIndex = MotionEventCompat.getActionIndex(ev);
                    mActivePointerId = ev.getPointerId(pointerIndex);
                    mDragBeginY = ev.getY(ev.findPointerIndex(mActivePointerId));
                    mDragLastY = mDragBeginY;
                    break;
                }
                case MotionEvent.ACTION_MOVE:
                    float y = ev.getY(ev.findPointerIndex(mActivePointerId));
                    float yDiff = mDragBeginY - y;
                    if (Math.abs(yDiff) > mTouchSlop / 2 && !mDraggedDispatch) {
                        mDraggedDispatch = true;
                        mDragLastY = y;
                    }

                    if (mDraggedDispatch) {
                        offSetScroll((int) (mDragLastY - y), true);
                        if (getScrollY() == 0) {
                            mPreScroll = false;
                        }
                        mDragLastY = y;
                        return true;
                    }
                    break;
                case MotionEventCompat.ACTION_POINTER_UP:
                    onSecondaryPointerUp(ev);
                    break;
            }
        }

        if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
            if (tryBackToRefreshing() || tryBackToFreshFinish() || mDraggedDispatch || mDraggedIntercept) {
                ev.setAction(MotionEvent.ACTION_CANCEL);
            }
        }

        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mNestedScrollInProgress || !isEnabled() || (canChildScrollDown() && canChildScrollUp())) {
            return false;
        }

        final int action = MotionEventCompat.getActionMasked(ev);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = ev.getPointerId(0);
                mDragBeginY = ev.getY(ev.findPointerIndex(mActivePointerId));
                mDragLastY = mDragBeginY;
                break;

            case MotionEventCompat.ACTION_POINTER_DOWN: {
                int pointerIndex = MotionEventCompat.getActionIndex(ev);
                mActivePointerId = ev.getPointerId(pointerIndex);
                mDragBeginY = ev.getY(ev.findPointerIndex(mActivePointerId));
                mDragLastY = mDragBeginY;
                break;
            }


            case MotionEvent.ACTION_MOVE:

                float y = ev.getY(ev.findPointerIndex(mActivePointerId));


                final float yDiff = mDragBeginY - y;


                if (!isSameDirection(mDragBeginDirect, -yDiff)) {
                    mDragBeginDirect = -yDiff;
                    mDragBeginY = y;
                    return false;
                }


                if (Math.abs(yDiff) > mTouchSlop && !mDraggedIntercept &&
                        ((yDiff > 0 && !canChildScrollUp()) || (yDiff < 0 && !canChildScrollDown()))) {                //头部 与尾巴自动判断
                    mDraggedIntercept = true;
                    mDragLastY = y;
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }


                break;
            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
        }

        return mDraggedIntercept;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        int pointerIndex;
        switch (MotionEventCompat.getActionMasked(ev)) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = ev.getPointerId(0);
                mDragBeginY = ev.getY(ev.findPointerIndex(mActivePointerId));
                mDragLastY = (int) ev.getY();
                break;
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                pointerIndex = MotionEventCompat.getActionIndex(ev);
                mActivePointerId = ev.getPointerId(pointerIndex);
                mDragBeginY = ev.getY(ev.findPointerIndex(mActivePointerId));
                mDragLastY = mDragBeginY;
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                float y = ev.getY(ev.findPointerIndex(mActivePointerId));

                int deltaY = (int) mDragBeginY - (int) y;
                if (!mDraggedIntercept && Math.abs(deltaY) > mTouchSlop) {
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                    mDraggedIntercept = true;
                }
                if (mDraggedIntercept) {
                    offSetScroll((int) (mDragLastY - y), false);
                }
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
        }

        pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
        if (pointerIndex >= 0) {
            mDragLastY = MotionEventCompat.getY(ev, pointerIndex);
        }

        return true;
    }

    @Override
    public void addView(View child, int index, LayoutParams params) {
        if(getChildCount()>=3){
            throw new RuntimeException("only one View is support");
        }
        super.addView(child, index, params);
    }

    private boolean isSameDirection(float arg1, float arg2) {
        return arg1 == 0 || (arg1 > 0 && arg2 > 0) || (arg1 < 0 && arg2 < 0);
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = (ev.getAction() & MotionEventCompat.ACTION_POINTER_INDEX_MASK) >>
                MotionEventCompat.ACTION_POINTER_INDEX_SHIFT;
        final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if (pointerId == mActivePointerId) {
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mDragLastY = (int) ev.getY(newPointerIndex);
            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
        }
    }


    private boolean canChildScrollDown() {
        return mTargetView!=null && ViewCompat.canScrollVertically(mTargetView, -1);
    }

    private boolean canChildScrollUp() {
        return mTargetView !=null && ViewCompat.canScrollVertically(mTargetView, 1);
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        if (mTargetView == null || mTargetView instanceof NestedScrollingChild) {
            super.requestDisallowInterceptTouchEvent(b);
        }
    }

    //--------------------------------------------- NestedScrollingParent-----------------------------------------------------//

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int nestedScrollAxes) {
        mTargetView = target;
        mParentHelper.onNestedScrollAccepted(child, target, nestedScrollAxes);
        mNestedScrollInProgress = true;
        stopAllScroll();
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        int delta = offSetScroll(dy, true);
        consumed[1] = delta;
    }

    @Override
    public int getNestedScrollAxes() {
        return mParentHelper.getNestedScrollAxes();
    }


    @Override
    public void onStopNestedScroll(View target) {
        mParentHelper.onStopNestedScroll(target);
        mNestedScrollInProgress = false;
        if (!tryBackToRefreshing()) {
            tryBackToFreshFinish();
        }

    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        offSetScroll(dyUnconsumed, false);
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {

        return false;
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {

        return getScrollY() != 0;
    }


    private int offSetScroll(int deltaOriginY, boolean pre) {
        int deltaY = deltaOriginY;
        if (deltaOriginY == 0) return 0;

        int middleHei = -overScrollTop != 0 ? -overScrollTop + mSwipeControl.getOverScrollHei() : 0;
        int currentScrollY = getScrollY();
        if (deltaOriginY < 0 && currentScrollY < middleHei) {                                                                         //过度拉伸 阻尼效果
            deltaY = (int) (deltaY * Math.pow((mSwipeControl.getOverScrollHei() - (middleHei - currentScrollY)) * 1f
                    / mSwipeControl.getOverScrollHei(), 2));
        }

        if (!(currentScrollY == 0 || currentScrollY == contentScroll) || !pre) {
            int willTo = currentScrollY + deltaY;
            willTo = Math.min(willTo, overScrollBottom);
            willTo = Math.max(willTo, -overScrollTop);

            if ((currentScrollY > 0 && willTo < 0) || (currentScrollY < 0 && willTo > 0)) {                                                   //确保scroll值经过0
                willTo = 0;
            }

            if ((currentScrollY > contentScroll && willTo < contentScroll) || (currentScrollY < contentScroll && willTo > contentScroll)) {   //确保scroll值经过scrollContent
                willTo = contentScroll;
            }

            if (mDragBeginDirect > 0 && willTo > contentScroll) {                                                      //确保上拉刷新独立
                willTo = contentScroll;
            }
            if (mDragBeginDirect < 0 && willTo < 0) {                                                                  //确保下拉加载独立
                willTo = 0;
            }


            if (willTo > 0 && !canChildScrollDown()) {                                                                                          //确保当mTarget没有足够内容进行独立滑动时 上拉加载不启动
                willTo = 0;
            }

            if (willTo == currentScrollY) {
                return deltaOriginY;
            }

            scrollTo(0, willTo);


            // ----------------------------------------------------------------------------------------------------------------》》下拉
            if (0 > willTo && willTo > middleHei && (mFreshStatus == ISwipe.FreshStatus.ERROR || mFreshStatus == FreshStatus.ERROR_NET
                    || mFreshStatus == ISwipe.FreshStatus.SUCCESS) && !mRefreshStatusContinueRunning) {                                              //重置下拉刷新状态
                mFreshStatus = ISwipe.FreshStatus.CONTINUE;
                if (mOnStatusListener != null) {
                    mOnStatusListener.onFreshStatue(mFreshStatus);
                }
            }
            if (0 > willTo) {                                                                                                                     //刷新下拉状态
                int swipeViewVisibilityHei = 0 - willTo;
                switch (mFreshStatus) {
                    case CONTINUE:
                        if (mRefreshStatusContinueRunning) {
                            mSwipeControl.onSwipeStatue(SwipeControl.SwipeStatus.SWIPE_HEAD_LOADING, swipeViewVisibilityHei, mViewTop.getHeight());
                        } else if (willTo < middleHei) {
                            mSwipeControl.onSwipeStatue(SwipeControl.SwipeStatus.SWIPE_HEAD_OVER, swipeViewVisibilityHei, mViewTop.getHeight());
                        } else {
                            mSwipeControl.onSwipeStatue(SwipeControl.SwipeStatus.SWIPE_HEAD_TOAST, swipeViewVisibilityHei, mViewTop.getHeight());
                        }
                        break;
                    case SUCCESS:
                        mSwipeControl.onSwipeStatue(SwipeControl.SwipeStatus.SWIPE_HEAD_COMPLETE_OK, swipeViewVisibilityHei, mViewTop.getHeight());
                        break;
                    case ERROR:
                        mSwipeControl.onSwipeStatue(SwipeControl.SwipeStatus.SWIPE_HEAD_COMPLETE_ERROR, swipeViewVisibilityHei, mViewTop.getHeight());
                        break;
                    case ERROR_NET:
                        mSwipeControl.onSwipeStatue(SwipeControl.SwipeStatus.SWIPE_HEAD_COMPLETE_ERROR_NET, swipeViewVisibilityHei, mViewTop.getHeight());
                        break;
                }
            }
            // ----------------------------------------------------------------------------------------------------------------《《 下拉


            // ---------------------------------------------------------------------------------------------------------------- 》》上拉
            if (willTo > contentScroll && !mLoadedStatusContinueRunning) {                                                                                     //重置上拉刷新状态
                mLoadedStatusContinueRunning = true;
                if (mOnRefreshListener != null) {
                    mOnRefreshListener.onLoading(true);
                }
            }
            if (willTo > contentScroll) {
                if (mLoadedStatus == ISwipe.LoadedStatus.NO_MORE) {
                    mSwipeControl.onSwipeStatue(SwipeControl.SwipeStatus.SWIPE_LOAD_NO_MORE, willTo - contentScroll, mViewBottom.getHeight());
                } else if (mLoadedStatus == ISwipe.LoadedStatus.ERROR) {
                    mSwipeControl.onSwipeStatue(SwipeControl.SwipeStatus.SWIPE_LOAD_ERROR, willTo - contentScroll, mViewBottom.getHeight());
                } else if (mLoadedStatus == ISwipe.LoadedStatus.CONTINUE) {
                    mSwipeControl.onSwipeStatue(SwipeControl.SwipeStatus.SWIPE_LOAD_LOADING, willTo - contentScroll, mViewBottom.getHeight());
                }
            }
            // ---------------------------------------------------------------------------------------------------------------- 《《上拉
            return (deltaOriginY);
        }
        return 0;
    }

    private boolean tryBackToRefreshing() {
        if (mIsTouchEventMode || mFreshStatus == FreshStatus.SUCCESS || mFreshStatus == FreshStatus.ERROR || mFreshStatus == FreshStatus.ERROR_NET)
            return false;
        int scrollY = getScrollY();
        int middleHei = overScrollTop != 0 ? -overScrollTop + mSwipeControl.getOverScrollHei() : 0;
        boolean isOverProgress = scrollY < middleHei;
        if (isOverProgress) {
            stopAllScroll();
            animationReBackToRefreshing = ValueAnimator.ofInt(scrollY, middleHei);
            animationReBackToRefreshing.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int value = (int) animation.getAnimatedValue();
                    if (value > getScrollY()) {
                        scrollTo(0, value);
                    }
                }
            });
            animationReBackToRefreshing.addListener(new Animator.AnimatorListener() {
                boolean isCancel = false;

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (!isCancel) {
                        switch (mFreshStatus) {
                            case CONTINUE:
                                mSwipeControl.onSwipeStatue(SwipeControl.SwipeStatus.SWIPE_HEAD_LOADING, mViewTop.getHeight(), mViewTop.getHeight());
                                break;
                        }

                        if (!mRefreshStatusContinueRunning) {
                            mRefreshStatusContinueRunning = true;
                            if (mOnRefreshListener != null) {
                                mOnRefreshListener.onRefresh();
                            }
                        }
                    }

                    isCancel = true;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    isCancel = true;
                }

                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
            animationReBackToRefreshing.setDuration(Math.abs(550 * (middleHei - scrollY) / mViewTop.getHeight()));
            animationReBackToRefreshing.start();
        }
        return isOverProgress;
    }

    private boolean tryBackToFreshFinish() {
        if (mIsTouchEventMode) return false;

        int scrollY = getScrollY();
        int middleHei = overScrollTop != 0 ? -overScrollTop + mSwipeControl.getOverScrollHei() : 0;

        if ((mFreshStatus == FreshStatus.CONTINUE) && scrollY == middleHei)
            return false;

        if (scrollY < 0) {
            stopAllScroll();
            animationReBackToTop = ValueAnimator.ofInt(scrollY, 0);
            animationReBackToTop.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int value = (int) animation.getAnimatedValue();
                    scrollTo(0, value);
                }
            });
            animationReBackToTop.addListener(new Animator.AnimatorListener() {
                boolean isCancel;

                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (!isCancel) {
                        mSwipeControl.onSwipeStatue(SwipeControl.SwipeStatus.SWIPE_HEAD_TOAST, 0, mViewTop.getHeight());
                    }
                    isCancel = true;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    isCancel = true;
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });


            if (scrollY == middleHei) {
                animationReBackToTop.setDuration(Math.abs(550 * (0 - scrollY) / mViewTop.getHeight()));
                animationReBackToTop.setStartDelay(650);
            } else {
                animationReBackToTop.setDuration(320);
            }
            animationReBackToTop.start();
            return true;
        }
        return false;
    }

    private void stopAllScroll() {
        if (animationReBackToRefreshing != null) {
            animationReBackToRefreshing.cancel();
        }
        if (animationReBackToTop != null) {
            animationReBackToTop.cancel();
        }
    }

    // ----------------------------------------------------------------ISwipeIMP-------------------------------------------------------------------//

    // 设置刷新控制监听
    @Override
    public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
        this.mOnRefreshListener = onRefreshListener;
    }

    // 设置被动监听
    @Override
    public void setOnStatusListener(OnStatusListener onStatusListener) {
        this.mOnStatusListener = onStatusListener;
    }

    @Override
    public void setFreshStatue(FreshStatus statue) {
        switch (statue) {
            case CONTINUE:
                mFreshStatus = FreshStatus.CONTINUE;
                if (mOnStatusListener != null) {
                    mOnStatusListener.onFreshStatue(mFreshStatus);
                }
                mRefreshStatusContinueRunning = true;
                mLoadedStatus = LoadedStatus.CONTINUE;
                mLoadedStatusContinueRunning = false;

                if (mOnRefreshListener != null) {
                    mOnRefreshListener.onRefresh();
                }

                mSwipeControl.onSwipeStatue(SwipeControl.SwipeStatus.SWIPE_HEAD_LOADING,
                        -getScrollY(), mViewTop.getHeight());
                tryBackToRefreshing();
                break;
            case SUCCESS:                                                                                   //设置刷新成功 自动隐藏
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mFreshStatus = FreshStatus.SUCCESS;
                        if (mOnStatusListener != null) {
                            mOnStatusListener.onFreshStatue(mFreshStatus);
                        }
                        mRefreshStatusContinueRunning = false;
                        mLoadedStatus = LoadedStatus.CONTINUE;
                        mLoadedStatusContinueRunning = false;
                        mSwipeControl.onSwipeStatue(SwipeControl.SwipeStatus.SWIPE_HEAD_COMPLETE_OK,
                                -getScrollY(), mViewTop.getHeight());
                        tryBackToFreshFinish();
                    }
                }, 1000);
                break;
            case ERROR_NET:                                                                              //设置刷新失败 自动隐藏
                if (mOnStatusListener != null) {
                    mOnStatusListener.onFreshStatue(FreshStatus.ERROR_NET);
                }
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mFreshStatus = FreshStatus.ERROR_NET;
                        if (mOnStatusListener != null) {
                            mOnStatusListener.onFreshStatue(mFreshStatus);
                        }
                        mRefreshStatusContinueRunning = false;
                        mLoadedStatus = LoadedStatus.CONTINUE;
                        mLoadedStatusContinueRunning = false;
                        mSwipeControl.onSwipeStatue(SwipeControl.SwipeStatus.SWIPE_HEAD_COMPLETE_ERROR_NET,
                                -getScrollY(), mViewTop.getHeight());
                        tryBackToFreshFinish();
                    }
                }, 1000);

                break;
            case ERROR:                                                                        //设置刷新失败 自动隐藏
                if (mOnStatusListener != null) {
                    mOnStatusListener.onFreshStatue(FreshStatus.ERROR);
                }
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mFreshStatus = FreshStatus.ERROR;
                        if (mOnStatusListener != null) {
                            mOnStatusListener.onFreshStatue(mFreshStatus);
                        }
                        mRefreshStatusContinueRunning = false;
                        mLoadedStatus = LoadedStatus.CONTINUE;
                        mLoadedStatusContinueRunning = false;
                        mSwipeControl.onSwipeStatue(SwipeControl.SwipeStatus.SWIPE_HEAD_COMPLETE_ERROR,
                                -getScrollY(), mViewTop.getHeight());
                        tryBackToFreshFinish();
                    }
                }, 1000);
                break;
        }
    }

    @Override
    public void setLoadMoreStatus(ISwipe.LoadedStatus status) {
        switch (status) {
            case CONTINUE:
                lab:
                {
                    int currentScrollY = getScrollY();
                    if (currentScrollY <= 0) {
                        break lab;
                    }
                    if ((mTargetView instanceof RecyclerView ||
                            mTargetView instanceof ListView ||
                            mTargetView instanceof ScrollView ||
                            mTargetView instanceof NestedScrollView) && canChildScrollUp()) {
                        mTargetView.scrollBy(0, currentScrollY);
                    }

                    stopAllScroll();
                    scrollTo(0, 0);
                }

                this.mLoadedStatusContinueRunning = false;
                this.mLoadedStatus = LoadedStatus.CONTINUE;
                mSwipeControl.onSwipeStatue(SwipeControl.SwipeStatus.SWIPE_LOAD_LOADING,
                        getHeight() - mViewBottom.getTop(), mViewBottom.getHeight());
                break;
            case ERROR:
                this.mLoadedStatusContinueRunning = true;
                this.mLoadedStatus = LoadedStatus.ERROR;
                mSwipeControl.onSwipeStatue(SwipeControl.SwipeStatus.SWIPE_LOAD_ERROR,
                        getHeight() - mViewBottom.getTop(), mViewBottom.getHeight());
                break;
            case NO_MORE:
                this.mLoadedStatusContinueRunning = true;
                this.mLoadedStatus = LoadedStatus.NO_MORE;
                mSwipeControl.onSwipeStatue(SwipeControl.SwipeStatus.SWIPE_LOAD_NO_MORE,
                        getHeight() - mViewBottom.getTop(), mViewBottom.getHeight());
                break;
        }
    }


    // 设置刷新模式
    @Override
    public void setSwipeModel(SwipeControl.SwipeModel model) {
        if (this.mModel != model && model != null) {
            this.mModel = model;
            requestLayout();
        }
    }

    // 设置自定义刷新视图
    @Override
    public void setSwipeControl(SwipeControl control) {
        if (control != null && this.mSwipeControl != control) {
            removeView(mViewTop);
            removeView(mViewBottom);
            this.mSwipeControl = control;
            mViewTop = mSwipeControl.getSwipeHead();
            mViewBottom = mSwipeControl.getSwipeFoot();
            addView(mViewTop);
            addView(mViewBottom);
            requestLayout();
        }
    }

}
































