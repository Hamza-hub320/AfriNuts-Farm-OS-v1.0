package com.afrinuts.farmos.ui.notifications;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.ui.compliance.DueAlertRules;
import com.afrinuts.farmos.ui.compliance.DueExpiringAdapter;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Shared bell navigation and unread count badge for the five hubs. */
public final class NotificationBell {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final String PREFS = "notifications_inbox_v1";

    private NotificationBell() { }

    public static TextView wire(Activity activity, ImageButton bell) {
        if (bell == null) return null;
        bell.setClickable(true);
        bell.setFocusable(true);
        bell.setOnClickListener(v -> activity.startActivity(new android.content.Intent(activity, NotificationsActivity.class)));
        ViewGroup parent = (ViewGroup) bell.getParent();
        if (parent instanceof FrameLayout && parent.getChildCount() > 1) {
            View badge = parent.getChildAt(parent.getChildCount() - 1);
            if (badge instanceof TextView) return (TextView) badge;
        }
        if (parent == null) return null;
        int index = parent.indexOfChild(bell);
        ViewGroup.LayoutParams original = bell.getLayoutParams();
        parent.removeView(bell);
        FrameLayout wrapper = new FrameLayout(activity);
        wrapper.setClipChildren(false);
        wrapper.setClipToPadding(false);
        wrapper.setLayoutParams(original);
        FrameLayout.LayoutParams buttonParams = new FrameLayout.LayoutParams(-1, -1);
        bell.setLayoutParams(buttonParams);
        wrapper.addView(bell);
        TextView badge = new TextView(activity);
        badge.setGravity(Gravity.CENTER);
        badge.setTextColor(Color.WHITE);
        badge.setTextSize(9);
        badge.setTypeface(activity.getResources().getFont(R.font.manrope_extrabold));
        badge.setMinWidth(dp(activity, 16));
        badge.setMinimumHeight(dp(activity, 16));
        badge.setPadding(dp(activity, 3), 0, dp(activity, 3), 0);
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(0xFFB3402A);
        badgeBg.setCornerRadius(dp(activity, 999));
        badge.setBackground(badgeBg);
        badge.setVisibility(View.GONE);
        FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(-2, dp(activity, 16), Gravity.TOP | Gravity.END);
        badgeParams.topMargin = dp(activity, -2);
        badgeParams.rightMargin = dp(activity, -2);
        wrapper.addView(badge, badgeParams);
        parent.addView(wrapper, index);
        parent.setClipChildren(false);
        parent.setClipToPadding(false);
        return badge;
    }

    public static void refresh(Activity activity, TextView badge) {
        if (badge == null || activity.isFinishing()) return;
        IO.execute(() -> {
            int count = unreadCount(activity);
            activity.runOnUiThread(() -> {
                if (activity.isFinishing() || badge.getWindowToken() == null) return;
                badge.setText(count > 99 ? "99+" : String.valueOf(count));
                badge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
            });
        });
    }

    private static int unreadCount(Activity activity) {
        com.afrinuts.farmos.utils.SessionManager session = new com.afrinuts.farmos.utils.SessionManager(activity);
        com.afrinuts.farmos.data.local.entity.UserEntity.UserRole role = session.getUserRole();
        if (role != com.afrinuts.farmos.data.local.entity.UserEntity.UserRole.OWNER
                && role != com.afrinuts.farmos.data.local.entity.UserEntity.UserRole.CEO
                && role != com.afrinuts.farmos.data.local.entity.UserEntity.UserRole.MANAGER) return 0;
        try {
            AppDatabase db = AppDatabase.getInstance(activity.getApplicationContext());
            com.afrinuts.farmos.data.local.entity.FarmEntity farm = db.farmDao().getFirstFarm();
            if (farm == null) return 0;
            android.content.SharedPreferences prefs = activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE);
            int count = 0;
            Set<String> activeKeys = new HashSet<>();
            for (DueExpiringAdapter.Row row : DueAlertRules.buildRows(
                    db.certificationDao().getCertificationsByFarmId(farm.getId()),
                    db.documentDao().getDocumentsByFarmId(farm.getId()),
                    db.auditDao().getAuditsByFarmId(farm.getId()),
                    db.inspectionDao().getInspectionsByFarmId(farm.getId()), System.currentTimeMillis())) {
                if (row.alertKey != null) activeKeys.add(row.alertKey);
            }
            List<SyncEntity> queue = db.syncDao().getPendingSyncItems();
            if (queue != null) for (SyncEntity item : queue) {
                if (item.getStatus() != SyncEntity.SyncStatus.FAILED) continue;
                activeKeys.add(syncKey(item));
            }
            for (String key : activeKeys) if (!prefs.getBoolean(readKey(key), false)) count++;
            android.content.SharedPreferences.Editor cleanup = prefs.edit();
            boolean changed = false;
            for (String stored : prefs.getAll().keySet()) if (stored.startsWith("read_") && !activeKeys.contains(stored.substring(5))) {
                cleanup.remove(stored); changed = true;
            }
            if (changed) cleanup.commit();
            return count;
        } catch (Exception ignored) { return 0; }
    }

    public static String syncKey(SyncEntity item) {
        String id = item.getEntityStringId() == null || item.getEntityStringId().trim().isEmpty()
                ? String.valueOf(item.getEntityId()) : item.getEntityStringId();
        return "sync_" + item.getEntityType() + "_" + id;
    }

    public static String readKey(String alertKey) { return "read_" + alertKey; }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

}
