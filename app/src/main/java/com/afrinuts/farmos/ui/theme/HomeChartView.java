package com.afrinuts.farmos.ui.theme;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

/** Compact, dependency-free charts used only by the Home overview. */
public class HomeChartView extends View {
    public enum Mode { AREA, DONUT, BARS }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Mode mode = Mode.AREA;
    private float[] values = new float[0];

    public HomeChartView(Context context, AttributeSet attrs) { super(context, attrs); }
    public void setMode(Mode mode) { this.mode = mode; invalidate(); }
    public void setValues(float[] values) { this.values = values == null ? new float[0] : values.clone(); invalidate(); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mode == Mode.DONUT) drawDonut(canvas);
        else if (mode == Mode.BARS) drawBars(canvas);
        else drawArea(canvas);
    }

    private void drawArea(Canvas canvas) {
        float width = getWidth(), height = getHeight();
        if (width <= 0 || height <= 0 || values.length == 0) return;
        float pad = dp(4);
        float max = 0;
        for (float value : values) max = Math.max(max, value);
        Path area = new Path();
        Path line = new Path();
        for (int i = 0; i < values.length; i++) {
            float x = pad + (width - 2 * pad) * i / Math.max(1, values.length - 1);
            float y = height - pad - (max <= 0 ? 0 : (height - 2 * pad) * values[i] / max);
            if (i == 0) {
                area.moveTo(x, height - pad);
                area.lineTo(x, y);
                line.moveTo(x, y);
            } else {
                area.lineTo(x, y);
                line.lineTo(x, y);
            }
        }
        area.lineTo(width - pad, height - pad);
        area.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(0, 0, 0, height,
                0x470E6B3C, 0x080E6B3C, Shader.TileMode.CLAMP));
        canvas.drawPath(area, paint);
        paint.setShader(null);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(3));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(0xFF0E6B3C);
        canvas.drawPath(line, paint);

        float lastY = height - pad - (max <= 0 ? 0 : (height - 2 * pad) * values[values.length - 1] / max);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(width - pad, lastY, dp(4.5f), paint);
    }

    private void drawDonut(Canvas canvas) {
        float side = Math.min(getWidth(), getHeight());
        if (side <= 0) return;
        float stroke = dp(13);
        float inset = stroke / 2f + dp(1);
        float premium = values.length > 0 ? Math.max(0, values[0]) : 0;
        float total = 0;
        for (float value : values) total += Math.max(0, value);
        float sweep = total <= 0 ? 0 : 360f * Math.min(premium / total, 1f);
        float right = side - inset;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(stroke);
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setColor(0xFFF3EAD8);
        canvas.drawArc(inset, inset, right, right, 0, 360, false, paint);
        if (sweep > 0) {
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(0xFF0E6B3C);
            canvas.drawArc(inset, inset, right, right, -90, sweep, false, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawBars(Canvas canvas) {
        float width = getWidth(), height = getHeight();
        if (width <= 0 || height <= 0) return;
        String[] labels = {"Premium", "Standard", "Reject"};
        int[] colors = {0xFF0E6B3C, 0xFFC9A86A, 0xFFB3402A};
        float max = 0;
        for (float value : values) max = Math.max(max, value);

        float cellWidth = width / 3f;
        float barWidth = Math.min(dp(30), cellWidth * .62f);
        float valueSize = sp(13);
        float gradeSize = sp(12);
        float baseline = height - gradeSize - dp(8);
        float chartTop = valueSize + dp(8);
        float maxBarHeight = Math.max(0, baseline - chartTop);
        paint.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        paint.setTextAlign(Paint.Align.CENTER);

        for (int i = 0; i < labels.length; i++) {
            float value = i < values.length ? Math.max(0, values[i]) : 0;
            float center = cellWidth * (i + .5f);
            float barHeight = max <= 0 ? 0 : maxBarHeight * value / max;
            float top = baseline - barHeight;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFF1C1B1A);
            paint.setTextSize(valueSize);
            canvas.drawText(String.valueOf(Math.round(value)), center, Math.max(valueSize, top - dp(4)), paint);

            if (barHeight > 0) {
                paint.setColor(colors[i]);
                canvas.drawRoundRect(center - barWidth / 2f, top, center + barWidth / 2f, baseline,
                        dp(6), dp(6), paint);
            }

            paint.setColor(0xFF3D3A35);
            paint.setTextSize(gradeSize);
            canvas.drawText(labels[i], center, height - dp(1), paint);
        }
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(Typeface.DEFAULT);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
