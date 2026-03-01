package com.afrinuts.farmos.data.remote;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class WeatherResponse {

    @SerializedName("location")
    private Location location;

    @SerializedName("current")
    private Current current;

    @SerializedName("forecast")
    private Forecast forecast;

    public Location getLocation() { return location; }
    public Current getCurrent() { return current; }
    public Forecast getForecast() { return forecast; }

    public static class Location {
        @SerializedName("name")
        private String name;

        @SerializedName("region")
        private String region;

        @SerializedName("country")
        private String country;

        @SerializedName("lat")
        private double lat;

        @SerializedName("lon")
        private double lon;

        @SerializedName("localtime")
        private String localtime;

        public String getName() { return name; }
        public String getRegion() { return region; }
        public String getCountry() { return country; }
        public double getLat() { return lat; }
        public double getLon() { return lon; }
        public String getLocaltime() { return localtime; }
    }

    public static class Current {
        @SerializedName("temp_c")
        private double tempC;

        @SerializedName("temp_f")
        private double tempF;

        @SerializedName("condition")
        private Condition condition;

        @SerializedName("wind_kph")
        private double windKph;

        @SerializedName("humidity")
        private int humidity;

        @SerializedName("cloud")
        private int cloud;

        @SerializedName("feelslike_c")
        private double feelslikeC;

        @SerializedName("uv")
        private double uv;

        public double getTempC() { return tempC; }
        public double getTempF() { return tempF; }
        public Condition getCondition() { return condition; }
        public double getWindKph() { return windKph; }
        public int getHumidity() { return humidity; }
        public int getCloud() { return cloud; }
        public double getFeelslikeC() { return feelslikeC; }
        public double getUv() { return uv; }
    }

    public static class Condition {
        @SerializedName("text")
        private String text;

        @SerializedName("icon")
        private String icon;

        @SerializedName("code")
        private int code;

        public String getText() { return text; }
        public String getIcon() { return icon; }
        public int getCode() { return code; }
    }

    public static class Forecast {
        @SerializedName("forecastday")
        private List<ForecastDay> forecastday;

        public List<ForecastDay> getForecastday() { return forecastday; }
    }

    public static class ForecastDay {
        @SerializedName("date")
        private String date;

        @SerializedName("day")
        private Day day;

        @SerializedName("hour")
        private List<Hour> hour;

        public String getDate() { return date; }
        public Day getDay() { return day; }
        public List<Hour> getHour() { return hour; }
    }

    public static class Day {
        @SerializedName("maxtemp_c")
        private double maxtempC;

        @SerializedName("mintemp_c")
        private double mintempC;

        @SerializedName("avgtemp_c")
        private double avgtempC;

        @SerializedName("maxwind_kph")
        private double maxwindKph;

        @SerializedName("totalprecip_mm")
        private double totalprecipMm;

        @SerializedName("avghumidity")
        private double avghumidity;

        @SerializedName("daily_will_it_rain")
        private int dailyWillItRain;

        @SerializedName("daily_chance_of_rain")
        private int dailyChanceOfRain;

        @SerializedName("condition")
        private Condition condition;

        public double getMaxtempC() { return maxtempC; }
        public double getMintempC() { return mintempC; }
        public double getAvgtempC() { return avgtempC; }
        public double getMaxwindKph() { return maxwindKph; }
        public double getTotalprecipMm() { return totalprecipMm; }
        public double getAvghumidity() { return avghumidity; }
        public int getDailyWillItRain() { return dailyWillItRain; }
        public int getDailyChanceOfRain() { return dailyChanceOfRain; }
        public Condition getCondition() { return condition; }
    }

    public static class Hour {
        @SerializedName("time")
        private String time;

        @SerializedName("temp_c")
        private double tempC;

        @SerializedName("condition")
        private Condition condition;

        @SerializedName("chance_of_rain")
        private int chanceOfRain;

        public String getTime() { return time; }
        public double getTempC() { return tempC; }
        public Condition getCondition() { return condition; }
        public int getChanceOfRain() { return chanceOfRain; }
    }
}