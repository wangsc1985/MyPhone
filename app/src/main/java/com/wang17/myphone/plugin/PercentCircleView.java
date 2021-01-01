package com.wang17.myphone.plugin;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.wang17.myphone.R;

public class PercentCircleView extends View {

    private static final String _TAG = "wangsc";
    private Paint circlePaint = new Paint();
    private int progressColor = ContextCompat.getColor(getContext(), R.color.progressbar_color);
    private float viewRadius = 0f;
    private float circleSize = dip2px(10f);
    private float strokeWidth = dip2px(2.0f);
    private int percent = 0;

    public void setMax(int max) {
        this.max = max;
    }

    private int max=100;
    private RectF rectF = null;

    public PercentCircleView(Context context) {
        this(context, null);
    }

    public PercentCircleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PercentCircleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        circlePaint.setColor(progressColor);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(strokeWidth);
        circlePaint.setAntiAlias(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        viewRadius = Math.min(getMeasuredWidth(), getMeasuredHeight()) * 1.0f / 2;
        rectF = new RectF(circleSize / 2, circleSize / 2, 2 * viewRadius - circleSize / 2 - strokeWidth, 2 * viewRadius - circleSize / 2 - strokeWidth);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        circlePaint.setColor(progressColor);
        circlePaint.setStrokeWidth(strokeWidth);
        canvas.drawArc(rectF, -90f, 360 * (percent * 1.0f / this.max), false, circlePaint);
    }

    private float dip2px(float dipValue) {
        float scale = getContext().getResources().getDisplayMetrics().density;
        return dipValue * scale;
    }

    public void setProgress(int progress) {
        percent = progress;
        if (percent < 0) percent = 0;
        if (percent > this.max) percent = this.max;
        invalidate();
    }

}
