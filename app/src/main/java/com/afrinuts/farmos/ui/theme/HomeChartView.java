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
    public enum Mode { AREA, PIE, BARS }

    private static final int[] GRADE_COLORS = {0xFF0E6B3C, 0xFFC9A86A, 0xFFB3402A};

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Mode mode = Mode.AREA;
    private float[] values = new float[0];

    public HomeChartView(Context context, AttributeSet attrs) { super(context, attrs); }
    public void setMode(Mode mode) { this.mode = mode; invalidate(); }
    public void setValues(float[] values) { this.values = values == null ? new float[0] : values.clone(); invalidate(); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mode == Mode.PIE) drawPie(canvas);
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

    private void drawPie(Canvas canvas) {
        float side = Math.min(getWidth(), getHeight());
        if (side <= 0) return;
        float inset = dp(2);
        float right = side - inset;
        float total = 0;
        for (float value : values) total += Math.max(0, value);
        paint.setStyle(Paint.Style.FILL);
        if (total <= 0) {
            paint.setColor(0xFFE9E1CF);
            canvas.drawCircle(side / 2f, side / 2f, (side - 2 * inset) / 2f, paint);
            return;
        }

        float startAngle = -90;
        for (int i = 0; i < GRADE_COLORS.length; i++) {
            float value = i < values.length ? Math.max(0, values[i]) : 0;
            if (value <= 0) continue;
            float sweep = 360f * value / total;
            paint.setColor(GRADE_COLORS[i]);
            canvas.drawArc(inset, inset, right, right, startAngle, sweep, true, paint);
            startAngle += sweep;
        }
    }

    private void drawBars(Canvas canvas) {
        float width = getWidth(), height = getHeight();
        if (width <= 0 || height <= 0) return;
        String[] labels = {"Premium", "Standard", "Reject"};
        float max = 0;
        for (float value : values) max = Math.max(max, value);

        float edgeInset = dp(4);
        float cellWidth = (width - 2 * edgeInset) / 3f;
        float barWidth = Math.min(dp(22), cellWidth * .46f);
        float valueSize = sp(13);
        float gradeSize = sp(11);
        float baseline = height - gradeSize - dp(8);
        float chartTop = valueSize + dp(8);
        float maxBarHeight = Math.max(0, baseline - chartTop);
        paint.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        paint.setTextAlign(Paint.Align.CENTER);

        for (int i = 0; i < labels.length; i++) {
            float value = i < values.length ? Math.max(0, values[i]) : 0;
            float center = edgeInset + cellWidth * (i + .5f);
            float barHeight = max <= 0 ? 0 : maxBarHeight * value / max;
            float top = baseline - barHeight;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFF1C1B1A);
            paint.setTextSize(valueSize);
            canvas.drawText(String.valueOf(Math.round(value)), center, Math.max(valueSize, top - dp(4)), paint);

            if (barHeight > 0) {
                paint.setColor(GRADE_COLORS[i]);
                canvas.drawRoundRect(center - barWidth / 2f, top, center + barWidth / 2f, baseline,
                        dp(6), dp(6), paint);
            }

            paint.setColor(0xFF3D3A35);
            paint.setTextSize(gradeSize);
            while (paint.measureText(labels[i]) > cellWidth - dp(6) && gradeSize > sp(9)) {
                gradeSize -= sp(.5f);
                paint.setTextSize(gradeSize);
            }
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
