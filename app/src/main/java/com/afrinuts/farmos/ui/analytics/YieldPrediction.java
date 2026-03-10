package com.afrinuts.farmos.ui.analytics;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class YieldPrediction {
    private int year;
    private double predictedYieldKg;
    private double confidenceLevel; // 0-100%
    private double estimatedRevenue;
    private String riskLevel; // "Low", "Medium", "High"
    private String recommendation;

    public YieldPrediction(int year, double predictedYieldKg, double confidenceLevel,
                           double estimatedRevenue, String riskLevel, String recommendation) {
        this.year = year;
        this.predictedYieldKg = predictedYieldKg;
        this.confidenceLevel = confidenceLevel;
        this.estimatedRevenue = estimatedRevenue;
        this.riskLevel = riskLevel;
        this.recommendation = recommendation;
    }

    public int getYear() { return year; }
    public double getPredictedYieldKg() { return predictedYieldKg; }
    public double getConfidenceLevel() { return confidenceLevel; }
    public double getEstimatedRevenue() { return estimatedRevenue; }
    public String getRiskLevel() { return riskLevel; }
    public String getRecommendation() { return recommendation; }

    public String getFormattedYield() {
        return String.format(Locale.getDefault(), "%,.0f kg", predictedYieldKg);
    }

    public String getFormattedRevenue() {
        return String.format(Locale.getDefault(), "%,.0f XAF", estimatedRevenue);
    }

    public int getRiskColor() {
        switch (riskLevel) {
            case "Low": return android.R.color.holo_green_dark;
            case "Medium": return android.R.color.holo_orange_dark;
            case "High": return android.R.color.holo_red_dark;
            default: return android.R.color.black;
        }
    }
}