package com.afrinuts.farmos.ui.weather;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.afrinuts.farmos.MainActivity;
import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.remote.WeatherResponse;
import com.afrinuts.farmos.ui.BaseHubActivity;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WeatherActivity extends BaseHubActivity {

    private TextView tvLocation;
    private TextView tvLastUpdated;
    private ImageView ivWeatherIcon;
    private TextView tvTemp;
    private TextView tvCondition;
    private TextView tvFeelsLike;
    private TextView tvHumidity;
    private TextView tvWind;
    private TextView tvUvIndex;
    private TextView tvSunrise;
    private TextView tvSunset;
    private TextView tvFarmAdvice;
    private LinearLayout forecastContainer;
    private View adviceCard;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_weather;
    }

    @Override
    protected int getCurrentNavItem() {
        return -1; // Weather is not a main nav item
    }

    @Override
    protected void onHubCreate(Bundle savedInstanceState) {
        initViews();
        loadWeatherData();
    }

    private void initViews() {
        tvLocation = findViewById(R.id.tvLocation);
        tvLastUpdated = findViewById(R.id.tvLastUpdated);
        ivWeatherIcon = findViewById(R.id.ivWeatherIcon);
        tvTemp = findViewById(R.id.tvTemp);
        tvCondition = findViewById(R.id.tvCondition);
        tvFeelsLike = findViewById(R.id.tvFeelsLike);
        tvHumidity = findViewById(R.id.tvHumidity);
        tvWind = findViewById(R.id.tvWind);
        tvUvIndex = findViewById(R.id.tvUvIndex);
        tvSunrise = findViewById(R.id.tvSunrise);
        tvSunset = findViewById(R.id.tvSunset);
        tvFarmAdvice = findViewById(R.id.tvFarmAdvice);
        forecastContainer = findViewById(R.id.forecastContainer);
        adviceCard = findViewById(R.id.adviceCard);
    }

    private void loadWeatherData() {
        String weatherJson = getIntent().getStringExtra("weather_json");

        if (weatherJson == null) {
            Toast.makeText(this, "No weather data available", Toast.LENGTH_SHORT).show();
            return;
        }

        WeatherResponse weather = new Gson().fromJson(weatherJson, WeatherResponse.class);

        if (weather == null) return;

        displayWeather(weather);
        generateFarmAdvice(weather);
    }

    private void displayWeather(WeatherResponse weather) {
        WeatherResponse.Location location = weather.getLocation();
        WeatherResponse.Current current = weather.getCurrent();

        if (location != null) {
            tvLocation.setText(location.getName() + ", " + location.getCountry());
            tvLocation.setVisibility(View.VISIBLE);

            if (location.getLocaltime() != null) {
                tvLastUpdated.setText("Updated: " + location.getLocaltime());
                tvLastUpdated.setVisibility(View.VISIBLE);
            }
        }

        if (current != null) {
            setWeatherIcon(ivWeatherIcon, current.getCondition().getText());

            tvTemp.setText(String.format(Locale.getDefault(), "%.0f°C", current.getTempC()));

            if (current.getCondition() != null) {
                tvCondition.setText(current.getCondition().getText());
            }

            tvFeelsLike.setText(String.format(Locale.getDefault(),
                    "Feels like %.0f°C", current.getFeelslikeC()));

            tvHumidity.setText(current.getHumidity() + "%");

            tvWind.setText(String.format(Locale.getDefault(),
                    "%.0f km/h", current.getWindKph()));

            tvUvIndex.setText(String.format(Locale.getDefault(),
                    "%.0f", current.getUv()));
        }

        displayForecast(weather);
    }

    private void displayForecast(WeatherResponse weather) {
        if (weather.getForecast() == null ||
                weather.getForecast().getForecastday() == null) return;

        forecastContainer.removeAllViews();

        SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat output = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());

        for (WeatherResponse.ForecastDay day : weather.getForecast().getForecastday()) {
            View dayView = getLayoutInflater().inflate(R.layout.item_forecast_detail, forecastContainer, false);

            TextView tvDay = dayView.findViewById(R.id.forecastDay);
            ImageView ivIcon = dayView.findViewById(R.id.forecastIcon);
            TextView tvHighLow = dayView.findViewById(R.id.forecastHighLow);
            TextView tvRain = dayView.findViewById(R.id.forecastRain);

            try {
                Date date = input.parse(day.getDate());
                tvDay.setText(output.format(date));
            } catch (Exception e) {
                tvDay.setText("--");
            }

            setWeatherIcon(ivIcon, day.getDay().getCondition().getText());

            tvHighLow.setText(String.format(Locale.getDefault(),
                    "H:%.0f° L:%.0f°",
                    day.getDay().getMaxtempC(),
                    day.getDay().getMintempC()));

            tvRain.setText(day.getDay().getDailyChanceOfRain() + "%");

            forecastContainer.addView(dayView);
        }
    }

    private void generateFarmAdvice(WeatherResponse weather) {
        if (weather.getForecast() == null) return;

        WeatherResponse.ForecastDay today = weather.getForecast().getForecastday().get(0);
        int rainChance = today.getDay().getDailyChanceOfRain();
        double temp = today.getDay().getAvgtempC();

        String advice;
        int adviceIcon;

        if (rainChance > 70) {
            advice = "Heavy rain expected. Avoid planting and spraying today.";
            adviceIcon = R.drawable.ic_warning;
        } else if (rainChance > 40) {
            advice = "Scattered showers. Good day for indoor maintenance tasks.";
            adviceIcon = R.drawable.ic_rain;
        } else if (temp > 35) {
            advice = "Very hot. Schedule work for early morning or evening.";
            adviceIcon = R.drawable.ic_wb_sunny;
        } else if (temp > 25) {
            advice = "Perfect conditions for planting and fertilization.";
            adviceIcon = R.drawable.ic_park;
        } else {
            advice = "Cool weather. Good for pruning and weeding.";
            adviceIcon = R.drawable.ic_air;
        }

        tvFarmAdvice.setText(advice);

        ImageView adviceIconView = adviceCard.findViewById(R.id.adviceIcon);
        if (adviceIconView != null) {
            adviceIconView.setImageResource(adviceIcon);
        }
    }

    private void setWeatherIcon(ImageView imageView, String condition) {
        String lowerCondition = condition.toLowerCase();

        if (lowerCondition.contains("sunny") || lowerCondition.contains("clear")) {
            imageView.setImageResource(R.drawable.ic_wb_sunny);
        } else if (lowerCondition.contains("partly cloudy")) {
            imageView.setImageResource(R.drawable.ic_partly_cloudy);
        } else if (lowerCondition.contains("cloudy")) {
            imageView.setImageResource(R.drawable.ic_cloud);
        } else if (lowerCondition.contains("rain")) {
            imageView.setImageResource(R.drawable.ic_rain);
        } else if (lowerCondition.contains("storm") || lowerCondition.contains("thunder")) {
            imageView.setImageResource(R.drawable.ic_storm);
        } else if (lowerCondition.contains("snow")) {
            imageView.setImageResource(R.drawable.ic_snow);
        } else if (lowerCondition.contains("fog") || lowerCondition.contains("mist")) {
            imageView.setImageResource(R.drawable.ic_fog);
        } else {
            imageView.setImageResource(R.drawable.ic_wb_sunny);
        }

        imageView.setColorFilter(getColor(R.color.accent));
    }

    @Override
    protected boolean checkRoleAccess() {
        return true;
    }
}