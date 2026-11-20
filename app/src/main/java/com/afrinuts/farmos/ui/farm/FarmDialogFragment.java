package com.afrinuts.farmos.ui.farm;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

/** Shared white sheet styling for dialogs launched from Farm workflows. */
public abstract class FarmDialogFragment extends DialogFragment {
    @Override public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog == null) return;
        Window window = dialog.getWindow();
        if (window == null) return;
        float density = requireContext().getResources().getDisplayMetrics().density;
        window.setBackgroundDrawableResource(android.R.color.transparent);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams params = window.getAttributes();
        params.dimAmount = .48f;
        params.gravity = Gravity.BOTTOM;
        params.y = Math.round(16 * density);
        window.setAttributes(params);
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        View sheet = getView();
        if (sheet != null) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.WHITE);
            bg.setCornerRadius(28 * density);
            sheet.setBackground(bg);
            sheet.setClipToOutline(true);
            sheet.setPadding(Math.round(24 * density), Math.round(24 * density),
                    Math.round(24 * density), Math.round(24 * density));
            styleInputs(sheet, density);
        }
        dialog.setCanceledOnTouchOutside(true);
        Button cancel = dialog.findViewById(android.R.id.button2);
        Button primary = dialog.findViewById(android.R.id.button1);
        styleAction(cancel, false, density);
        styleAction(primary, true, density);
    }

    private void styleInputs(View view, float density) {
        if (view instanceof EditText) {
            EditText input = (EditText) view;
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0xFFF6F1E7);
            bg.setCornerRadius(16 * density);
            bg.setStroke(Math.max(1, Math.round(density)), 0xFFE9E1CF);
            input.setBackground(bg);
            input.setMinHeight(Math.round(52 * density));
            input.setPadding(Math.round(16 * density), input.getPaddingTop(), Math.round(16 * density), input.getPaddingBottom());
            input.setTextSize(15);
            input.setTextColor(0xFF1C1B1A);
            input.setHintTextColor(0xFF8A857B);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) styleInputs(group.getChildAt(i), density);
        }
    }

    private void styleAction(Button button, boolean primary, float density) {
        if (button == null) return;
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(primary ? 0xFF0E6B3C : 0xFFF3EAD8);
        bg.setCornerRadius(999 * density);
        button.setBackground(bg);
        button.setTextColor(primary ? Color.WHITE : 0xFF1C1B1A);
        button.setTextSize(15);
        button.setTypeface(android.graphics.Typeface.create("sans-serif-black", android.graphics.Typeface.NORMAL));
        button.setMinHeight(Math.round(52 * density));
    }
}
