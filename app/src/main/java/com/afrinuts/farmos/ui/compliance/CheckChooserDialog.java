package com.afrinuts.farmos.ui.compliance;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.ui.farm.FarmDialogFragment;

/** Standard Farm sheet for choosing which kind of compliance check to add. */
public class CheckChooserDialog extends FarmDialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        int green = 0xFF0E6B3C;
        int dark = 0xFF1C1B1A;
        int muted = 0xFF59544C;

        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(4, density), dp(4, density), dp(4, density), dp(4, density));

        TextView title = new TextView(requireContext());
        title.setText("Add check");
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(0, 0, dp(48, density), 0);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, dp(44, density));
        titleParams.bottomMargin = dp(12, density);
        content.addView(title, titleParams);

        String[] labels = {"Add audit", "Add compliance inspection", "Add verification"};
        int[] icons = {R.drawable.ic_audit, R.drawable.ic_inspection, R.drawable.ic_verified};
        int[] tileColors = {0xFFE3F0E7, 0xFFF3EAD8, 0xFFE3F0E7};
        for (int i = 0; i < labels.length; i++) {
            final int choice = i;
            FrameLayout row = new FrameLayout(requireContext());
            row.setBackground(outline(density));
            row.setClickable(true);
            row.setFocusable(true);
            row.setOnClickListener(v -> {
                if (getActivity() instanceof ComplianceHubActivity) {
                    ((ComplianceHubActivity) getActivity()).showCheckChoice(choice);
                }
                dismiss();
            });

            FrameLayout tile = new FrameLayout(requireContext());
            tile.setBackground(rounded(tileColors[i], dp(12, density)));
            ImageView icon = new ImageView(requireContext());
            icon.setImageResource(icons[i]);
            icon.setColorFilter(green);
            icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            tile.addView(icon, new FrameLayout.LayoutParams(dp(20, density), dp(20, density), Gravity.CENTER));
            android.widget.RelativeLayout rowContents = new android.widget.RelativeLayout(requireContext());
            rowContents.setPadding(dp(12, density), 0, dp(10, density), 0);
            row.addView(rowContents, new FrameLayout.LayoutParams(-1, -1));
            int tileId = View.generateViewId();
            tile.setId(tileId);
            android.widget.RelativeLayout.LayoutParams tileParams = new android.widget.RelativeLayout.LayoutParams(
                    dp(36, density), dp(36, density));
            tileParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_START);
            tileParams.addRule(android.widget.RelativeLayout.CENTER_VERTICAL);
            rowContents.addView(tile, tileParams);

            TextView label = new TextView(requireContext());
            label.setText(labels[i]);
            label.setTextColor(dark);
            label.setTextSize(15);
            label.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
            label.setId(View.generateViewId());

            FrameLayout arrowTile = new FrameLayout(requireContext());
            arrowTile.setBackground(rounded(0xFFF3EAD8, dp(999, density)));
            int arrowId = View.generateViewId();
            arrowTile.setId(arrowId);
            ImageView arrow = new ImageView(requireContext());
            arrow.setImageResource(R.drawable.ic_arrow_forward);
            arrow.setColorFilter(muted);
            arrowTile.addView(arrow, new FrameLayout.LayoutParams(dp(16, density), dp(16, density), Gravity.CENTER));
            android.widget.RelativeLayout.LayoutParams arrowTileParams = new android.widget.RelativeLayout.LayoutParams(
                    dp(32, density), dp(32, density));
            arrowTileParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_END);
            arrowTileParams.addRule(android.widget.RelativeLayout.CENTER_VERTICAL);
            rowContents.addView(arrowTile, arrowTileParams);
            android.widget.RelativeLayout.LayoutParams labelParams = new android.widget.RelativeLayout.LayoutParams(
                    android.widget.RelativeLayout.LayoutParams.MATCH_PARENT, android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT);
            labelParams.addRule(android.widget.RelativeLayout.RIGHT_OF, tileId);
            labelParams.addRule(android.widget.RelativeLayout.LEFT_OF, arrowId);
            labelParams.addRule(android.widget.RelativeLayout.CENTER_VERTICAL);
            labelParams.setMargins(dp(12, density), 0, dp(8, density), 0);
            rowContents.addView(label, labelParams);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, dp(56, density));
            if (i > 0) rowParams.topMargin = dp(8, density);
            content.addView(row, rowParams);
        }

        Button cancel = new Button(requireContext());
        cancel.setText("Cancel");
        cancel.setAllCaps(false);
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(-1, dp(52, density));
        cancelParams.topMargin = dp(20, density);
        content.addView(cancel, cancelParams);
        cancel.setOnClickListener(v -> dismiss());

        return new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setView(content)
                .setCancelable(true)
                .create();
    }

    private int dp(float value, float density) {
        return Math.round(value * density);
    }

    private android.graphics.drawable.GradientDrawable outline(float density) {
        android.graphics.drawable.GradientDrawable bg = rounded(0xFFFFFFFF, dp(16, density));
        bg.setStroke(Math.max(1, dp(1, density)), 0xFFE9E1CF);
        return bg;
    }

    private android.graphics.drawable.GradientDrawable rounded(int color, float radius) {
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(radius);
        return bg;
    }
}
