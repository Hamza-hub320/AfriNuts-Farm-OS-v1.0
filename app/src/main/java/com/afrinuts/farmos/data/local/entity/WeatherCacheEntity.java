package com.afrinuts.farmos.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "weather_cache")
public class WeatherCacheEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String location;
    private long timestamp;
    private String weatherData; // JSON string
    private int forecastDays;

    public WeatherCacheEntity(String location, long timestamp, String weatherData, int forecastDays) {
        this.location = location;
        this.timestamp = timestamp;
        this.weatherData = weatherData;
        this.forecastDays = forecastDays;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getWeatherData() { return weatherData; }
    public void setWeatherData(String weatherData) { this.weatherData = weatherData; }

    public int getForecastDays() { return forecastDays; }
    public void setForecastDays(int forecastDays) { this.forecastDays = forecastDays; }

    public boolean isExpired() {
        long oneHourInMs = 60 * 60 * 1000;
        return System.currentTimeMillis() - timestamp > oneHourInMs;
    }
}