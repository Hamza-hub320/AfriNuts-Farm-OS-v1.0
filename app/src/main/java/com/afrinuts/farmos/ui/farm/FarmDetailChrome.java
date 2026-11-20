package com.afrinuts.farmos.ui.farm;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.Toolbar;

/** Shared visual chrome for record details opened from the Farm Hub. */
public final class FarmDetailChrome {
    private FarmDetailChrome() { }

    public static void apply(Activity activity, Toolbar toolbar) {
        toolbar.setBackgroundColor(Color.TRANSPARENT);
        toolbar.setElevation(0f);
        toolbar.setContentInsetsRelative(0, 0);
        toolbar.setNavigationIcon(com.afrinuts.farmos.R.drawable.ic_farm_detail_back);
        toolbar.setNavigationOnClickListener(v -> activity.finish());
        if (activity instanceof AppCompatActivity) {
            AppCompatActivity app = (AppCompatActivity) activity;
            if (app.getSupportActionBar() != null) app.getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        for (int i = 0; i < toolbar.getChildCount(); i++) {
            View child = toolbar.getChildAt(i);
            if (child instanceof ActionMenuView) {
                ActionMenuView actions = (ActionMenuView) child;
                for (int j = 0; j < actions.getChildCount(); j++) {
                    View action = actions.getChildAt(j);
                    GradientDrawable circle = new GradientDrawable();
                    circle.setColor(Color.WHITE);
                    circle.setCornerRadius(activity.getResources().getDisplayMetrics().density * 22);
                    circle.setStroke(Math.max(1, Math.round(activity.getResources().getDisplayMetrics().density)), 0xFFE9E1CF);
                    action.setBackground(circle);
                    action.setMinimumWidth(dp(activity, 44));
                    action.setMinimumHeight(dp(activity, 44));
                    if (action instanceof TextView) ((TextView) action).setTextColor(0xFF1C1B1A);
                }
            }
        }
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
