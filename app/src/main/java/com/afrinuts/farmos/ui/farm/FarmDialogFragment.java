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
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import com.afrinuts.farmos.R;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.textfield.TextInputLayout;

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
        int screenWidth = requireContext().getResources().getDisplayMetrics().widthPixels;
        window.setLayout(screenWidth - Math.round(32 * density), ViewGroup.LayoutParams.WRAP_CONTENT);
        View titlePanel = dialog.findViewById(androidx.appcompat.R.id.topPanel);
        if (titlePanel != null) titlePanel.setVisibility(View.GONE);
        ViewGroup customSlot = dialog.findViewById(androidx.appcompat.R.id.custom);
        if (customSlot != null && customSlot.getChildCount() > 0) {
            customSlot.setPadding(0, 0, 0, 0);
            View content = customSlot.getChildAt(0);
            removeGlassDecor(content, density);
            styleCardSurfaces(content, density);
            styleInputs(content, density);
            styleText(content);
            if (content instanceof ViewGroup) content.setBackgroundColor(Color.TRANSPARENT);
            removeLeadingHeaderIcon(content);
            styleDialogTitle(content, true);
            styleButtons(content, density);
            for (int i = customSlot.getChildCount() - 1; i >= 0; i--) {
                View child = customSlot.getChildAt(i);
                if (child != content) customSlot.removeViewAt(i);
            }
            wrapAsSheet(dialog, customSlot, content, density);
            customSlot.bringToFront();
            customSlot.requestLayout();
            window.getDecorView().requestLayout();
        }
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        Button cancel = dialog.findViewById(android.R.id.button2);
        Button primary = dialog.findViewById(android.R.id.button1);
        styleAction(cancel, false, density, false);
        styleAction(primary, true, density, false);
    }

    private void wrapAsSheet(Dialog dialog, ViewGroup slot, View content, float density) {
        ViewGroup actionRow = findActionRow(content);
        if (actionRow != null && actionRow.getParent() instanceof ViewGroup) {
            ((ViewGroup) actionRow.getParent()).removeView(actionRow);
        } else {
            actionRow = null;
        }
        slot.removeView(content);
        FrameLayout sheet = new FrameLayout(requireContext());
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(28 * density);
        sheet.setBackground(bg);
        sheet.setClipToOutline(true);
        sheet.setElevation(28 * density);
        int pad = Math.round(24 * density);
        sheet.setPadding(pad, pad, pad, pad);
        slot.addView(sheet, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        android.widget.LinearLayout body = new android.widget.LinearLayout(requireContext());
        body.setOrientation(android.widget.LinearLayout.VERTICAL);
        body.setGravity(Gravity.TOP);
        FrameLayout.LayoutParams bodyParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sheet.addView(body, bodyParams);
        if (content instanceof android.widget.ScrollView) {
            int screenHeight = requireContext().getResources().getDisplayMetrics().heightPixels;
            int maxScrollHeight = Math.max(Math.round(300 * density),
                    Math.min(Math.round(440 * density), screenHeight - Math.round(280 * density)));
            int scrollHeight = maxScrollHeight;
            body.addView(content, new android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, scrollHeight));
            ((android.widget.ScrollView) content).setFillViewport(false);
            content.setScrollY(0);
        } else {
            body.addView(content, new android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        if (actionRow != null) {
            android.widget.LinearLayout.LayoutParams actions = new android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Math.round(60 * density));
            actions.topMargin = Math.round(24 * density);
            body.addView(actionRow, actions);
        }

        ImageButton close = new ImageButton(requireContext());
        close.setImageResource(com.afrinuts.farmos.R.drawable.ic_farm_dialog_close);
        close.setContentDescription("Close dialog");
        close.setScaleType(android.widget.ImageView.ScaleType.CENTER);
        close.setPadding(0, 0, 0, 0);
        close.setBackground(circleBackground(density));
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(
                Math.round(36 * density), Math.round(36 * density), Gravity.TOP | Gravity.END);
        closeParams.topMargin = Math.round(8 * density);
        sheet.addView(close, closeParams);
        sheet.bringChildToFront(close);
        close.setOnClickListener(v -> dialog.dismiss());
    }

    private ViewGroup findActionRow(View view) {
        if (!(view instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) view;
        if (group instanceof android.widget.LinearLayout) {
            java.util.ArrayList<Button> buttons = new java.util.ArrayList<>();
            for (int i = 0; i < group.getChildCount(); i++) {
                if (group.getChildAt(i) instanceof Button) buttons.add((Button) group.getChildAt(i));
            }
            if (buttons.size() == 2) {
                Button first = buttons.get(0);
                Button second = buttons.get(1);
                String firstText = first.getText() == null ? "" : first.getText().toString();
                String secondText = second.getText() == null ? "" : second.getText().toString();
                if (firstText.matches("(?i).*cancel.*") || secondText.matches("(?i).*cancel.*")) {
                    Button cancel = firstText.matches("(?i).*cancel.*") ? first : second;
                    Button primary = cancel == first ? second : first;
                    if (group.indexOfChild(cancel) > group.indexOfChild(primary)) {
                        group.removeView(cancel);
                        group.addView(cancel, 0);
                    }
                    return group;
                }
            }
        }
        for (int i = 0; i < group.getChildCount(); i++) {
            ViewGroup found = findActionRow(group.getChildAt(i));
            if (found != null) return found;
        }
        return null;
    }

    private void removeLeadingHeaderIcon(View view) {
        if (!(view instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) view;
        if (group instanceof android.widget.LinearLayout
                && ((android.widget.LinearLayout) group).getOrientation() == android.widget.LinearLayout.HORIZONTAL
                && group.getChildCount() >= 2 && group.getChildAt(0) instanceof FrameLayout) {
            View candidate = group.getChildAt(1);
            if (candidate instanceof TextView) {
                group.removeViewAt(0);
                ViewGroup.LayoutParams raw = candidate.getLayoutParams();
                if (raw instanceof android.widget.LinearLayout.LayoutParams) {
                    android.widget.LinearLayout.LayoutParams lp = (android.widget.LinearLayout.LayoutParams) raw;
                    lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    lp.weight = 0;
                    candidate.setLayoutParams(lp);
                }
                return;
            }
        }
        for (int i = 0; i < group.getChildCount(); i++) removeLeadingHeaderIcon(group.getChildAt(i));
    }

    private GradientDrawable circleBackground(float density) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFF6F1E7);
        bg.setCornerRadius(999 * density);
        bg.setStroke(Math.max(1, Math.round(density)), 0xFFE9E1CF);
        return bg;
    }

    private void styleCardSurfaces(View view, float density) {
        if (view instanceof androidx.cardview.widget.CardView) {
            androidx.cardview.widget.CardView card = (androidx.cardview.widget.CardView) view;
            card.setCardBackgroundColor(Color.TRANSPARENT);
            card.setCardElevation(0);
            card.setRadius(0);
            card.setUseCompatPadding(false);
            for (int i = 0; i < card.getChildCount(); i++) {
                View inner = card.getChildAt(i);
                if (inner instanceof ViewGroup) inner.setPadding(0, 0, 0, 0);
            }
            TextView heading = firstTextView(card);
            if (heading != null) {
                heading.setTextSize(16);
                heading.setTextColor(0xFF1C1B1A);
                heading.setTypeface(android.graphics.Typeface.create(ResourcesCompat.getFont(requireContext(), R.font.manrope), android.graphics.Typeface.BOLD));
            }
        }
        if (view instanceof android.widget.LinearLayout || view instanceof android.widget.ScrollView) {
            view.setBackgroundColor(Color.TRANSPARENT);
        }
        if (view instanceof android.widget.ScrollView) {
            android.widget.ScrollView scroll = (android.widget.ScrollView) view;
            scroll.setVerticalScrollBarEnabled(false);
            if (scroll.getChildCount() > 0 && scroll.getChildAt(0) instanceof ViewGroup) {
                scroll.getChildAt(0).setPadding(0, 0, 0, 0);
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) styleCardSurfaces(group.getChildAt(i), density);
        }
    }

    private TextView firstTextView(View view) {
        if (view instanceof TextView && !(view instanceof Button) && !(view instanceof EditText)) {
            CharSequence text = ((TextView) view).getText();
            if (text != null && text.length() > 0) return (TextView) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = firstTextView(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private void removeGlassDecor(View view, float density) {
        if (!(view instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) view;
        if (group instanceof FrameLayout) {
            for (int i = group.getChildCount() - 1; i >= 0; i--) {
                View child = group.getChildAt(i);
                ViewGroup.LayoutParams params = child.getLayoutParams();
                if (child.getClass() == View.class && params != null
                        && params.width == ViewGroup.LayoutParams.MATCH_PARENT
                        && params.height == ViewGroup.LayoutParams.MATCH_PARENT) {
                    group.removeViewAt(i);
                }
            }
        }
        for (int i = 0; i < group.getChildCount(); i++) removeGlassDecor(group.getChildAt(i), density);
    }

    private void styleText(View view) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            if (text.getTextSize() > 0 && text.getTextSize() / getResources().getDisplayMetrics().scaledDensity < 12) {
                text.setTextSize(13);
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) styleText(group.getChildAt(i));
        }
    }

    private boolean styleDialogTitle(View view, boolean first) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            if (text.getVisibility() == View.VISIBLE && text.getText() != null && !text.getText().toString().trim().isEmpty()) {
                text.setTypeface(android.graphics.Typeface.create(ResourcesCompat.getFont(requireContext(), R.font.manrope), android.graphics.Typeface.BOLD));
                text.setTextColor(0xFF1C1B1A);
                text.setTextSize(20);
                text.setIncludeFontPadding(false);
                text.setTag("farm_dialog_title");
                return false;
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                first = styleDialogTitle(group.getChildAt(i), first);
                if (!first) break;
            }
        }
        return first;
    }

    private void styleButtons(View view, float density) {
        if (view instanceof Button && !(view.getParent() instanceof com.google.android.material.button.MaterialButtonToggleGroup)) {
            Button button = (Button) view;
            String label = button.getText() == null ? "" : button.getText().toString().trim();
            boolean destructive = label.matches("(?i).*(delete|remove|discard).*" );
            boolean primary = destructive || label.matches("(?i).*(save|add|create|record|confirm|update|assign|submit|intake|generate|link|revise|close commitment).*" );
            styleAction(button, primary, density, destructive);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) styleButtons(group.getChildAt(i), density);
            if (group instanceof android.widget.LinearLayout) {
                java.util.ArrayList<Button> actions = new java.util.ArrayList<>();
                for (int i = 0; i < group.getChildCount(); i++) {
                    if (group.getChildAt(i) instanceof Button) actions.add((Button) group.getChildAt(i));
                }
                if (actions.size() == 2) {
                    android.widget.LinearLayout row = (android.widget.LinearLayout) group;
                    Button first = actions.get(0);
                    Button second = actions.get(1);
                    String firstLabel = first.getText() == null ? "" : first.getText().toString();
                    String secondLabel = second.getText() == null ? "" : second.getText().toString();
                    if (firstLabel.matches("(?i).*cancel.*") || secondLabel.matches("(?i).*cancel.*")) {
                        Button cancel = firstLabel.matches("(?i).*cancel.*") ? first : second;
                        Button primary = cancel == first ? second : first;
                        if (row.indexOfChild(cancel) > row.indexOfChild(primary)) {
                            row.removeView(cancel);
                            row.addView(cancel, 0);
                        }
                        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                        row.setGravity(Gravity.CENTER_VERTICAL);
                        for (int i = 0; i < actions.size(); i++) {
                            Button action = actions.get(i);
                            ViewGroup.LayoutParams raw = action.getLayoutParams();
                            android.widget.LinearLayout.LayoutParams lp = raw instanceof android.widget.LinearLayout.LayoutParams
                                    ? (android.widget.LinearLayout.LayoutParams) raw
                                    : new android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
                            lp.width = 0;
                            lp.weight = 1f;
                            lp.height = Math.round(60 * density);
                            lp.setMargins(i == 0 ? 0 : Math.round(12 * density), 0, 0, 0);
                            action.setLayoutParams(lp);
                        }
                    }
                }
            }
        }
    }

    private void styleInputs(View view, float density) {
        if (view instanceof TextInputLayout) {
            TextInputLayout inputLayout = (TextInputLayout) view;
            inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
            inputLayout.setBoxBackgroundColor(0xFFF6F1E7);
            inputLayout.setBoxCornerRadii(16 * density, 16 * density, 16 * density, 16 * density);
            android.content.res.ColorStateList stroke = new android.content.res.ColorStateList(
                    new int[][]{new int[]{android.R.attr.state_focused}, new int[]{}},
                    new int[]{0xFF0E6B3C, 0xFFE9E1CF});
            inputLayout.setBoxStrokeColorStateList(stroke);
            inputLayout.setBoxStrokeWidth(Math.max(1, Math.round(density)));
            inputLayout.setBoxStrokeWidthFocused(Math.max(2, Math.round(2 * density)));
        }
        if (view instanceof EditText) {
            EditText input = (EditText) view;
            if (!(input.getParent() instanceof TextInputLayout)) {
                GradientDrawable inputBackground = new GradientDrawable();
                inputBackground.setColor(0xFFF6F1E7);
                inputBackground.setCornerRadius(16 * density);
                inputBackground.setStroke(Math.max(1, Math.round(density)), 0xFFE9E1CF);
                input.setBackground(inputBackground);
            }
            input.setMinHeight(Math.round(52 * density));
            input.setPadding(Math.round(16 * density), input.getPaddingTop(), Math.round(48 * density), input.getPaddingBottom());
            input.setTextSize(15);
            input.setTypeface(android.graphics.Typeface.create(ResourcesCompat.getFont(requireContext(), R.font.manrope_medium), android.graphics.Typeface.NORMAL));
            input.setTextColor(0xFF1C1B1A);
            input.setHintTextColor(0xFF8A857B);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) styleInputs(group.getChildAt(i), density);
        }
    }

    private void styleAction(Button button, boolean primary, float density, boolean destructive) {
        if (button == null) return;
        int fill = destructive ? 0xFFF8E4DD : primary ? 0xFF0E6B3C : 0xFFF3EAD8;
        if (button instanceof com.google.android.material.button.MaterialButton) {
            com.google.android.material.button.MaterialButton materialButton =
                    (com.google.android.material.button.MaterialButton) button;
            materialButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(fill));
            materialButton.setCornerRadius(Math.round(999 * density));
            materialButton.setInsetTop(0);
            materialButton.setInsetBottom(0);
            materialButton.setStrokeWidth(0);
            materialButton.setIcon(null);
            materialButton.setStateListAnimator(null);
        } else {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(fill);
            bg.setCornerRadius(999 * density);
            button.setBackground(bg);
        }
        button.setTextColor(destructive ? 0xFFB3402A : primary ? Color.WHITE : 0xFF1C1B1A);
        button.setTextSize(15);
        button.setTypeface(android.graphics.Typeface.create(ResourcesCompat.getFont(requireContext(), R.font.manrope), android.graphics.Typeface.BOLD));
        button.setTextSize(15);
            button.setMinHeight(Math.round(60 * density));
        button.setAllCaps(false);
        button.setSingleLine(false);
        button.setMaxLines(2);
        button.setEllipsize(null);
        button.setGravity(Gravity.CENTER);
        button.setElevation(0);
        button.setTranslationZ(0);
        button.setStateListAnimator(null);
        button.setCompoundDrawables(null, null, null, null);
    }
}
