package com.afrinuts.farmos.ui.notifications;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.AuditEntity;
import com.afrinuts.farmos.data.local.entity.CertificationEntity;
import com.afrinuts.farmos.data.local.entity.DocumentEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.InspectionEntity;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.ui.BaseActivity;
import com.afrinuts.farmos.ui.compliance.AuditDetailActivity;
import com.afrinuts.farmos.ui.compliance.CertificationDetailActivity;
import com.afrinuts.farmos.ui.compliance.ComplianceHubActivity;
import com.afrinuts.farmos.ui.compliance.DocumentDetailActivity;
import com.afrinuts.farmos.ui.compliance.DueAlertRules;
import com.afrinuts.farmos.ui.compliance.DueExpiringAdapter;
import com.afrinuts.farmos.ui.compliance.InspectionDetailActivity;
import com.afrinuts.farmos.ui.sync.SyncCenterActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotificationsActivity extends BaseActivity {
    private static final int INK = 0xFF1C1B1A;
    private static final int MUTED = 0xFF59544C;
    private static final int GREEN = 0xFF0E6B3C;
    private static final int RED = 0xFFB3402A;
    private static final String PREFS = "notifications_inbox_v1";
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final List<Alert> visibleAlerts = new ArrayList<>();
    private LinearLayout dueRows;
    private LinearLayout syncRows;
    private TextView emptyView;
    private TextView markAllRead;
    private List<DueExpiringAdapter.Row> dueItems = new ArrayList<>();
    private List<Alert> syncAlerts = new ArrayList<>();
    private AppDatabase database;
    private boolean fieldOfficer;

    @Override protected boolean checkRoleAccess() { return true; }

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!sessionManager.isLoggedIn()) return;
        setContentView(R.layout.activity_notifications);
        database = AppDatabase.getInstance(getApplicationContext());
        dueRows = findViewById(R.id.dueRows);
        syncRows = findViewById(R.id.syncRows);
        emptyView = findViewById(R.id.emptyNotifications);
        markAllRead = findViewById(R.id.markAllRead);
        ((ImageButton) findViewById(R.id.toolbarBack)).setOnClickListener(v -> finish());
        markAllRead.setOnClickListener(v -> markVisibleRead());
        fieldOfficer = sessionManager.isFieldOfficer();
        if (fieldOfficer) { render(); return; }
        io.execute(() -> {
            FarmEntity farm = database.farmDao().getFirstFarm();
            if (farm == null) { runOnUiThread(this::render); return; }
            if (isFinishing()) return;
            runOnUiThread(() -> observeFarm(farm.getId()));
        });
    }

    private void observeFarm(long farmId) {
        LiveData<List<CertificationEntity>> certs = database.certificationDao().getCertificationsByFarmIdLiveData(farmId);
        LiveData<List<DocumentEntity>> docs = database.documentDao().getDocumentsByFarmIdLiveData(farmId);
        LiveData<List<AuditEntity>> audits = database.auditDao().getAuditsByFarmIdLiveData(farmId);
        LiveData<List<InspectionEntity>> inspections = database.inspectionDao().getInspectionsByFarmIdLiveData(farmId);
        certs.observe(this, rows -> { latestCertifications = rows == null ? new ArrayList<>() : rows; rebuildDue(); });
        docs.observe(this, rows -> { latestDocs = rows; rebuildDue(); });
        audits.observe(this, rows -> { latestAudits = rows; rebuildDue(); });
        inspections.observe(this, rows -> { latestInspections = rows; rebuildDue(); });
        database.syncDao().getPendingSyncItemsLiveData().observe(this, rows -> {
            List<SyncEntity> failed = new ArrayList<>();
            if (rows != null) for (SyncEntity row : rows) if (row.getStatus() == SyncEntity.SyncStatus.FAILED) failed.add(row);
            io.execute(() -> {
                List<Alert> latest = new ArrayList<>();
                Set<String> keys = new HashSet<>();
                for (SyncEntity row : failed) {
                    Alert alert = Alert.forSync(row, SyncCenterActivity.resolveLabel(database, row));
                    if (keys.add(alert.key)) latest.add(alert);
                }
                runOnUiThread(() -> { if (!isFinishing()) { syncAlerts = latest; render(); } });
            });
        });
    }

    private List<CertificationEntity> latestCertifications = new ArrayList<>();
    private List<DocumentEntity> latestDocs = new ArrayList<>();
    private List<AuditEntity> latestAudits = new ArrayList<>();
    private List<InspectionEntity> latestInspections = new ArrayList<>();

    private void rebuildDue() {
        dueItems = DueAlertRules.buildRows(latestCertifications, latestDocs, latestAudits, latestInspections, System.currentTimeMillis());
        render();
    }

    private void render() {
        if (dueRows == null) return;
        dueRows.removeAllViews();
        syncRows.removeAllViews();
        visibleAlerts.clear();
        Set<String> renderedKeys = new HashSet<>();
        if (!fieldOfficer) {
            for (DueExpiringAdapter.Row row : dueItems) {
                if (row.alertKey == null || !renderedKeys.add(row.alertKey)) continue;
                Alert alert = Alert.forDue(row);
                visibleAlerts.add(alert);
                dueRows.addView(makeAlertCard(alert));
            }
            for (Alert alert : syncAlerts) {
                if (!renderedKeys.add(alert.key)) continue;
                visibleAlerts.add(alert);
                syncRows.addView(makeAlertCard(alert));
            }
        }
        emptyView.setVisibility(visibleAlerts.isEmpty() ? View.VISIBLE : View.GONE);
        markAllRead.setVisibility(visibleAlerts.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private View makeAlertCard(Alert alert) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE); bg.setCornerRadius(dp(20)); bg.setStroke(dp(1), 0xFFE9E1CF);
        card.setBackground(bg);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.bottomMargin = dp(8); card.setLayoutParams(cp);
        boolean read = getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(NotificationBell.readKey(alert.key), false);
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text(alert.title, 14.5f, INK, true);
        title.setMaxLines(2); title.setEllipsize(TextUtils.TruncateAt.END);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        TextView state = text(read ? "Read" : "New", 11.5f, read ? MUTED : GREEN, true);
        state.setPadding(dp(9), dp(4), dp(9), dp(4));
        state.setBackground(shape(read ? 0xFFF3EAD8 : 0xFFE3F0E7));
        header.addView(state);
        card.addView(header);
        TextView body = text(alert.body, 12.5f, MUTED, false);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2); bp.topMargin = dp(5);
        card.addView(body, bp);
        card.setOnClickListener(v -> openAlert(alert));
        return card;
    }

    private void openAlert(Alert alert) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(NotificationBell.readKey(alert.key), true).commit();
        Intent intent;
        if (alert.sync == null && getResources().getConfiguration().screenWidthDp >= 600) {
            intent = new Intent(this, ComplianceHubActivity.class);
            intent.putExtra(ComplianceHubActivity.EXTRA_OPEN_DUE_TYPE, alert.due.recordType);
            intent.putExtra(ComplianceHubActivity.EXTRA_OPEN_DUE_ID, alert.due.recordId);
        } else if (alert.sync != null) {
            intent = new Intent(this, SyncCenterActivity.class);
            intent.putExtra(SyncCenterActivity.EXTRA_SYNC_QUEUE_ID, alert.sync.getId());
        } else {
            switch (alert.due.recordType) {
                case "document": intent = new Intent(this, DocumentDetailActivity.class); intent.putExtra(DocumentDetailActivity.EXTRA_DOCUMENT_ID, alert.due.recordId); break;
                case "audit": intent = new Intent(this, AuditDetailActivity.class); intent.putExtra(AuditDetailActivity.EXTRA_AUDIT_ID, alert.due.recordId); break;
                case "inspection": intent = new Intent(this, InspectionDetailActivity.class); intent.putExtra(InspectionDetailActivity.EXTRA_INSPECTION_ID, alert.due.recordId); break;
                default: intent = new Intent(this, CertificationDetailActivity.class); intent.putExtra(CertificationDetailActivity.EXTRA_CERTIFICATION_ID, alert.due.recordId); break;
            }
        }
        startActivity(intent);
    }

    private void markVisibleRead() {
        android.content.SharedPreferences.Editor edit = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
        for (Alert alert : visibleAlerts) edit.putBoolean(NotificationBell.readKey(alert.key), true);
        edit.commit();
        render();
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color);
        view.setTypeface(getResources().getFont(bold ? R.font.manrope_extrabold : R.font.manrope_semibold));
        return view;
    }
    private GradientDrawable shape(int color) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(999)); return d; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override protected void onDestroy() { io.shutdown(); super.onDestroy(); }

    private static final class Alert {
        final String key, title, body; final DueExpiringAdapter.Row due; final SyncEntity sync;
        private Alert(String key, String title, String body, DueExpiringAdapter.Row due, SyncEntity sync) {
            this.key = key; this.title = title; this.body = body; this.due = due; this.sync = sync;
        }
        static Alert forDue(DueExpiringAdapter.Row row) {
            return new Alert(row.alertKey, row.title, row.subtitle + " · " + row.dateText + " · " + row.daysText, row, null);
        }
        static Alert forSync(SyncEntity item, String resolvedLabel) {
            String key = NotificationBell.syncKey(item);
            String title = resolvedLabel;
            String reason = TextUtils.isEmpty(item.getErrorMessage()) ? "Upload failed" : item.getErrorMessage();
            String attention = item.getAttemptCount() >= 7 ? "Needs attention" : "Failed";
            return new Alert(key, title, attention + " · Attempt " + item.getAttemptCount() + " · " + reason, null, item);
        }
    }
}
