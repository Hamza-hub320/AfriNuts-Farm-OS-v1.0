package com.afrinuts.farmos.ui.weather;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.remote.WeatherResponse;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WeatherForecastDialog extends DialogFragment {

    private static final String ARG_WEATHER = "weather";

    private WeatherResponse weather;

    private TextView tvLocation;
    private TextView tvLastUpdated;
    private TextView tvCurrentTemp;
    private TextView tvCurrentCondition;
    private TextView tvFeelsLike;
    private TextView tvHumidity;
    private TextView tvWind;
    private TextView tvUvIndex;
    private ImageView ivCurrentIcon;
    private LinearLayout forecastContainer;

    public static WeatherForecastDialog newInstance(WeatherResponse weather) {
        WeatherForecastDialog dialog = new WeatherForecastDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_WEATHER, weather);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            weather = (WeatherResponse) getArguments().getSerializable(ARG_WEATHER);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_weather_forecast, null);

        initViews(view);
        displayWeatherData();

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Weather Forecast")
                .setView(view)
                .setPositiveButton("Close", null)
                .create();
    }

    private void initViews(View view) {
        tvLocation = view.findViewById(R.id.tvLocation);
        tvLastUpdated = view.findViewById(R.id.tvLastUpdated);
        tvCurrentTemp = view.findViewById(R.id.tvCurrentTemp);
        tvCurrentCondition = view.findViewById(R.id.tvCurrentCondition);
        tvFeelsLike = view.findViewById(R.id.tvFeelsLike);
        tvHumidity = view.findViewById(R.id.tvHumidity);
        tvWind = view.findViewById(R.id.tvWind);
        tvUvIndex = view.findViewById(R.id.tvUvIndex);
        ivCurrentIcon = view.findViewById(R.id.ivCurrentIcon);
        forecastContainer = view.findViewById(R.id.forecastContainer);
    }

    private void displayWeatherData() {
        if (weather == null) return;

        WeatherResponse.Location location = weather.getLocation();
        WeatherResponse.Current current = weather.getCurrent();

        // Location info
        if (location != null) {
            tvLocation.setText(location.getName() + ", " + location.getCountry());

            // Format local time
            if (location.getLocaltime() != null) {
                tvLastUpdated.setText("Last updated: " + location.getLocaltime());
            }
        }

        // Current weather
        if (current != null) {
            tvCurrentTemp.setText(String.format(Locale.getDefault(), "%.0f°C", current.getTempC()));
            tvCurrentCondition.setText(current.getCondition().getText());
            tvFeelsLike.setText(String.format(Locale.getDefault(), "Feels like: %.0f°C", current.getFeelslikeC()));
            tvHumidity.setText(String.format(Locale.getDefault(), "%d%%", current.getHumidity()));
            tvWind.setText(String.format(Locale.getDefault(), "%.0f km/h", current.getWindKph()));
            tvUvIndex.setText(String.format(Locale.getDefault(), "%.0f", current.getUv()));

            // Set weather icon
            setWeatherIcon(ivCurrentIcon, current.getCondition().getText());
        }

        // Display forecast if available
        if (weather.getForecast() != null && weather.getForecast().getForecastday() != null) {
            displayForecast();
        }
    }

    private void displayForecast() {
        forecastContainer.removeAllViews();

        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("EEEE", Locale.getDefault());

        for (WeatherResponse.ForecastDay day : weather.getForecast().getForecastday()) {
            View dayView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_forecast_day, forecastContainer, false);

            TextView tvDay = dayView.findViewById(R.id.forecastDay);
            ImageView ivIcon = dayView.findViewById(R.id.forecastIcon);
            TextView tvTemp = dayView.findViewById(R.id.forecastTemp);
            TextView tvRain = dayView.findViewById(R.id.forecastRain);

            try {
                Date date = inputFormat.parse(day.getDate());
                tvDay.setText(outputFormat.format(date));
            } catch (Exception e) {
                tvDay.setText("--");
            }

            // Set icon based on condition
            setWeatherIcon(ivIcon, day.getDay().getCondition().getText());

            tvTemp.setText(String.format(Locale.getDefault(),
                    "%.0f°", day.getDay().getAvgtempC()));
            tvRain.setText(day.getDay().getDailyChanceOfRain() + "%");

            forecastContainer.addView(dayView);
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
            imageView.setImageResource(R.drawable.ic_wb_sunny); // Default
        }
    }
}