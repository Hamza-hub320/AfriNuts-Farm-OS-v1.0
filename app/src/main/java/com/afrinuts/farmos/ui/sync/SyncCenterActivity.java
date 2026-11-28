package com.afrinuts.farmos.ui.sync;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import com.google.android.material.card.MaterialCardView;
import androidx.lifecycle.Observer;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.data.sync.SyncManager;
import com.afrinuts.farmos.ui.BaseActivity;
import com.afrinuts.farmos.utils.NetworkUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SyncCenterActivity extends BaseActivity {
    public static final String EXTRA_SYNC_QUEUE_ID = "sync_center_queue_id";
    private static final int INK = 0xFF1C1B1A;
    private static final int MUTED = 0xFF59544C;
    private static final int GREEN = 0xFF0E6B3C;
    private static final int DEEP_GREEN = 0xFF0A5530;
    private static final int RED = 0xFFB3402A;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private AppDatabase database;
    private SyncManager syncManager;
    private SyncManager.SyncListener centerListener;
    private LinearLayout pendingRows;
    private LinearLayout recentRows;
    private TextView pendingCount;
    private TextView failedCount;
    private TextView connectionValue;
    private View connectionDot;
    private TextView lastSyncValue;
    private long farmId;
    private long filterQueueId;

    @Override protected boolean checkRoleAccess() {
        com.afrinuts.farmos.data.local.entity.UserEntity.UserRole role = sessionManager.getUserRole();
        return role == com.afrinuts.farmos.data.local.entity.UserEntity.UserRole.OWNER
                || role == com.afrinuts.farmos.data.local.entity.UserEntity.UserRole.CEO
                || role == com.afrinuts.farmos.data.local.entity.UserEntity.UserRole.MANAGER;
    }

    @Override protected String getAccessDeniedMessage() { return "Sync Center is available to Owners, CEOs and Managers"; }

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!sessionManager.isLoggedIn() || !checkRoleAccess()) return;
        setContentView(R.layout.activity_sync_center);
        filterQueueId = getIntent().getLongExtra(EXTRA_SYNC_QUEUE_ID, 0L);
        database = AppDatabase.getInstance(this);
        pendingRows = findViewById(R.id.pendingRows);
        recentRows = findViewById(R.id.recentRows);
        pendingCount = findViewById(R.id.pendingCount);
        failedCount = findViewById(R.id.failedCount);
        connectionValue = findViewById(R.id.connectionValue);
        connectionDot = findViewById(R.id.connectionDot);
        lastSyncValue = findViewById(R.id.lastSyncValue);
        ((ImageButton) findViewById(R.id.toolbarBack)).setOnClickListener(v -> finish());
        findViewById(R.id.btnSyncNow).setOnClickListener(v -> startSync());
        new NetworkUtils(this).observe(this, connected -> setConnection(Boolean.TRUE.equals(connected)));
        database.syncDao().getPendingSyncItemsLiveData().observe(this, items -> renderPending(items));
        database.syncDao().getPendingCountLiveData().observe(this, count -> updateCounts(count == null ? 0 : count));
        database.syncDao().getRecentCompletedLiveData().observe(this, this::renderRecent);
        io.execute(() -> {
            FarmEntity farm = database.farmDao().getFirstFarm();
            if (farm == null) return;
            farmId = farm.getId();
            runOnUiThread(() -> {
                syncManager = SyncManager.getInstance(getApplicationContext());
                centerListener = new SyncManager.SyncListener() {
                    @Override public void onSyncStarted() { setSyncButtonState(true); }
                    @Override public void onSyncCompleted(int count) {
                        setSyncButtonState(false);
                        long stamp = getSharedPreferences("sync_center", MODE_PRIVATE)
                                .getLong("last_sync_success_" + farmId, 0L);
                        lastSyncValue.setText(stamp > 0 ? new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(new Date(stamp)) : "—");
                        Toast.makeText(SyncCenterActivity.this, count + " record" + (count == 1 ? "" : "s") + " synced", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onSyncFailed(String error) {
                        setSyncButtonState(false);
                        Toast.makeText(SyncCenterActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                    @Override public void onOfflineMode() { setConnection(false); }
                    @Override public void onOnlineMode() { setConnection(true); }
                };
                syncManager.setSyncCenterContext(farmId, centerListener);
                long stamp = getSharedPreferences("sync_center", MODE_PRIVATE)
                        .getLong("last_sync_success_" + farmId, 0L);
                lastSyncValue.setText(stamp > 0 ? new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(new Date(stamp)) : "—");
            });
        });
    }

    private void startSync() {
        if (syncManager == null || farmId <= 0) {
            Toast.makeText(this, "Sync is not ready yet", Toast.LENGTH_SHORT).show();
            return;
        }
        io.execute(() -> {
            int count = database.syncDao().getPendingCount();
            runOnUiThread(() -> {
                if (count == 0) {
                    Toast.makeText(this, "Nothing to sync", Toast.LENGTH_SHORT).show();
                } else if (!NetworkUtils.isConnected(this)) {
                    Toast.makeText(this, "You are offline. The queue will sync when connection returns.", Toast.LENGTH_LONG).show();
                } else {
                    syncManager.forceSyncNow();
                }
            });
        });
    }

    private void setConnection(boolean online) {
        connectionValue.setText(online ? "Online" : "Offline");
        connectionDot.setBackground(makeShape(online ? 0xFF14794A : 0xFF8A857C, 999, Color.TRANSPARENT));
    }

    private void setSyncButtonState(boolean syncing) {
        View button = findViewById(R.id.btnSyncNow);
        button.setEnabled(!syncing);
        if (button instanceof com.google.android.material.button.MaterialButton) {
            ((com.google.android.material.button.MaterialButton) button).setText(syncing ? "Syncing…" : "Sync now");
        }
    }

    private void updateCounts(int combined) {
        List<SyncEntity> current = database.syncDao().getPendingSyncItemsLiveData().getValue();
        int failed = 0;
        if (current != null) for (SyncEntity item : current) if (item.getStatus() == SyncEntity.SyncStatus.FAILED) failed++;
        failedCount.setText(String.valueOf(failed));
        pendingCount.setText(String.valueOf(Math.max(0, combined - failed)));
    }

    private void renderPending(List<SyncEntity> items) {
        if (items == null) return;
        List<SyncEntity> visible = new ArrayList<>(items);
        if (filterQueueId > 0) {
            visible.removeIf(item -> item.getId() != filterQueueId);
        }
        int failed = 0;
        for (SyncEntity item : visible) if (item.getStatus() == SyncEntity.SyncStatus.FAILED) failed++;
        final int failedItems = failed;
        failedCount.setText(String.valueOf(failed));
        io.execute(() -> {
            List<String> labels = new ArrayList<>();
            for (SyncEntity item : visible) labels.add(resolveLabel(item));
            runOnUiThread(() -> {
                pendingRows.removeAllViews();
                for (int i = 0; i < visible.size(); i++) pendingRows.addView(makeQueueCard(visible.get(i), labels.get(i)));
                if (visible.isEmpty()) pendingRows.addView(emptyText("No pending uploads"));
                io.execute(() -> {
                    int combined = database.syncDao().getPendingCount();
                    runOnUiThread(() -> pendingCount.setText(String.valueOf(Math.max(0, combined - failedItems))));
                });
            });
        });
    }

    private View makeQueueCard(SyncEntity item, String label) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(24)); card.setCardElevation(0); card.setCardBackgroundColor(Color.WHITE);
        card.setUseCompatPadding(false);
        boolean filteredTarget = filterQueueId > 0 && item.getId() == filterQueueId;
        card.setStrokeColor(filteredTarget ? GREEN : 0xB3E9E1CF); card.setStrokeWidth(dp(filteredTarget ? 2 : 1));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2); cp.bottomMargin = dp(8); card.setLayoutParams(cp);
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout textCol = new LinearLayout(this); textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1); row.addView(textCol, tp);
        TextView title = text(label, 15, INK, true); textCol.addView(title);
        String type = displayType(item.getEntityType());
        TextView meta = text(type + " · Attempt " + Math.max(1, item.getAttemptCount()), 12.5f, MUTED, false);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(-2, -2); mp.topMargin = dp(2); textCol.addView(meta, mp);
        if (item.getStatus() == SyncEntity.SyncStatus.FAILED && item.getAttemptCount() > 6) {
            LinearLayout attentionRow = new LinearLayout(this); attentionRow.setGravity(Gravity.CENTER_VERTICAL);
            android.widget.ImageView warning = new android.widget.ImageView(this); warning.setImageResource(R.drawable.ic_warning); warning.setColorFilter(RED);
            attentionRow.addView(warning, new LinearLayout.LayoutParams(dp(15), dp(15)));
            TextView attention = text("Needs attention", 12.5f, RED, true); LinearLayout.LayoutParams attentionTextParams = new LinearLayout.LayoutParams(-2, -2); attentionTextParams.leftMargin = dp(5); attentionRow.addView(attention, attentionTextParams);
            LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-2, -2); ap.topMargin = dp(4); textCol.addView(attentionRow, ap);
        }
        if (item.getStatus() == SyncEntity.SyncStatus.FAILED && !TextUtils.isEmpty(item.getErrorMessage())) {
            TextView reason = text(item.getErrorMessage(), 11.5f, MUTED, false);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2); rp.topMargin = dp(3); textCol.addView(reason, rp);
        }
        TextView pill = text(item.getStatus() == SyncEntity.SyncStatus.FAILED ? "Failed" : "Waiting", 12, item.getStatus() == SyncEntity.SyncStatus.FAILED ? RED : 0xFF7A5F22, true);
        pill.setPadding(dp(12), dp(7), dp(12), dp(7));
        pill.setBackground(makeShape(item.getStatus() == SyncEntity.SyncStatus.FAILED ? 0xFFF8E4DD : 0xFFF3EAD8, 999, Color.TRANSPARENT));
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-2, -2); pp.leftMargin = dp(8); row.addView(pill, pp);
        card.addView(row);
        return card;
    }

    private void renderRecent(List<SyncEntity> items) {
        if (items == null) return;
        io.execute(() -> {
            List<String> labels = new ArrayList<>(); for (SyncEntity item : items) labels.add(resolveLabel(item));
            runOnUiThread(() -> {
                recentRows.removeAllViews();
                if (items.isEmpty()) { recentRows.addView(emptyText("No completed uploads yet")); return; }
                for (int i = 0; i < items.size(); i++) {
                    SyncEntity item = items.get(i);
                    LinearLayout line = new LinearLayout(this); line.setGravity(Gravity.CENTER_VERTICAL); line.setPadding(dp(4), dp(14), dp(4), dp(14));
                    TextView label = text(labels.get(i) + " uploaded", 14, INK, true); label.setMaxLines(1); label.setEllipsize(TextUtils.TruncateAt.END);
                    line.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
                    TextView time = text(item.getLastAttemptAt() > 0 ? new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(item.getLastAttemptAt())) : "—", 13, MUTED, true);
                    line.addView(time);
                    recentRows.addView(line, new LinearLayout.LayoutParams(-1, -2));
                    if (i < items.size() - 1) { View divider = new View(this); divider.setBackgroundColor(0xFFE9E1CF); recentRows.addView(divider, new LinearLayout.LayoutParams(-1, dp(1))); }
                }
            });
        });
    }

    private String resolveLabel(SyncEntity item) {
        return resolveLabel(database, item);
    }

    public static String resolveLabel(AppDatabase database, SyncEntity item) {
        String raw = item.getEntityType() == null ? "record" : item.getEntityType().trim().toLowerCase(Locale.ROOT);
        long id = item.getEntityId();
        try {
            if (raw.equals("block") || raw.equals("blocks")) { BlockEntity value = database.blockDao().getBlockById(id); if (value != null) return "Block " + value.getBlockName(); }
            if (raw.equals("expense") || raw.equals("expenses")) { ExpenseEntity value = database.expenseDao().getExpenseById(id); if (value != null) return value.getCategory().getDisplayName() + " expense"; }
            if (raw.equals("revenue") || raw.equals("revenues")) { RevenueEntity value = database.revenueDao().getRevenueById(id); if (value != null && !TextUtils.isEmpty(value.getInvoiceNumber())) return "Revenue " + value.getInvoiceNumber(); }
            if (raw.equals("task") || raw.equals("tasks")) { TaskEntity value = database.taskDao().getTaskById(id); if (value != null) return value.getTitle(); }
            if (raw.equals("quality_batch")) { QualityBatchEntity value = database.qualityBatchDao().getBatchById(id); if (value != null) return "Batch " + value.getBatchNumber(); }
            if (raw.equals("suppliers") || raw.equals("supplier")) {
                com.afrinuts.farmos.data.local.entity.SupplierEntity value = database.supplierDao().getSupplierById(id);
                if (value != null) return "Supplier " + value.getName();
            }
        } catch (Exception ignored) { }
        String identifier = item.getEntityStringId() != null ? item.getEntityStringId() : String.valueOf(id);
        String type = raw.replace('_', ' ');
        String capitalized = type.isEmpty() ? "Record" : type.substring(0, 1).toUpperCase(Locale.ROOT) + type.substring(1);
        return capitalized + " " + identifier;
    }

    private String displayType(String raw) {
        String value = raw == null ? "Record" : raw.trim().toLowerCase(Locale.ROOT);
        if (value.startsWith("block")) return "Block";
        if (value.contains("batch")) return "Batch";
        if (value.contains("revenue")) return "Revenue";
        if (value.contains("expense")) return "Expense";
        if (value.contains("task")) return "Task";
        if (value.contains("photo")) return "Photo";
        if (value.contains("supplier")) return "Supplier";
        return capitalize(value.replace('_', ' '));
    }

    private String capitalize(String value) { return value.isEmpty() ? "Record" : value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1); }
    private TextView emptyText(String value) { TextView t = text(value, 13, MUTED, false); t.setPadding(dp(18), dp(18), dp(18), dp(18)); return t; }
    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color);
        view.setFontFeatureSettings("kern");
        view.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(this, bold ? R.font.manrope_extrabold : R.font.manrope_semibold));
        return view;
    }
    private GradientDrawable makeShape(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); if (stroke != Color.TRANSPARENT) d.setStroke(dp(1), stroke); return d;
    }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override protected void onDestroy() {
        if (syncManager != null && centerListener != null) syncManager.clearSyncCenterListener(centerListener);
        io.shutdown();
        super.onDestroy();
    }
}
