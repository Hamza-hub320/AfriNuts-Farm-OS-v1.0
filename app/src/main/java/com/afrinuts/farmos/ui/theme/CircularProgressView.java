package com.afrinuts.farmos.ui.theme;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.afrinuts.farmos.R;

public class CircularProgressView extends View {

    private Paint backgroundPaint;
    private Paint progressPaint;
    private Paint textPaint;
    private RectF rectF = new RectF();

    private int progress = 0;
    private int max = 100;
    private int progressColor;
    private int backgroundColor;
    private int textColor;
    private float textSize;

    public CircularProgressView(Context context) {
        super(context);
        init(null);
    }

    public CircularProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public CircularProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.CircularProgressView);
            progressColor = a.getColor(R.styleable.CircularProgressView_progressColor, getColor(R.color.primary));
            backgroundColor = a.getColor(R.styleable.CircularProgressView_backgroundColor, getColor(R.color.warm_grey));
            textColor = a.getColor(R.styleable.CircularProgressView_textColor, getColor(R.color.primary));
            textSize = a.getDimension(R.styleable.CircularProgressView_textSize, 40f);
            a.recycle();
        } else {
            progressColor = getColor(R.color.primary);
            backgroundColor = getColor(R.color.warm_grey);
            textColor = getColor(R.color.primary);
            textSize = 40f;
        }

        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(backgroundColor);
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(8f);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(progressColor);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(8f);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(textColor);
        textPaint.setTextSize(textSize);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    private int getColor(int resId) {
        return getContext().getColor(resId);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int size = Math.min(width, height);
        int centerX = width / 2;
        int centerY = height / 2;
        float radius = size / 2f - 8f;

        rectF.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);

        // Draw background circle
        canvas.drawArc(rectF, 0, 360, false, backgroundPaint);

        // Draw progress arc
        float angle = 360 * progress / max;
        canvas.drawArc(rectF, -90, angle, false, progressPaint);

        // Draw percentage text
        String text = progress + "%";
        float textX = centerX;
        float textY = centerY - ((textPaint.descent() + textPaint.ascent()) / 2);
        canvas.drawText(text, textX, textY, textPaint);
    }

    public void setProgress(int progress) {
        this.progress = Math.min(progress, max);
        invalidate();
    }

    public void setMax(int max) {
        this.max = max;
        invalidate();
    }
}