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

import com.afrinuts.farmos.ui.farm.FarmDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Farm-styled source picker for adding an inspection photo. */
public class PhotoSourceDialog extends FarmDialogFragment {
    public static final int SOURCE_CAMERA = 0;
    public static final int SOURCE_GALLERY = 1;

    public interface Listener { void onSourceSelected(int source); }
    private Listener listener;

    public void setListener(Listener listener) { this.listener = listener; }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        float density = getResources().getDisplayMetrics().density;
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(4, density), dp(4, density), dp(4, density), dp(4, density));

        TextView title = new TextView(requireContext());
        title.setText("Add photo");
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(0, 0, dp(48, density), 0);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, dp(44, density));
        titleParams.bottomMargin = dp(12, density);
        content.addView(title, titleParams);

        addOption(content, "Camera", android.R.drawable.ic_menu_camera, SOURCE_CAMERA, density);
        addOption(content, "Gallery", android.R.drawable.ic_menu_gallery, SOURCE_GALLERY, density);

        Button cancel = new Button(requireContext());
        cancel.setText("Cancel");
        cancel.setAllCaps(false);
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(-1, dp(52, density));
        cancelParams.topMargin = dp(20, density);
        content.addView(cancel, cancelParams);
        cancel.setOnClickListener(v -> dismiss());

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(content)
                .setCancelable(true)
                .create();
    }

    private void addOption(LinearLayout content, String label, int iconRes, int source, float density) {
        int green = 0xFF0E6B3C;
        FrameLayout row = new FrameLayout(requireContext());
        row.setBackground(outline(density));
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> {
            Listener callback = listener;
            dismiss();
            if (callback != null) callback.onSourceSelected(source);
        });

        LinearLayout contents = new LinearLayout(requireContext());
        contents.setOrientation(LinearLayout.HORIZONTAL);
        contents.setGravity(Gravity.CENTER_VERTICAL);
        contents.setPadding(dp(12, density), 0, dp(12, density), 0);
        row.addView(contents, new FrameLayout.LayoutParams(-1, -1));

        FrameLayout iconTile = new FrameLayout(requireContext());
        iconTile.setBackground(rounded(0xFFE3F0E7, dp(36, density)));
        ImageView icon = new ImageView(requireContext());
        icon.setImageResource(iconRes);
        icon.setColorFilter(green);
        iconTile.addView(icon, new FrameLayout.LayoutParams(dp(20, density), dp(20, density), Gravity.CENTER));
        contents.addView(iconTile, new LinearLayout.LayoutParams(dp(36, density), dp(36, density)));

        TextView text = new TextView(requireContext());
        text.setText(label);
        text.setTextColor(0xFF1C1B1A);
        text.setTextSize(15);
        text.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, -2, 1);
        textParams.setMargins(dp(12, density), 0, 0, 0);
        contents.addView(text, textParams);

        ImageView arrow = new ImageView(requireContext());
        arrow.setImageResource(com.afrinuts.farmos.R.drawable.ic_arrow_forward);
        arrow.setColorFilter(0xFF59544C);
        FrameLayout arrowTile = new FrameLayout(requireContext());
        arrowTile.setBackground(rounded(0xFFF3EAD8, dp(999, density)));
        arrowTile.addView(arrow, new FrameLayout.LayoutParams(dp(16, density), dp(16, density), Gravity.CENTER));
        contents.addView(arrowTile, new LinearLayout.LayoutParams(dp(32, density), dp(32, density)));

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, dp(56, density));
        rowParams.bottomMargin = dp(8, density);
        content.addView(row, rowParams);
    }

    private int dp(float value, float density) { return Math.round(value * density); }

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
