package com.afrinuts.farmos.ui.integrity;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.integrity.IntegrityScanner;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.IntegrityFindingEntity;
import com.afrinuts.farmos.data.local.entity.IntegrityScanRunEntity;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.ui.BaseActivity;
import com.afrinuts.farmos.utils.AccessMap;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Phone list for the internal Data Integrity Center. */
public class IntegrityCenterActivity extends BaseActivity {
    private static final int CREAM = 0xFFF6F1E7, WHITE = Color.WHITE, INK = 0xFF1C1B1A;
    private static final int MUTED = 0xFF59544C, GREEN = 0xFF0E6B3C, PALE_GREEN = 0xFFE3F0E7;
    private static final int RED = 0xFFB3402A, PALE_RED = 0xFFF8E4DF, GOLD = 0xFF916313, PALE_GOLD = 0xFFF5ECD3;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private AppDatabase db;
    private LinearLayout content, findings, history;
    private View runButton;
    private TextView lastScan, runLabel, criticalCount, warningCount, resolvedCount;
    private long farmId;
    private boolean fieldOfficer;

    @Override protected boolean checkRoleAccess() {
        return AccessMap.canOpenScreen("IntegrityCenterActivity", sessionManager.getUserRole());
    }
    @Override protected String getAccessDeniedMessage() { return "Integrity Center is not available for this account"; }

