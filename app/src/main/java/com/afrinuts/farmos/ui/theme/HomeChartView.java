package com.afrinuts.farmos.ui.theme;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.content.ContextCompat;
import com.afrinuts.farmos.R;

/** Small, dependency-free charts for the Home quality summary. */
public class HomeChartView extends View {
    public enum Mode { AREA, DONUT, BARS }
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Mode mode = Mode.AREA;
    private float[] values = new float[0];
    private boolean hero;

    public HomeChartView(Context context, AttributeSet attrs) { super(context, attrs); }
    public void setMode(Mode mode) { this.mode = mode; invalidate(); }
    public void setHero(boolean hero) { this.hero = hero; invalidate(); }
    public void setValues(float[] values) { this.values = values == null ? new float[0] : values.clone(); invalidate(); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mode == Mode.DONUT) drawDonut(canvas);
        else if (mode == Mode.BARS) drawBars(canvas);
        else drawArea(canvas);
    }

    private void drawArea(Canvas canvas) {
        float width = getWidth(), height = getHeight();
        if (width <= 0 || height <= 0) return;
        float max = 0;
        for (float value : values) max = Math.max(max, value);
        if (values.length == 0) return;
        float pad = 4f;
        Path area = new Path(), line = new Path();
        for (int i = 0; i < values.length; i++) {
            float x = pad + (width - 2 * pad) * i / Math.max(1, values.length - 1);
            float y = height - pad - (max <= 0 ? 0 : (height - 2 * pad) * values[i] / max);
            if (i == 0) { area.moveTo(x, height); area.lineTo(x, y); line.moveTo(x, y); }
            else { area.lineTo(x, y); line.lineTo(x, y); }
        }
        area.lineTo(width - pad, height); area.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(hero ? 0x55FFFFFF : 0x223C8A5D);
        canvas.drawPath(area, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(hero ? 2.5f : 2f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(hero ? 0xFFBFE8CF : ContextCompat.getColor(getContext(), R.color.primary));
        canvas.drawPath(line, paint);
        paint.setStyle(Paint.Style.FILL);
        if (values.length > 0) {
            float y = height - pad - (max <= 0 ? 0 : (height - 2 * pad) * values[values.length - 1] / max);
            paint.setColor(hero ? 0xFFFFFFFF : ContextCompat.getColor(getContext(), R.color.primary));
            canvas.drawCircle(width - pad, y, 3.5f, paint);
        }
    }

    private void drawDonut(Canvas canvas) {
        float side = Math.min(getWidth(), getHeight());
        if (side <= 0) return;
        float stroke = side * .17f;
        float inset = stroke / 2f + 1f;
        float premium = values.length > 0 ? values[0] : 0;
        float standard = values.length > 1 ? values[1] : 0;
        float reject = values.length > 2 ? values[2] : 0;
        float total = premium + standard + reject;
        float start = -90f;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(stroke);
        paint.setStrokeCap(Paint.Cap.BUTT);
        int[] colors = {ContextCompat.getColor(getContext(), R.color.primary), 0xFFD9C7A7, 0xFFB7B2A9};
        float[] parts = {premium, standard, reject};
        for (int i = 0; i < parts.length; i++) {
            float sweep = total <= 0 ? (i == 0 ? 360f : 0f) : 360f * parts[i] / total;
            paint.setColor(total <= 0 ? 0xFFECE5D9 : colors[i]);
            canvas.drawArc(inset, inset, side - inset, side - inset, start, sweep, false, paint);
            start += sweep;
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawBars(Canvas canvas) {
        float width = getWidth(), height = getHeight();
        if (width <= 0 || height <= 0 || values.length == 0) return;
        float max = 0;
        for (float value : values) max = Math.max(max, value);
        float gap = width * .16f;
        float barWidth = (width - gap * (values.length - 1)) / values.length;
        int[] colors = {ContextCompat.getColor(getContext(), R.color.primary), 0xFFD9C7A7, 0xFFB7B2A9};
        for (int i = 0; i < values.length; i++) {
            float barHeight = max <= 0 ? 0 : (height - 2) * values[i] / max;
            float left = i * (barWidth + gap);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(colors[i % colors.length]);
            canvas.drawRoundRect(left, height - barHeight, left + barWidth, height, barWidth * .18f, barWidth * .18f, paint);
        }
    }
}
