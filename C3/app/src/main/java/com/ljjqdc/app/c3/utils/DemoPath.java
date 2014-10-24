package com.ljjqdc.app.c3.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class DemoPath extends View {
    private float mX , mY;
    private Path mPath;
    private Paint mPaint;
    private static final float TOUCH_TOLERANCE = 4;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Paint   mBitmapPaint;

    public DemoPath(Context context, AttributeSet set)
    {
        super(context, set);
        init();
    }

    public DemoPath(Context c) {
        super(c);

        init();
    }

    public void clearAll(){
        init();
    }

    private void init(){
        mPaint = new Paint();//创建画笔渲染对象
        mPaint.setAntiAlias(true);//设置抗锯齿，让绘画比较平滑
        mPaint.setDither(true);//设置递色
        mPaint.setColor(0xFFFF0000);//设置画笔的颜色
        mPaint.setStyle(Paint.Style.STROKE);//画笔的类型有三种（1.FILL 2.FILL_AND_STROKE 3.STROKE ）
        mPaint.setStrokeJoin(Paint.Join.ROUND);//默认类型是MITER（1.BEVEL 2.MITER 3.ROUND ）
        mPaint.setStrokeCap(Paint.Cap.ROUND);//默认类型是BUTT（1.BUTT 2.ROUND 3.SQUARE ）
        mPaint.setStrokeWidth(8);//设置描边的宽度，如果设置的值为0那么边是一条极细的线

        mBitmap = Bitmap.createBitmap(720, 1280, Bitmap.Config.ARGB_8888);//绘制固定大小的bitmap对象
        mCanvas = new Canvas(mBitmap);//将固定的bitmap对象嵌入到canvas对象中
        mPath = new Path();//创建画笔路径
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
    }
    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(0xFFAAAAAA);
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        canvas.drawPath(mPath, mPaint);
        super.onDraw(canvas);
    }
    private void onTouchDown(float x , float y){
        mPath.reset();//将上次的路径保存起来，并重置新的路径。
        mPath.moveTo(x, y);//设置新的路径“轮廓”的开始
        mX = x;
        mY = y;
    }
    private void onTouchMove(float x , float y){
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
            mX = x;
            mY = y;
        }
    }
    private void onTouchUp(float x , float y){
        mPath.lineTo(mX, mY);//从最后一个指定的xy点绘制一条线，如果没有用moveTo方法，那么起始点表示（0，0）点。
        // commit the path to our offscreen
        mCanvas.drawPath(mPath, mPaint);//手指离开屏幕后，绘制创建的“所有”路径。
        // kill this so we don't double draw
        mPath.reset();
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN://手指开始按压屏幕，这个动作包含了初始化位置
                onTouchDown(x , y);
                invalidate();//刷新画布，重新运行onDraw（）方法
                break;
            case MotionEvent.ACTION_MOVE://手指按压屏幕时，位置的改变触发，这个方法在ACTION_DOWN和ACTION_UP之间。
                onTouchMove(x , y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP://手指离开屏幕，不再按压屏幕
                onTouchUp(x , y);
                invalidate();
                break;
            default:
                break;
        }
        return true;
    }
}
