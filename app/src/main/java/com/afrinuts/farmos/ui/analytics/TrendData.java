package com.afrinuts.farmos.ui.analytics;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TrendData {
    private String metricName;
    private List<DataPoint> dataPoints = new ArrayList<>();
    private double trendPercentage;
    private boolean isPositive; // true = upward trend, false = downward

    public static class DataPoint {
        private String label;
        private double value;

        public DataPoint(String label, double value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() { return label; }
        public double getValue() { return value; }
    }

    public TrendData(String metricName) {
        this.metricName = metricName;
    }

    public void addDataPoint(String label, double value) {
        dataPoints.add(new DataPoint(label, value));
        calculateTrend();
    }

    private void calculateTrend() {
        if (dataPoints.size() < 2) return;

        double firstValue = dataPoints.get(0).getValue();
        double lastValue = dataPoints.get(dataPoints.size() - 1).getValue();

        if (firstValue == 0) return;

        trendPercentage = ((lastValue - firstValue) / firstValue) * 100;
        isPositive = lastValue > firstValue;
    }

    public String getMetricName() { return metricName; }
    public List<DataPoint> getDataPoints() { return dataPoints; }
    public double getTrendPercentage() { return trendPercentage; }
    public boolean isPositive() { return isPositive; }

    public String getFormattedTrend() {
        return String.format(Locale.getDefault(), "%+.1f%%", trendPercentage);
    }
}