    @Override protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        if (!sessionManager.isLoggedIn() || !checkRoleAccess()) return;
        fieldOfficer = sessionManager.getUserRole() == UserEntity.UserRole.FIELD_OFFICER;
        db = AppDatabase.getInstance(this);
        setContentView(buildScreen());
        io.execute(() -> {
            FarmEntity farm = db.farmDao().getFirstFarm();
            if (farm == null) return;
            farmId = farm.getId();
            runOnUiThread(this::observeData);
        });
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(CREAM);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(22), dp(22), dp(22), dp(32));
        scroll.addView(content);

        android.widget.FrameLayout header = new android.widget.FrameLayout(this);
        header.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(54)));
        ImageButton back = new ImageButton(this);
        back.setImageResource(R.drawable.ic_finance_back_chevron);
        back.setScaleType(android.widget.ImageView.ScaleType.CENTER);
        back.setBackground(shape(WHITE, 999, 0xFFE9E1CF));
        android.widget.FrameLayout.LayoutParams bp = new android.widget.FrameLayout.LayoutParams(dp(44), dp(44), Gravity.START | Gravity.CENTER_VERTICAL);
        header.addView(back, bp);
        back.setOnClickListener(v -> finish());
        TextView title = text("Integrity Center", 17, INK, true);
        title.setGravity(Gravity.CENTER);
        android.widget.FrameLayout.LayoutParams tp = new android.widget.FrameLayout.LayoutParams(-1, -1);
        header.addView(title, tp);
        content.addView(header);

        LinearLayout run = new LinearLayout(this);
        run.setOrientation(LinearLayout.HORIZONTAL); run.setGravity(Gravity.CENTER);
        run.setPadding(dp(20), 0, dp(20), 0); run.setBackground(shape(GREEN, 999, Color.TRANSPARENT));
        android.widget.ImageView refresh = new android.widget.ImageView(this);
        refresh.setImageResource(R.drawable.ic_refresh_due); refresh.setColorFilter(WHITE);
        LinearLayout.LayoutParams iconp = new LinearLayout.LayoutParams(dp(26), dp(26)); iconp.rightMargin = dp(10); run.addView(refresh, iconp);
        runLabel = text("Run integrity scan", 18, WHITE, true); runLabel.setGravity(Gravity.CENTER);
        run.addView(runLabel); runButton = run;
        runButton.setContentDescription("Run integrity scan");
        runButton.setEnabled(!fieldOfficer);
        runButton.setAlpha(fieldOfficer ? .55f : 1f);
        runButton.setOnClickListener(v -> runScan());
        LinearLayout.LayoutParams runp = new LinearLayout.LayoutParams(-1, dp(64));
        runp.topMargin = dp(18); content.addView(runButton, runp);

        lastScan = text("No scan yet", 16, MUTED, false);
        lastScan.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(16); content.addView(lastScan, lp);

        LinearLayout counts = new LinearLayout(this); counts.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, dp(102)); cp.topMargin = dp(18); content.addView(counts, cp);
        criticalCount = countCard(counts, "Critical", RED);
        warningCount = countCard(counts, "Warning", GOLD);
        resolvedCount = countCard(counts, "Resolved", INK);

        section(content, "FINDINGS", dp(24));
        findings = new LinearLayout(this); findings.setOrientation(LinearLayout.VERTICAL); content.addView(findings);
        section(content, "RESOLUTION HISTORY", dp(26));
        history = new LinearLayout(this); history.setOrientation(LinearLayout.VERTICAL); content.addView(history);
        return scroll;
    }

    private TextView countCard(LinearLayout row, String label, int valueColor) {
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(12), 0, dp(12), 0); card.setBackground(shape(WHITE, 24, 0xFFEFE6D7));
        TextView name = text(label, 14, MUTED, true), value = text("0", 30, valueColor, true);
        card.addView(name); card.addView(value);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -1, 1f); p.setMargins(dp(4), 0, dp(4), 0); row.addView(card, p);
        card.setTag(value); return value;
    }

    private void observeData() {
        db.integrityFindingDao().listByFarm(farmId).observe(this, this::renderFindings);
        db.integrityFindingDao().countOpenCritical(farmId).observe(this, n -> criticalCount.setText(String.valueOf(n == null ? 0 : n)));
        db.integrityFindingDao().countOpenWarning(farmId).observe(this, n -> warningCount.setText(String.valueOf(n == null ? 0 : n)));
        db.integrityFindingDao().countResolved(farmId).observe(this, n -> resolvedCount.setText(String.valueOf(n == null ? 0 : n)));
        db.integrityScanRunDao().latestLive(farmId).observe(this, this::renderLastScan);
    }

    private void renderLastScan(IntegrityScanRunEntity run) {
        if (run == null) { lastScan.setText("No scan yet"); return; }
        String date = new SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(new Date(run.getCompletedAt()));
        lastScan.setText("Last scan · " + date + " · " + run.getRecordsScanned() + " records · "
                + run.getRulesEvaluated() + " rules · " + run.getIssuesDetected() + " findings · "
                + run.getPendingSyncCount() + " pending sync");
    }

    private void renderFindings(List<IntegrityFindingEntity> rows) {
        if (rows == null) return;
        findings.removeAllViews(); history.removeAllViews();
        List<IntegrityFindingEntity> visible = new ArrayList<>();
        for (IntegrityFindingEntity f : rows) {
            if (fieldOfficer && !isFieldOfficerFinding(f)) continue;
            visible.add(f);
            findings.addView(findingCard(f));
        }
        if (visible.isEmpty()) findings.addView(empty("No findings yet. Run an integrity scan to check the records."));
        List<IntegrityFindingEntity> resolved = new ArrayList<>();
        for (IntegrityFindingEntity f : visible) if ("RESOLVED".equals(f.getStatus()) || "REJECTED".equals(f.getStatus())) resolved.add(f);
        if (resolved.isEmpty()) history.addView(empty("No resolved findings yet."));
        else {
            LinearLayout card = card();
            int resolvedCountOnly = 0;
            for (IntegrityFindingEntity f : resolved) if ("RESOLVED".equals(f.getStatus())) resolvedCountOnly++;
            LinearLayout top = horizontal(); top.addView(text("Resolution history", 20, INK, true), new LinearLayout.LayoutParams(0, -2, 1f));
            top.addView(text(resolvedCountOnly + " resolved", 15, MUTED, true)); card.addView(top);
            int shown = 0;
            for (IntegrityFindingEntity f : resolved) {
                if (shown++ == 3) break;
                TextView meta = text(f.getRuleCode() + " · " + f.getStatus() + " · " + date(f.getResolvedAt() == null ? f.getDetectedAt() : f.getResolvedAt())
                        + " · " + safe(f.getResolvedBy()), 15, MUTED, true);
                addDivider(card); card.addView(meta);
                card.addView(text("“" + safe(f.getResolutionNotes()) + "”", 16, INK, false));
            }
            if (resolved.size() > 3) card.addView(text("Showing the 3 latest of " + resolved.size() + " resolved findings.", 15, MUTED, false));
            history.addView(card);
        }
    }

    private View findingCard(IntegrityFindingEntity f) {
        LinearLayout card = card();
        LinearLayout pills = horizontal();
        boolean critical = "Critical".equalsIgnoreCase(f.getSeverity());
        pills.addView(pill(critical ? "CRITICAL" : "WARNING", critical ? PALE_RED : PALE_GOLD, critical ? RED : GOLD, 13, 800));
        View status = pill(f.getStatus().replace('_', ' '), statusBg(f.getStatus()), statusColor(f.getStatus()), 13, 800);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-2, dp(30)); sp.leftMargin = dp(8); pills.addView(status, sp);
        card.addView(pills);
        card.addView(text(f.getRuleCode() + " — " + shortDescription(f.getDescription()), 16, INK, true));
        card.addView(text(safe(f.getDescription()), 16, MUTED, false));
        card.addView(text("Expected " + safe(f.getExpectedValue()) + " · Actual " + safe(f.getActualValue()), 15, MUTED, false));
        card.addView(text("Detected " + date(f.getDetectedAt()) + " · " + f.getEntityType() + " " + f.getEntityId(), 14, MUTED, false));
        card.setClickable(!fieldOfficer); card.setFocusable(!fieldOfficer);
        if (!fieldOfficer) card.setOnClickListener(v -> startActivity(new android.content.Intent(this, IntegrityFindingDetailActivity.class)
                .putExtra(IntegrityFindingDetailActivity.EXTRA_FINDING_ID, f.getId())));
        return card;
    }

    private void runScan() {
        if (farmId <= 0 || fieldOfficer) return;
        runButton.setEnabled(false);
        runLabel.setText("Scanning…");
        io.execute(() -> {
            IntegrityScanRunEntity result = new IntegrityScanner(getApplicationContext()).scan(farmId, "manual");
            runOnUiThread(() -> {
                runButton.setEnabled(true); runLabel.setText("Run integrity scan");
                android.widget.Toast.makeText(this, "COMPLETED".equals(result.getStatus()) ? "Integrity scan complete" : "Integrity scan failed", android.widget.Toast.LENGTH_SHORT).show();
            });
        });
    }

    private boolean isFieldOfficerFinding(IntegrityFindingEntity f) {
        return "supplier".equals(f.getEntityType()) || "quality_batch".equals(f.getEntityType())
                || "supplier_inspection".equals(f.getEntityType());
    }
    private String shortDescription(String value) { if (value == null) return "Integrity finding"; int p = value.indexOf('.'); return p > 0 ? value.substring(0, p) : value; }
    private int statusBg(String s) { if ("RESOLVED".equals(s)) return PALE_GREEN; if ("UNDER_REVIEW".equals(s)) return PALE_GOLD; if ("REJECTED".equals(s)) return 0xFFE9E6E0; return PALE_RED; }
    private int statusColor(String s) { if ("RESOLVED".equals(s)) return 0xFF0A5530; if ("UNDER_REVIEW".equals(s)) return GOLD; if ("REJECTED".equals(s)) return MUTED; return RED; }
    private String date(long t) { return t <= 0 ? "—" : new SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(new Date(t)); }
    private String safe(String s) { return s == null || s.trim().isEmpty() ? "—" : s; }
    private int dp(float v) { return (int) (v * getResources().getDisplayMetrics().density + .5f); }
    private TextView text(String s, int sp, int color, boolean bold) { TextView v = new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(color); if (bold) v.setTypeface(Typeface.create("sans-serif", Typeface.BOLD)); v.setPadding(0, dp(5), 0, dp(5)); return v; }
    private LinearLayout horizontal() { LinearLayout r = new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL); r.setGravity(Gravity.CENTER_VERTICAL); return r; }
    private LinearLayout card() { LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(18), dp(16), dp(18), dp(16)); c.setBackground(shape(WHITE, 24, 0xFFEFE6D7)); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.bottomMargin = dp(12); c.setLayoutParams(p); return c; }
    private View pill(String s, int bg, int fg, int size, int weight) { LinearLayout l = new LinearLayout(this); l.setGravity(Gravity.CENTER); l.setPadding(dp(12), 0, dp(12), 0); l.setBackground(shape(bg, 999, Color.TRANSPARENT)); TextView t = text(s, size, fg, weight >= 700); t.setGravity(Gravity.CENTER); t.setPadding(dp(2), 0, dp(2), 0); l.addView(t); return l; }
    private GradientDrawable shape(int color, int radius, int stroke) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); if (stroke != Color.TRANSPARENT) d.setStroke(dp(1), stroke); return d; }
    private void section(LinearLayout root, String name, int margin) { TextView t = text(name, 15, GREEN, true); t.setLetterSpacing(.12f); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.topMargin = margin; p.bottomMargin = dp(8); root.addView(t, p); }
    private TextView empty(String s) { TextView v = text(s, 16, MUTED, false); v.setPadding(dp(4), dp(10), dp(4), dp(14)); return v; }
    private void addDivider(LinearLayout root) { View d = new View(this); d.setBackgroundColor(0xFFE9E1CF); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(1)); p.topMargin = dp(10); p.bottomMargin = dp(8); root.addView(d, p); }
    @Override protected void onDestroy() { super.onDestroy(); io.shutdown(); }
}
