package com.afrinuts.farmos.utils;

import android.content.Context;
import android.content.res.Configuration;
import androidx.core.content.ContextCompat;
import com.afrinuts.farmos.R;

public class ThemeUtils {

    public static boolean isDarkMode(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    public static int getChartTextColor(Context context) {
        if (isDarkMode(context)) {
            return ContextCompat.getColor(context, R.color.text_primary);
        } else {
            return ContextCompat.getColor(context, R.color.text_dark);
        }
    }

    public static int getChartAxisColor(Context context) {
        if (isDarkMode(context)) {
            return ContextCompat.getColor(context, R.color.text_secondary);
        } else {
            return ContextCompat.getColor(context, R.color.navy);
        }
    }
}