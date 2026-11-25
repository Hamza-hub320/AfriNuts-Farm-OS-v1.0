package com.afrinuts.farmos.ui.weather;

import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.res.ResourcesCompat;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.remote.WeatherResponse;
import com.afrinuts.farmos.data.repository.WeatherRepository;
import com.afrinuts.farmos.ui.BaseActivity;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WeatherActivity extends BaseActivity {

    private static final int INK = Color.rgb(28, 27, 26);
    private static final int MUTED = Color.rgb(89, 84, 76);
    private static final int DEEP_GREEN = Color.rgb(10, 85, 48);
    private static final int PALE_GREEN = Color.rgb(227, 240, 231);
    private static final int BORDER = Color.rgb(233, 225, 207);

    private TextView tvLocation, tvLastUpdated, tvTemp, tvCondition, tvHighLow;
    private TextView tvHumidity, tvWind, tvUvIndex, tvCloudCover, tvRainChance;
    private TextView tvPrecipitation, tvFeelsLike, tvSunrise, tvSunset, tvFarmAdvice;
    private ImageView ivWeatherIcon, adviceIcon;
    private LinearLayout hourlyContainer, forecastContainer;
    private WeatherRepository weatherRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!sessionManager.isLoggedIn() || !checkRoleAccess()) return;
        setContentView(R.layout.activity_weather);
        bindViews();
        findViewById(R.id.toolbarBack).setOnClickListener(v -> finish());
        loadWeatherData();
    }

    private void bindViews() {
        tvLocation = findViewById(R.id.tvLocation);
        tvLastUpdated = findViewById(R.id.tvLastUpdated);
        ivWeatherIcon = findViewById(R.id.ivWeatherIcon);
        tvTemp = findViewById(R.id.tvTemp);
        tvCondition = findViewById(R.id.tvCondition);
        tvHighLow = findViewById(R.id.tvHighLow);
        tvHumidity = findViewById(R.id.tvHumidity);
        tvWind = findViewById(R.id.tvWind);
        tvUvIndex = findViewById(R.id.tvUvIndex);
        tvCloudCover = findViewById(R.id.tvCloudCover);
        tvRainChance = findViewById(R.id.tvRainChance);
        tvPrecipitation = findViewById(R.id.tvPrecipitation);
        tvFeelsLike = findViewById(R.id.tvFeelsLike);
        tvSunrise = findViewById(R.id.tvSunrise);
        tvSunset = findViewById(R.id.tvSunset);
        tvFarmAdvice = findViewById(R.id.tvFarmAdvice);
        adviceIcon = findViewById(R.id.adviceIcon);
        hourlyContainer = findViewById(R.id.hourlyContainer);
        forecastContainer = findViewById(R.id.forecastContainer);
        ((ImageView) findViewById(R.id.iconSunrise)).setImageDrawable(new HorizonSunDrawable(true));
        ((ImageView) findViewById(R.id.iconSunset)).setImageDrawable(new HorizonSunDrawable(false));
        for (int id : new int[]{R.id.iconHumidity, R.id.iconWind, R.id.iconUvIndex,
                R.id.iconCloudCover, R.id.iconRainChance, R.id.iconPrecipitation,
                R.id.iconFeelsLike, R.id.adviceIcon}) {
            View icon = findViewById(id);
            if (icon != null) icon.setBackground(roundDrawable(PALE_GREEN, 14));
        }
        for (int id : new int[]{R.id.tileCloudCover, R.id.tileRainChance,
                R.id.tilePrecipitation, R.id.tileFeelsLike}) {
            View tile = findViewById(id);
            if (tile != null) tile.setBackground(roundDrawable(0xFFFCF9F1, 18));
        }
    }

    private void loadWeatherData() {
        String weatherJson = getIntent().getStringExtra("weather_json");
        if (weatherJson == null) {
            Toast.makeText(this, "No weather data available", Toast.LENGTH_SHORT).show();
            return;
        }
        WeatherResponse weather = new Gson().fromJson(weatherJson, WeatherResponse.class);
        if (weather != null) {
            displayWeather(weather);
            if (hasForecast(weather)) {
                generateFarmAdvice(weather);
                return;
            }
        }
        loadForecastData();
    }

    private boolean hasForecast(WeatherResponse weather) {
        return weather.getForecast() != null && weather.getForecast().getForecastday() != null
                && !weather.getForecast().getForecastday().isEmpty();
    }

    private void loadForecastData() {
        weatherRepository = new WeatherRepository(this);
        weatherRepository.getWeatherForecast(new WeatherRepository.WeatherCallback() {
            @Override public void onSuccess(WeatherResponse weather) {
                if (isFinishing() || isDestroyed() || weather == null) return;
                displayWeather(weather);
                generateFarmAdvice(weather);
            }

            @Override public void onError(String error) {
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(WeatherActivity.this, "Weather forecast unavailable", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void displayWeather(WeatherResponse weather) {
        WeatherResponse.Location location = weather.getLocation();
        WeatherResponse.Current current = weather.getCurrent();
        List<WeatherResponse.ForecastDay> days = weather.getForecast() == null
                ? null : weather.getForecast().getForecastday();

        if (location != null) {
            String name = location.getName() == null ? "" : location.getName();
            String country = location.getCountry() == null ? "" : location.getCountry();
            tvLocation.setText(name + (country.isEmpty() ? "" : ", " + country));
            String localtime = location.getLocaltime();
            if (localtime != null) {
                try {
                    Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(localtime);
                    tvLastUpdated.setText("Updated: " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date));
                } catch (Exception ignored) {
                    tvLastUpdated.setText("Updated: " + localtime);
                }
            }
        }

        if (current != null) {
            WeatherResponse.Condition condition = current.getCondition();
            String conditionText = condition == null ? "" : condition.getText();
            setWeatherIcon(ivWeatherIcon, conditionText);
            tvTemp.setText(String.format(Locale.getDefault(), "%.0f°C", current.getTempC()));
            tvCondition.setText(conditionText);
            tvHumidity.setText(current.getHumidity() + "%");
            tvWind.setText(String.format(Locale.getDefault(), "%.0f km/h", current.getWindKph()));
            tvUvIndex.setText(String.format(Locale.getDefault(), "%.0f", current.getUv()));
            tvCloudCover.setText(current.getCloud() + "%");
            tvFeelsLike.setText(String.format(Locale.getDefault(), "%.0f°C", current.getFeelslikeC()));
        }

        if (days != null && !days.isEmpty()) {
            WeatherResponse.ForecastDay today = days.get(0);
            WeatherResponse.Day day = today.getDay();
            if (day != null) {
                tvHighLow.setText(String.format(Locale.getDefault(), "H:%.0f°  L:%.0f°", day.getMaxtempC(), day.getMintempC()));
                tvRainChance.setText(day.getDailyChanceOfRain() + "%");
                tvPrecipitation.setText(String.format(Locale.getDefault(), "%.1f mm", day.getTotalprecipMm()));
            }
            displayHourly(weather, days);
            displayForecast(days);
        }
    }

    private void displayHourly(WeatherResponse weather, List<WeatherResponse.ForecastDay> days) {
        List<WeatherResponse.Hour> hours = new ArrayList<>();
        for (WeatherResponse.ForecastDay day : days) {
            if (day != null && day.getHour() != null) hours.addAll(day.getHour());
        }
        if (hours.isEmpty()) return;

        int start = 0;
        String localtime = weather.getLocation() == null ? null : weather.getLocation().getLocaltime();
        if (localtime != null) {
            try {
                Date now = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(localtime);
                String hourKey = new SimpleDateFormat("yyyy-MM-dd HH", Locale.getDefault()).format(now);
                for (int i = 0; i < hours.size(); i++) {
                    if (hours.get(i).getTime() != null && hours.get(i).getTime().startsWith(hourKey)) {
                        start = i;
                        break;
                    }
                }
            } catch (Exception ignored) { }
        }

        int count = Math.min(24, hours.size() - start);
        for (int i = 0; i < count; i++) {
            WeatherResponse.Hour hour = hours.get(start + i);
            String time = hour.getTime();
            String label = i == 0 ? "Now" : formatHour(time);
            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setPadding(dp(10), dp(12), dp(10), dp(12));
            tile.setBackground(roundDrawable(i == 0 ? PALE_GREEN : 0xFFFCF9F1, 18));
            LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(dp(76), dp(148));
            tileParams.setMargins(dp(4), 0, dp(6), 0);
            hourlyContainer.addView(tile, tileParams);

            tile.addView(text(label, 13, i == 0 ? DEEP_GREEN : MUTED, true));
            ImageView icon = new ImageView(this);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(26), dp(26));
            iconParams.topMargin = dp(10);
            iconParams.bottomMargin = dp(9);
            tile.addView(icon, iconParams);
            setWeatherIcon(icon, hour.getCondition() == null ? "" : hour.getCondition().getText());
            tile.addView(text(String.format(Locale.getDefault(), "%.0f°", hour.getTempC()), 17, INK, true));
            LinearLayout rain = new LinearLayout(this);
            rain.setGravity(Gravity.CENTER);
            ImageView drop = new ImageView(this);
            drop.setImageResource(R.drawable.ic_water_drop);
            drop.setColorFilter(DEEP_GREEN);
            rain.addView(drop, new LinearLayout.LayoutParams(dp(13), dp(13)));
            TextView rainValue = text(hour.getChanceOfRain() + "%", 12, DEEP_GREEN, true);
            LinearLayout.LayoutParams rainValueParams = new LinearLayout.LayoutParams(-2, -2);
            rainValueParams.leftMargin = dp(3);
            rain.addView(rainValue, rainValueParams);
            LinearLayout.LayoutParams rainParams = new LinearLayout.LayoutParams(-2, -2);
            rainParams.topMargin = dp(8);
            tile.addView(rain, rainParams);
        }
    }

    private String formatHour(String value) {
        if (value == null) return "—";
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(value);
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
        } catch (Exception e) {
            return value.length() >= 16 ? value.substring(11, 16) : value;
        }
    }

    private void displayForecast(List<WeatherResponse.ForecastDay> days) {
        forecastContainer.removeAllViews();
        int count = Math.min(3, days.size());
        for (int i = 0; i < count; i++) {
            WeatherResponse.ForecastDay forecastDay = days.get(i);
            WeatherResponse.Day day = forecastDay.getDay();
            if (day == null) continue;
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(12), 0, dp(12));
            TextView name = text(forecastName(i, forecastDay.getDate()), 14, INK, true);
            row.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
            ImageView icon = new ImageView(this);
            icon.setBackground(roundDrawable(forecastIconBackground(day.getCondition()), 14));
            icon.setPadding(dp(5), dp(5), dp(5), dp(5));
            row.addView(icon, new LinearLayout.LayoutParams(dp(26), dp(26)));
            setWeatherIcon(icon, day.getCondition() == null ? "" : day.getCondition().getText());
            TextView range = text(String.format(Locale.getDefault(), "%.0f° / %.0f°", day.getMaxtempC(), day.getMintempC()), 14, MUTED, true);
            LinearLayout.LayoutParams rangeParams = new LinearLayout.LayoutParams(dp(102), -2);
            rangeParams.leftMargin = dp(14);
            row.addView(range, rangeParams);
            LinearLayout rain = new LinearLayout(this);
            rain.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
            ImageView drop = new ImageView(this);
            drop.setImageResource(R.drawable.ic_water_drop);
            drop.setColorFilter(DEEP_GREEN);
            rain.addView(drop, new LinearLayout.LayoutParams(dp(15), dp(15)));
            TextView rainValue = text(day.getDailyChanceOfRain() + "%", 13, DEEP_GREEN, true);
            LinearLayout.LayoutParams rainValueParams = new LinearLayout.LayoutParams(-2, -2);
            rainValueParams.leftMargin = dp(3);
            rain.addView(rainValue, rainValueParams);
            LinearLayout.LayoutParams rainParams = new LinearLayout.LayoutParams(dp(64), -2);
            rainParams.leftMargin = dp(8);
            row.addView(rain, rainParams);
            forecastContainer.addView(row);
            if (i < count - 1) forecastContainer.addView(divider());
        }
    }

    private String forecastName(int index, String dateValue) {
        if (index == 0) return "Today";
        if (index == 1) return "Tomorrow";
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateValue);
            return new SimpleDateFormat("EEEE", Locale.getDefault()).format(date);
        } catch (Exception e) { return "—"; }
    }

    private int forecastIconBackground(WeatherResponse.Condition condition) {
        String text = condition == null || condition.getText() == null
                ? "" : condition.getText().toLowerCase(Locale.ROOT);
        if (text.contains("sunny") || text.contains("clear")) return 0xFFFFF2D2;
        if (text.contains("rain") || text.contains("storm") || text.contains("thunder")) return PALE_GREEN;
        return 0xFFF6F1E7;
    }

    // Keep the existing Farm Advice rules and copy unchanged.
    private void generateFarmAdvice(WeatherResponse weather) {
        if (weather.getForecast() == null) return;

        WeatherResponse.ForecastDay today = weather.getForecast().getForecastday().get(0);
        int rainChance = today.getDay().getDailyChanceOfRain();
        double temp = today.getDay().getAvgtempC();

        String advice;
        int adviceDrawable;

        if (rainChance > 70) {
            advice = "Heavy rain expected. Avoid planting and spraying today.";
            adviceDrawable = R.drawable.ic_warning;
        } else if (rainChance > 40) {
            advice = "Scattered showers. Good day for indoor maintenance tasks.";
            adviceDrawable = R.drawable.ic_rain;
        } else if (temp > 35) {
            advice = "Very hot. Schedule work for early morning or evening.";
            adviceDrawable = R.drawable.ic_wb_sunny;
        } else if (temp > 25) {
            advice = "Perfect conditions for planting and fertilization.";
            adviceDrawable = R.drawable.ic_park;
        } else {
            advice = "Cool weather. Good for pruning and weeding.";
            adviceDrawable = R.drawable.ic_air;
        }

        tvFarmAdvice.setText(advice);
        adviceIcon.setImageResource(adviceDrawable);
        adviceIcon.setColorFilter(DEEP_GREEN);
    }

    private void setWeatherIcon(ImageView imageView, String condition) {
        String lowerCondition = condition == null ? "" : condition.toLowerCase(Locale.ROOT);
        if (lowerCondition.contains("sunny") || lowerCondition.contains("clear")) {
            imageView.setImageResource(R.drawable.ic_wb_sunny);
            imageView.setColorFilter(0xFFD18A17);
        } else if (lowerCondition.contains("partly cloudy")) {
            imageView.setImageResource(R.drawable.ic_partly_cloudy);
            imageView.setColorFilter(0xFFD18A17);
        } else if (lowerCondition.contains("cloudy")) {
            imageView.setImageResource(R.drawable.ic_cloud);
            imageView.setColorFilter(MUTED);
        } else if (lowerCondition.contains("rain")) {
            imageView.setImageResource(R.drawable.ic_rain);
            imageView.setColorFilter(DEEP_GREEN);
        } else if (lowerCondition.contains("storm") || lowerCondition.contains("thunder")) {
            imageView.setImageResource(R.drawable.ic_storm);
            imageView.setColorFilter(DEEP_GREEN);
        } else if (lowerCondition.contains("snow")) {
            imageView.setImageResource(R.drawable.ic_snow);
            imageView.setColorFilter(DEEP_GREEN);
        } else if (lowerCondition.contains("fog") || lowerCondition.contains("mist")) {
            imageView.setImageResource(R.drawable.ic_fog);
            imageView.setColorFilter(MUTED);
        } else {
            imageView.setImageResource(R.drawable.ic_wb_sunny);
            imageView.setColorFilter(0xFFD18A17);
        }
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setFontFeatureSettings("kern");
        Typeface typeface = ResourcesCompat.getFont(this, bold ? R.font.manrope_bold : R.font.manrope_semibold);
        if (typeface != null) view.setTypeface(typeface);
        return view;
    }

    private View divider() {
        View view = new View(this);
        view.setBackgroundColor(BORDER);
        view.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(1)));
        return view;
    }

    private GradientDrawable roundDrawable(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        if (weatherRepository != null) weatherRepository.shutdown();
        super.onDestroy();
    }

    private final class HorizonSunDrawable extends Drawable {
        private final boolean sunrise;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        HorizonSunDrawable(boolean sunrise) {
            this.sunrise = sunrise;
            paint.setColor(DEEP_GREEN);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
        }

        @Override public void draw(Canvas canvas) {
            Rect b = getBounds();
            canvas.save();
            canvas.translate(b.left, b.top);
            canvas.scale(b.width() / 24f, b.height() / 24f);
            canvas.drawArc(5, 8, 19, 22, 180, -180, false, paint);
            canvas.drawLine(2, 17, 22, 17, paint);
            canvas.drawLine(4, 21, 20, 21, paint);
            Path arrow = new Path();
            if (sunrise) {
                canvas.drawLine(12, 17, 12, 3, paint);
                arrow.moveTo(8, 7); arrow.lineTo(12, 3); arrow.lineTo(16, 7);
            } else {
                canvas.drawLine(12, 4, 12, 17, paint);
                arrow.moveTo(8, 13); arrow.lineTo(12, 17); arrow.lineTo(16, 13);
            }
            canvas.drawPath(arrow, paint);
            canvas.restore();
        }

        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
        @Override public void setColorFilter(android.graphics.ColorFilter colorFilter) { paint.setColorFilter(colorFilter); invalidateSelf(); }
        @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
    }

    @Override
    protected boolean checkRoleAccess() {
        return com.afrinuts.farmos.utils.AccessMap.canOpenScreen(
                getClass().getSimpleName(), sessionManager.getUserRole());
    }
}
