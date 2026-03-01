package com.afrinuts.farmos.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.afrinuts.farmos.data.local.entity.WeatherCacheEntity;

@Dao
public interface WeatherDao {

    @Insert
    void insert(WeatherCacheEntity weatherCache);

    @Query("SELECT * FROM weather_cache WHERE location = :location AND forecastDays = :forecastDays ORDER BY timestamp DESC LIMIT 1")
    WeatherCacheEntity getLatestWeather(String location, int forecastDays);

    @Query("DELETE FROM weather_cache WHERE timestamp < :expiryTime")
    void deleteOldCache(long expiryTime);
}