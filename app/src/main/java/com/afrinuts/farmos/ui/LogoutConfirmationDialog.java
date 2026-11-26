package com.afrinuts.farmos.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.afrinuts.farmos.R;

/** Shared redesign-style confirmation used by the account-menu Logout actions. */
public final class LogoutConfirmationDialog {
    private static final int INK = 0xFF1C1B1A;
    private static final int MUTED = 0xFF59544C;
    private static final int SAND = 0xFFF3EAD8;
    private static final int RED_TINT = 0xFFF8E4DD;
    private static final int RED = 0xFFB3402A;

    private LogoutConfirmationDialog() {}

    public static void show(Context context, Runnable onLogout) {
        float density = context.getResources().getDisplayMetrics().density;
        Dialog dialog = new Dialog(context);

        FrameLayout sheet = new FrameLayout(context);
        GradientDrawable sheetBackground = rounded(Color.WHITE, 28 * density);
        sheet.setBackground(sheetBackground);
        sheet.setClipToOutline(true);
        int pad = dp(density, 24);
        sheet.setPadding(pad, pad, pad, pad);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        sheet.addView(content, contentParams);

        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(context);
        title.setText("Logout");
        title.setTextColor(INK);
        title.setTextSize(20);
        title.setTypeface(font(context, R.font.manrope_extrabold));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1);
        header.addView(title, titleParams);

        ImageButton close = new ImageButton(context);
        close.setImageResource(R.drawable.ic_farm_dialog_close);
        close.setContentDescription("Close dialog");
        close.setScaleType(android.widget.ImageView.ScaleType.CENTER);
        close.setPadding(0, 0, 0, 0);
        close.setBackground(rounded(SAND, 999 * density));
        header.addView(close, new LinearLayout.LayoutParams(dp(density, 40), dp(density, 40)));
        content.addView(header, new LinearLayout.LayoutParams(-1, -2));
        close.setOnClickListener(v -> dialog.dismiss());

        TextView message = new TextView(context);
        message.setText("Are you sure you want to logout?");
        message.setTextColor(MUTED);
        message.setTextSize(15);
        message.setTypeface(font(context, R.font.manrope_medium));
        message.setLineSpacing(dp(density, 3), 1f);
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(-1, -2);
        messageParams.topMargin = dp(density, 18);
        content.addView(message, messageParams);

        LinearLayout actions = new LinearLayout(context);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(-1, dp(density, 52));
        actionsParams.topMargin = dp(density, 26);
        content.addView(actions, actionsParams);

        TextView cancel = actionButton(context, "Cancel", INK, SAND, density);
        TextView logout = actionButton(context, "Logout", RED, RED_TINT, density);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(0, -1, 1);
        actions.addView(cancel, buttonParams);
        LinearLayout.LayoutParams logoutParams = new LinearLayout.LayoutParams(0, -1, 1);
        logoutParams.setMarginStart(dp(density, 12));
        actions.addView(logout, logoutParams);
        cancel.setOnClickListener(v -> dialog.dismiss());
        logout.setOnClickListener(v -> {
            dialog.dismiss();
            if (onLogout != null) onLogout.run();
        });

        dialog.setContentView(sheet);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.dimAmount = .48f;
            params.gravity = Gravity.CENTER;
            window.setAttributes(params);
            int width = context.getResources().getDisplayMetrics().widthPixels - dp(density, 40);
            window.setLayout(Math.max(dp(density, 280), width), WindowManager.LayoutParams.WRAP_CONTENT);
        }
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        if (window != null) {
            int width = context.getResources().getDisplayMetrics().widthPixels - dp(density, 40);
            window.setLayout(Math.max(dp(density, 280), width), WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private static TextView actionButton(Context context, String label, int textColor, int fill, float density) {
        TextView button = new TextView(context);
        button.setText(label);
        button.setTextColor(textColor);
        button.setTextSize(15);
        button.setGravity(Gravity.CENTER);
        button.setTypeface(font(context, R.font.manrope_extrabold));
        button.setBackground(rounded(fill, 999 * density));
        button.setClickable(true);
        button.setFocusable(true);
        return button;
    }

    private static GradientDrawable rounded(int color, float radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private static Typeface font(Context context, int resource) {
        Typeface typeface = ResourcesCompat.getFont(context, resource);
        return typeface == null ? Typeface.DEFAULT : typeface;
    }

    private static int dp(float density, int value) {
        return Math.round(value * density);
    }
}
