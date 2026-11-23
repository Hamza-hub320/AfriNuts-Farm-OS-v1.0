package com.afrinuts.farmos.ui.compliance;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.afrinuts.farmos.ui.farm.FarmDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class UpdateActionStatusDialog extends FarmDialogFragment {
    private static final String ARG_STATUS = "status";
    private static final String ARG_TITLE = "title";
    private static final String[] VALUES = {"OPEN", "IN_PROGRESS", "CLOSED"};
    private static final String[] LABELS = {
            "Open · Not addressed yet", "In progress · Being addressed", "Closed · Resolved"
    };

    public interface Listener { void onStatusSelected(String status); }
    private Listener listener;
    private int selected = 0;

    public static UpdateActionStatusDialog newInstance(String status) {
        return newInstance(status, "Update action status");
    }

    public static UpdateActionStatusDialog newInstance(String status, String title) {
        UpdateActionStatusDialog dialog = new UpdateActionStatusDialog();
        Bundle args = new Bundle();
        args.putString(ARG_STATUS, status);
        args.putString(ARG_TITLE, title);
        dialog.setArguments(args);
        return dialog;
    }

    public void setListener(Listener listener) { this.listener = listener; }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        String status = getArguments() == null ? "OPEN" : getArguments().getString(ARG_STATUS, "OPEN");
        for (int i = 0; i < VALUES.length; i++) if (VALUES[i].equalsIgnoreCase(status)) selected = i;

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(0), dp(0), dp(0), dp(0));

        TextView title = new TextView(requireContext());
        title.setText(getArguments() == null
                ? "Update action status"
                : getArguments().getString(ARG_TITLE, "Update action status"));
        title.setTextColor(Color.parseColor("#1C1B1A"));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setPadding(0, 0, dp(44), dp(16));
        root.addView(title);

        LinearLayout rows = new LinearLayout(requireContext());
        rows.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < VALUES.length; i++) {
            final int index = i;
            FrameLayout row = new FrameLayout(requireContext());
            row.setMinimumHeight(dp(56));
            row.setBackground(rowBackground(i == selected));
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.bottomMargin = dp(8);
            rows.addView(row, rowParams);

            LinearLayout contents = new LinearLayout(requireContext());
            contents.setGravity(Gravity.CENTER_VERTICAL);
            contents.setPadding(dp(12), dp(8), dp(12), dp(8));
            row.addView(contents, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL));

            RadioButton radio = new RadioButton(requireContext());
            radio.setButtonTintList(android.content.res.ColorStateList.valueOf(
                    i == selected ? Color.parseColor("#0A5530") : Color.parseColor("#C9C1B2")));
            radio.setChecked(i == selected);
            contents.addView(radio, new LinearLayout.LayoutParams(dp(36), dp(40)));

            TextView label = new TextView(requireContext());
            label.setText(LABELS[i]);
            label.setTextColor(Color.parseColor("#1C1B1A"));
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            contents.addView(label, labelParams);

            View.OnClickListener select = v -> {
                selected = index;
                for (int j = 0; j < rows.getChildCount(); j++) {
                    FrameLayout option = (FrameLayout) rows.getChildAt(j);
                    LinearLayout optionContents = (LinearLayout) option.getChildAt(0);
                    RadioButton button = (RadioButton) optionContents.getChildAt(0);
                    button.setChecked(j == selected);
                    button.setButtonTintList(android.content.res.ColorStateList.valueOf(
                            j == selected ? Color.parseColor("#0A5530") : Color.parseColor("#C9C1B2")));
                    option.setBackground(rowBackground(j == selected));
                }
            };
            row.setOnClickListener(select);
            radio.setOnClickListener(select);
            label.setOnClickListener(select);
        }
        root.addView(rows);

        LinearLayout actions = new LinearLayout(requireContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(0, 0, 0, 0);
        Button cancel = new Button(requireContext());
        cancel.setText("Cancel");
        cancel.setAllCaps(false);
        Button save = new Button(requireContext());
        save.setText("Save");
        save.setAllCaps(false);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(0, dp(52), 1);
        actions.addView(cancel, actionParams);
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(0, dp(52), 1);
        saveParams.leftMargin = dp(12);
        actions.addView(save, saveParams);
        root.addView(actions);
        cancel.setOnClickListener(v -> dismiss());
        save.setOnClickListener(v -> {
            if (listener != null) listener.onStatusSelected(VALUES[selected]);
            dismiss();
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext()).setView(root).create();
        return dialog;
    }

    private GradientDrawable rowBackground(boolean selected) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(selected ? Color.parseColor("#E3F0E7") : Color.WHITE);
        drawable.setCornerRadius(dp(16));
        drawable.setStroke(dp(selected ? 2 : 1), selected
                ? Color.parseColor("#0A5530") : Color.parseColor("#E9E1CF"));
        return drawable;
    }

    private int dp(float value) {
        return Math.round(value * requireContext().getResources().getDisplayMetrics().density);
    }
}
