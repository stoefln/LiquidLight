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

/**
 * Created by steph on 01/03/17.
 */

public class DrawableGridView extends View {



    public interface OnDrawableGridChanged{
        void onDrawableGridChanged(int trackNum, int col, boolean value);
    }

    private static int ROWS = 8;
    private static int PAGE_COUNT = 4;
    private static int COLS_PER_PAGE = 40;
    public static int COLS_TOTAL = COLS_PER_PAGE * PAGE_COUNT;

    private int mCurrentPageIndex = 0;

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
        mPattern = new boolean[COLS_TOTAL][ROWS];

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


                final float cellWidth = width / COLS_PER_PAGE;
                final float cellHeight = getHeight() / ROWS;
                int colIndex = (int) (x / cellWidth) + mCurrentPageIndex * COLS_PER_PAGE;
                int rowIndex = (int) (y / cellHeight);

                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    Log.v("DrawableGrid", "ACTION_DOWN x: "+ x + " \ty:" + y+ " ci: "+colIndex+ " ri: "+rowIndex);
                    mActivate = !mPattern[colIndex][rowIndex];
                }
                if(event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL){
                    Log.v("DrawableGrid", "ACTION_UP x: "+ x + " \ty:" + y+ " ci: "+colIndex+ " ri: "+rowIndex);
                    mActivate = null;
                }
                if(mActivate != null && colIndex < mPattern.length && rowIndex < mPattern[rowIndex].length) {
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
        float cellWidth = (float) getWidth() / COLS_PER_PAGE;
        float cellHeight = (float) getHeight() / ROWS;

        for (int r = 0; r < ROWS; r++) {

            for (int c = 0; c < COLS_PER_PAGE; c++) {
                float x = (float) c * cellWidth;
                float y = (float) r * cellHeight;
                mPaint.setColor(mColors[0]);
                canvas.drawRect(x, y, x + cellWidth, y + cellHeight, mPaint);
                int col = mCurrentPageIndex * COLS_PER_PAGE + c;
                if(mPattern[col][r]) {
                    mPaint.setColor(col == mSelectedCol ? mColors[3] : mColors[2]);
                } else {
                    mPaint.setColor(col == mSelectedCol ? mColors[0] : mColors[1]);
                }
                canvas.drawRect(x + mBorderWidth, y + mBorderWidth, x + cellWidth - mBorderWidth, y + cellHeight - mBorderWidth, mPaint);

            }
        }
    }

    public void next() {
        setSelectedCol((mSelectedCol+1) % COLS_TOTAL);
    }


    public void selectFirstColOfCurrentPage() {
        setSelectedCol(COLS_PER_PAGE * mCurrentPageIndex);
    }

    public void setCurrentPageIndex(int pageIndex){
        mCurrentPageIndex = pageIndex;
        invalidate();
    }

    public int getCurrentPageIndex() {
        return mCurrentPageIndex;
    }

    public void nextPage(){
        mCurrentPageIndex = (mCurrentPageIndex + 1) % PAGE_COUNT;
        invalidate();
    }
    public void prevPage(){
        mCurrentPageIndex--;
        if(mCurrentPageIndex < 0){
            mCurrentPageIndex = PAGE_COUNT - 1;
        }
        invalidate();
    }

    public void setPattern(boolean pattern[][]){
        for (int c = 0; c < COLS_TOTAL; c++) {
            for (int r = 0; r < ROWS; r++) {
                mPattern[c][r] = false;
                if (pattern.length > c && pattern[c].length > r){
                    mPattern[c][r] = pattern[c][r];
                }
            }
        }
    }
}
