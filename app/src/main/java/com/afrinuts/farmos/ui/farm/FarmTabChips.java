package com.afrinuts.farmos.ui.farm;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public final class FarmTabChips {
    private FarmTabChips() { }
    public static void style(ChipGroup group) {
        if (group == null) return;
        float density = group.getResources().getDisplayMetrics().density;
        int[][] states = new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}};
        int[] textColors = new int[]{0xFFFFFFFF, 0xFF59544C};
        int[] fills = new int[]{0xFF191817, 0x00000000};
        int[] strokes = new int[]{0xFF191817, 0xFFE9E1CF};
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (!(child instanceof Chip)) continue;
            Chip chip = (Chip) child;
            chip.setTextColor(new ColorStateList(states, textColors));
            chip.setChipBackgroundColor(new ColorStateList(states, fills));
            chip.setChipStrokeColor(new ColorStateList(states, strokes));
            chip.setChipStrokeWidth(density);
            chip.setChipCornerRadius(999 * density);
            chip.setChipMinHeight(38 * density);
            chip.setTextSize(13);
            chip.setTypeface(Typeface.create("sans-serif-bold", Typeface.NORMAL));
            chip.setPadding(dp(chip, 14), 0, dp(chip, 14), 0);
        }
    }
    private static int dp(Chip chip, int value) { return Math.round(value * chip.getResources().getDisplayMetrics().density); }
}
