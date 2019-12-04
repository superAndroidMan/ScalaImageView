package com.scalavew.superandroid.scalaimageview;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.OverScroller;

import com.scalavew.superandroid.scalaimageview.util.BitmapUtil;
import com.scalavew.superandroid.scalaimageview.util.DensityUtil;

public class ScalaImageView extends View implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, Runnable {

    private final float OVER_SCALE_FATOR = 1.5f;//放大倍数
    private Paint mPaint;
    private Bitmap bitmap;
    private float originalOffsetX;//scale偏移量
    private float originalOffsetY;
    private float smallScale;
    private float bigScale;
    private String TAG = getClass().getSimpleName();
    private GestureDetector gestureDetector;
    private boolean isBig = false;
    private float scaleFraction;//动画
    ObjectAnimator scaleAnimator;
    private float offsetX;//移动偏移量
    private float offsetY;
    private OverScroller overScroller;

    public ScalaImageView(Context context) {
        this(context, null);
    }

    public ScalaImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScalaImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bitmap = BitmapUtil.getAvatar(getContext().getResources(), R.mipmap.avatar,
                DensityUtil.dip2px(getContext(), 300));
        gestureDetector = new GestureDetector(getContext(), this);
        overScroller = new OverScroller(getContext());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.translate(offsetX, offsetY);

        float scale = smallScale + (bigScale - smallScale) * scaleFraction;
        canvas.scale(scale, scale, getWidth() / 2f, getHeight() / 2f);
        canvas.drawBitmap(bitmap, originalOffsetX, originalOffsetY, mPaint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        originalOffsetX = (getWidth() - bitmap.getWidth()) / 2f;
        originalOffsetY = (getHeight() - bitmap.getHeight()) / 2f;

        //放大的情况，2种，根据view宽高比，宽图上下拉伸否则左右拉伸
        if ((float) bitmap.getWidth() / bitmap.getHeight() > (float) getWidth() / getHeight()) {
            smallScale = (float) getWidth() / bitmap.getWidth();
            bigScale = (float) getHeight() / bitmap.getHeight() * OVER_SCALE_FATOR;
        } else {
            smallScale = (float) bitmap.getWidth() / getWidth();
            bigScale = (float) bitmap.getHeight() / getHeight() * OVER_SCALE_FATOR;
        }

    }

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    /**
     * @param down         按下事件
     * @param currentEvent 当前事件
     * @param distanceX    与上一次滑动的差值 X
     * @param distanceY    与上一次滑动的差值 Y
     * @return
     */
    @Override
    public boolean onScroll(MotionEvent down, MotionEvent currentEvent, float distanceX, float distanceY) {
        if (isBig) {
            //边界判断
            offsetX -= distanceX;
            offsetY -= distanceY;
            offsetX = Math.min(offsetX, (bitmap.getWidth() * bigScale - getWidth()) / 2);
            offsetX = Math.max(offsetX, -(bitmap.getWidth() * bigScale - getWidth()) / 2);
            offsetY = Math.min(offsetY, (bitmap.getHeight() * bigScale - getHeight()) / 2);
            offsetY = Math.max(offsetY, -(bitmap.getHeight() * bigScale - getHeight()) / 2);
            invalidate();
        }
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (isBig) {
            int minX = -(int) (bitmap.getWidth() * bigScale - getWidth()) / 2;
            int maxX = (int) (bitmap.getWidth() * bigScale - getWidth()) / 2;
            int minY = -(int) (bitmap.getHeight() * bigScale - getHeight()) / 2;
            int maxY = (int) (bitmap.getHeight() * bigScale - getHeight()) / 2;
            overScroller.fling((int) offsetX, (int) offsetY, (int) velocityX, (int) velocityY,
                    minX, maxX, minY, maxY, 100, 100);
            postOnAnimation(this);
        }
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        isBig = !isBig;
        if (isBig) {
            getScaleAnimator().start();
        } else {
            //回到初始位置
            offsetX = 0;
            offsetY = 0;
            getScaleAnimator().reverse();
        }
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    private ObjectAnimator getScaleAnimator() {
        if (scaleAnimator == null) {
            scaleAnimator = ObjectAnimator.ofFloat(this, "scaleFraction", 0, 1f);
        }

        return scaleAnimator;
    }

    public float getScaleFraction() {
        return scaleFraction;
    }

    public void setScaleFraction(float scaleFraction) {
        this.scaleFraction = scaleFraction;
        invalidate();
    }

    /**
     * 执行fling动画
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void run() {
        //动画是否结束 If it returns true, the animation is not yet finished.
        boolean computeScrollOffset = overScroller.computeScrollOffset();
        if(computeScrollOffset){
            offsetX = overScroller.getCurrX();
            offsetY = overScroller.getCurrY();
            invalidate();
            postOnAnimation(this);
        }
    }
}
