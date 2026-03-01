package com.afrinuts.farmos.data.repository;

import android.content.Context;

import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.WeatherCacheEntity;
import com.afrinuts.farmos.data.remote.WeatherApiClient;
import com.afrinuts.farmos.data.remote.WeatherResponse;
import com.afrinuts.farmos.data.remote.WeatherService;
import com.google.gson.Gson;

import java.io.IOException;

import retrofit2.Call;

public class WeatherRepository {

    private static final String API_KEY = "96e38ff2387f454aa67191459252008";
    private static final String LOCATION = "Odienné";
    private static final int FORECAST_DAYS = 3;

    private AppDatabase database;
    private WeatherService weatherService;
    private Gson gson;

    public WeatherRepository(Context context) {
        this.database = AppDatabase.getInstance(context);
        this.weatherService = WeatherApiClient.getClient();
        this.gson = new Gson();
    }

    public interface WeatherCallback {
        void onSuccess(WeatherResponse weather);
        void onError(String error);
    }

    public void getCurrentWeather(WeatherCallback callback) {
        // Check cache first
        WeatherCacheEntity cached = database.weatherDao()
                .getLatestWeather(LOCATION, 0); // 0 = current only

        if (cached != null && !cached.isExpired()) {
            // Use cached data
            WeatherResponse weather = gson.fromJson(cached.getWeatherData(), WeatherResponse.class);
            callback.onSuccess(weather);
            return;
        }

        // Fetch from API
        new Thread(() -> {
            try {
                Call<WeatherResponse> call = weatherService.getCurrentWeather(
                        API_KEY, LOCATION, "no");
                WeatherResponse response = call.execute().body();

                if (response != null) {
                    // Cache the result
                    String jsonData = gson.toJson(response);
                    WeatherCacheEntity cache = new WeatherCacheEntity(
                            LOCATION, System.currentTimeMillis(), jsonData, 0);
                    database.weatherDao().insert(cache);

                    // Clean old cache
                    long oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
                    database.weatherDao().deleteOldCache(oneDayAgo);

                    callback.onSuccess(response);
                } else {
                    callback.onError("Empty response from weather API");
                }
            } catch (IOException e) {
                e.printStackTrace();
                callback.onError("Network error: " + e.getMessage());
            }
        }).start();
    }

    public void getWeatherForecast(WeatherCallback callback) {
        // Check cache first
        WeatherCacheEntity cached = database.weatherDao()
                .getLatestWeather(LOCATION, FORECAST_DAYS);

        if (cached != null && !cached.isExpired()) {
            // Use cached data
            WeatherResponse weather = gson.fromJson(cached.getWeatherData(), WeatherResponse.class);
            callback.onSuccess(weather);
            return;
        }

        // Fetch from API
        new Thread(() -> {
            try {
                Call<WeatherResponse> call = weatherService.getWeatherForecast(
                        API_KEY, LOCATION, FORECAST_DAYS, "no", "no");
                WeatherResponse response = call.execute().body();

                if (response != null) {
                    // Cache the result
                    String jsonData = gson.toJson(response);
                    WeatherCacheEntity cache = new WeatherCacheEntity(
                            LOCATION, System.currentTimeMillis(), jsonData, FORECAST_DAYS);
                    database.weatherDao().insert(cache);

                    // Clean old cache
                    long oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
                    database.weatherDao().deleteOldCache(oneDayAgo);

                    callback.onSuccess(response);
                } else {
                    callback.onError("Empty response from weather API");
                }
            } catch (IOException e) {
                e.printStackTrace();
                callback.onError("Network error: " + e.getMessage());
            }
        }).start();
    }
}