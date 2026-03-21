package com.afrinuts.farmos.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.text.DecimalFormat;
import java.util.Locale;

public class UnitHelper {

    private static final String PREF_UNITS = "units";
    private static final double KG_TO_LB = 2.20462;
    private static final double XAF_TO_USD = 0.0016; // Approximate rate

    public static boolean isMetric(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PREF_UNITS, "metric").equals("metric");
    }

    public static String formatWeight(Context context, double kg) {
        if (isMetric(context)) {
            return String.format(Locale.getDefault(), "%.1f kg", kg);
        } else {
            double lb = kg * KG_TO_LB;
            return String.format(Locale.getDefault(), "%.1f lb", lb);
        }
    }

    public static String formatCurrency(Context context, double xaf) {
        if (isMetric(context)) {
            return String.format(Locale.getDefault(), "%,.0f XAF", xaf);
        } else {
            double usd = xaf * XAF_TO_USD;
            return String.format(Locale.getDefault(), "%.2f USD", usd);
        }
    }

    public static String formatArea(Context context, double hectares) {
        if (isMetric(context)) {
            return String.format(Locale.getDefault(), "%.1f ha", hectares);
        } else {
            double acres = hectares * 2.47105;
            return String.format(Locale.getDefault(), "%.1f acres", acres);
        }
    }
}