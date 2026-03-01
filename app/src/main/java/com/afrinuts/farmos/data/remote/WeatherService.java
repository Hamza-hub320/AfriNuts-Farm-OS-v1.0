package com.afrinuts.farmos.data.remote;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherService {

    @GET("current.json")
    Call<WeatherResponse> getCurrentWeather(
            @Query("key") String apiKey,
            @Query("q") String location,
            @Query("aqi") String aqi
    );

    @GET("forecast.json")
    Call<WeatherResponse> getWeatherForecast(
            @Query("key") String apiKey,
            @Query("q") String location,
            @Query("days") int days,
            @Query("aqi") String aqi,
            @Query("alerts") String alerts
    );
}