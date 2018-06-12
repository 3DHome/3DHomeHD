package com.borqs.se.download;

import com.borqs.freehdhome.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ProgressBar;

public class SpecificProgressBar extends ProgressBar {
    private String mProgressPrefix;
    private Paint mPaint;
    private String mProgress;

    public SpecificProgressBar(Context context) {
        super(context);
    }

    public SpecificProgressBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public SpecificProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SpecificProgressBar);
        int textColor = a.getColor(R.styleable.SpecificProgressBar_progressTextColor, Color.WHITE);
        float textSize = a.getDimension(R.styleable.SpecificProgressBar_progressTextSize, 20);
        mPaint.setColor(textColor);
        mPaint.setTextSize(textSize);
        mProgressPrefix = a.getString(R.styleable.SpecificProgressBar_progressPrefix);
        a.recycle();
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Rect rect = new Rect();
        String progress = mProgressPrefix + mProgress;
        this.mPaint.getTextBounds(progress, 0, progress.length(), rect);
        int x = (getWidth() / 2) - rect.centerX();
        int y = (getHeight() / 2) - rect.centerY();
        canvas.drawText(progress, x, y, this.mPaint);
    }

    public synchronized void setProgress(float progress) {
        mProgress = Utils.converPercentage(progress);
        setProgress((int)(progress * 100));
    }

}
