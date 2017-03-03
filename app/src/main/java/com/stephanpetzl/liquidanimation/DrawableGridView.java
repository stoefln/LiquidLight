package com.stephanpetzl.liquidanimation;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;

import java.util.Vector;

/**
 * Created by steph on 01/03/17.
 */

public class DrawableGridView extends View {

    public interface OnDrawableGridChanged{
        void onDrawableGridChanged(int trackNum, int col, boolean value);
    }

    private int mCols = 16;
    private int mRows = 8;
    private Paint mPaint;
    private int[] mColors;
    private int mBorderWidth = 2;
    private boolean[][] mPattern;
    private int mSelectedCol = -1;
    private OnDrawableGridChanged mOnDrawableGridChangedListener;

    public void setOnDrawableGridChangedListener(OnDrawableGridChanged onDrawableGridChangedListener) {
        mOnDrawableGridChangedListener = onDrawableGridChangedListener;
    }

    public void setSelectedCol(int selectedCol) {
        mSelectedCol = selectedCol;
        invalidate();
    }

    public int getSelectedCol() {
        return mSelectedCol;
    }

    public boolean[] getCurrentColumn() {
        return mPattern[mSelectedCol];
    }

    public DrawableGridView(Context context) {
        super(context);
        init();
    }

    public DrawableGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DrawableGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public DrawableGridView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }


    private void init() {
        Context context = getContext();
        Resources res = context.getResources();
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mColors = new int[]{
                ContextCompat.getColor(context, R.color.colorPrimary),
                ContextCompat.getColor(context, R.color.colorPrimaryDark),
                ContextCompat.getColor(context, R.color.colorAccent),
                ContextCompat.getColor(context, R.color.colorAccentLight)
        };
        mPaint.setColor(mColors[0]);
        mPattern = new boolean[mCols][mRows];

        setOnTouchListener(new OnTouchListener() {
            private Boolean mActivate;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                float x = event.getX();
                float y = event.getY();
                int width = getWidth();
                if(x < 0 || x > width || y < 0 || y > getHeight()) {
                    return true;
                }


                final float cellWidth = width / mCols;
                final float cellHeight = getHeight() / mRows;
                int colIndex = (int) (x / cellWidth);
                int rowIndex = (int) (y / cellHeight);

                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    Log.v("DrawableGrid", "ACTION_DOWN x: "+ x + " \ty:" + y+ " ci: "+colIndex+ " ri: "+rowIndex);
                    mActivate = !mPattern[colIndex][rowIndex];
                }
                if(event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL){
                    Log.v("DrawableGrid", "ACTION_UP x: "+ x + " \ty:" + y+ " ci: "+colIndex+ " ri: "+rowIndex);
                    mActivate = null;
                }
                if(mActivate != null) {
                    if(mPattern[colIndex][rowIndex] != mActivate) {
                        mPattern[colIndex][rowIndex] = mActivate;
                        if(mOnDrawableGridChangedListener != null) {
                            mOnDrawableGridChangedListener.onDrawableGridChanged(rowIndex, colIndex, mActivate);
                        }
                        invalidate();
                    }
                }
                Log.v("DrawableGrid", "Touchxx x: "+ x + " \ty:" + y+ " ci: "+colIndex+ " ri: "+rowIndex);
                return true;
            }
        });


    }

    @Override
    public boolean onDragEvent(DragEvent event) {
        Log.v("DrawableGrid", "Drag event x: "+event.getX()+ " \ty:" + event.getY());
        return super.onDragEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cellWidth = getWidth() / mCols;
        float cellHeight = getHeight() / mRows;

        for (int r = 0; r < mRows; r++) {
            for (int c = 0; c < mCols; c++) {
                float x = c * cellWidth;
                float y = r * cellHeight;
                mPaint.setColor(mColors[0]);
                canvas.drawRect(x, y, x + cellWidth, y + cellHeight, mPaint);
                if(mPattern[c][r]) {
                    mPaint.setColor(c == mSelectedCol ? mColors[3] : mColors[2]);
                } else {
                    mPaint.setColor(c == mSelectedCol ? mColors[0] : mColors[1]);
                }
                canvas.drawRect(x + mBorderWidth, y + mBorderWidth, x + cellWidth - mBorderWidth, y + cellHeight - mBorderWidth, mPaint);

            }
        }
    }

    public void next() {
        setSelectedCol((mSelectedCol+1) % mCols);
    }

    public void setPattern(boolean pattern[][]){
        mPattern = pattern;
        mCols = pattern.length;
        mRows = pattern[0].length;
    }
}
