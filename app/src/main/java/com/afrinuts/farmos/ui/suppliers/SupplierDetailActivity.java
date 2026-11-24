package com.afrinuts.farmos.ui.suppliers;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;
import com.afrinuts.farmos.data.repository.QualityBatchRepository;
import com.afrinuts.farmos.data.repository.SupplierRepository;
import com.afrinuts.farmos.ui.BaseActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// --- new imports (add at the top of the file) ---
import com.afrinuts.farmos.data.local.entity.SupplierCommitmentEntity;
import com.afrinuts.farmos.data.local.entity.SupplierDisputeEntity;
import com.afrinuts.farmos.data.local.entity.SupplierInspectionEntity;
import com.afrinuts.farmos.data.local.entity.SupplierScoreSnapshotEntity;
import com.afrinuts.farmos.data.repository.SupplierCommitmentRepository;
import com.afrinuts.farmos.data.repository.SupplierDisputeRepository;
import com.afrinuts.farmos.data.repository.SupplierInspectionRepository;
import com.afrinuts.farmos.data.repository.SupplierScoreSnapshotRepository;

import java.util.Date;


public class SupplierDetailActivity extends BaseActivity {

    public static final String EXTRA_SUPPLIER_ID = "supplier_id";

    private long supplierId;
    private SupplierEntity supplier;
    private SupplierRepository supplierRepository;
    private QualityBatchRepository qualityBatchRepository;
    private AppDatabase database;
    private long farmId = 1;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements
    private TextView tvSupplierName;
    private TextView tvSupplierType;
    private TextView tvRating;
    private Chip chipStatus;
    private TextView tvContactPerson;
    private TextView tvPhone;
    private TextView tvEmail;
    private TextView tvLocation;
    private TextView tvAnnualVolume;
    private TextView tvAvgQualityScore;
    private LinearLayout layoutCertification;
    private TextView tvCertification;
    private TextView tvCertificationExpiry;
    private TextView tvTotalBatches;
    private TextView tvTotalBatchVolume;
    private RecyclerView batchesRecyclerView;
    private SupplierBatchAdapter batchAdapter;
    private MaterialButton btnRecordDelivery;
    private MaterialButton btnEditSupplier;
    private MaterialButton btnDelete;
    private Button btnShowMap;
    private View layoutFarmerProfile;


    // ✅ Vector icon ImageViews
    private ImageView ivCertificationIcon;
    private ImageView ivStatusIcon;

    private List<QualityBatchEntity> batches = new ArrayList<>();

    private CardView cardCommitments, cardInspections, cardDisputes, cardScoreSnapshot;
    private LinearLayout containerCommitments, containerInspections, containerDisputes;
    private TextView tvCommitmentsEmpty, tvInspectionsEmpty, tvDisputesEmpty;
    private TextView tvOverallScore, tvConfidence, tvScoreMeta;
    private LinearLayout tvScoreBreakdown;
    private com.google.android.material.button.MaterialButton btnAddCommitment, btnAddInspection;

    private SupplierCommitmentRepository commitmentRepository;
    private SupplierInspectionRepository inspectionRepository;
    private SupplierDisputeRepository disputeRepository;
    private SupplierScoreSnapshotRepository snapshotRepository;
    private SimpleDateFormat shortDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Override
    protected boolean checkRoleAccess() {
        return com.afrinuts.farmos.utils.AccessMap.canOpenScreen(
                getClass().getSimpleName(), sessionManager.getUserRole());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_supplier_detail);
        com.afrinuts.farmos.ui.TabletPane.attachList(this, savedInstanceState, new com.afrinuts.farmos.ui.suppliers.SuppliersFragment());

        supplierId = getIntent().getLongExtra(EXTRA_SUPPLIER_ID, -1);
        if (supplierId == -1) {
            Toast.makeText(this, "Supplier not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        database = AppDatabase.getInstance(this);
        supplierRepository = new SupplierRepository(this, farmId);
        qualityBatchRepository = new QualityBatchRepository(this, farmId);

        initViews();
        setupToolbar();
        loadSupplierData();
        loadSupplierBatches();
    }

    private void initViews() {

        tvSupplierName = findViewById(R.id.tvSupplierName);
        tvSupplierType = findViewById(R.id.tvSupplierType);
        tvRating = findViewById(R.id.tvRating);
        chipStatus = findViewById(R.id.chipStatus);
        tvContactPerson = findViewById(R.id.tvContactPerson);
        tvPhone = findViewById(R.id.tvPhone);
        tvEmail = findViewById(R.id.tvEmail);
        tvLocation = findViewById(R.id.tvLocation);
        tvAnnualVolume = findViewById(R.id.tvAnnualVolume);
        tvAvgQualityScore = findViewById(R.id.tvAvgQualityScore);
        layoutCertification = findViewById(R.id.layoutCertification);
        tvCertification = findViewById(R.id.tvCertification);
        tvCertificationExpiry = findViewById(R.id.tvCertificationExpiry);
        tvTotalBatches = findViewById(R.id.tvTotalBatches);
        tvTotalBatchVolume = findViewById(R.id.tvTotalBatchVolume);
        batchesRecyclerView = findViewById(R.id.batchesRecyclerView);
        btnRecordDelivery = findViewById(R.id.btnRecordDelivery);
        btnEditSupplier = findViewById(R.id.btnEditSupplier);
        btnDelete = findViewById(R.id.btnDelete);
        btnShowMap = findViewById(R.id.btnShowMap);

        // ✅ Vector icon ImageViews
        ivCertificationIcon = findViewById(R.id.ivCertificationIcon);
        ivStatusIcon = findViewById(R.id.ivStatusIcon);

        // Setup RecyclerView for batches
        batchesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        batchAdapter = new SupplierBatchAdapter(batches);
        batchAdapter.setOnBatchUpdatedListener(() -> {
            loadSupplierBatches();
            loadSupplierData();
        });

        batchAdapter.setOnBatchDisputeClickListener(batch -> {
            openDisputeDialog(
                    SupplierDisputeEntity.TargetEntity.BATCH,
                    batch.getId(),
                    batch.getBatchNumber());
        });

        batchesRecyclerView.setAdapter(batchAdapter);

        cardCommitments      = findViewById(R.id.cardCommitments);
        cardInspections      = findViewById(R.id.cardInspections);
        cardDisputes         = findViewById(R.id.cardDisputes);
        cardScoreSnapshot    = findViewById(R.id.cardScoreSnapshot);
        containerCommitments = findViewById(R.id.containerCommitments);
        containerInspections = findViewById(R.id.containerInspections);
        containerDisputes    = findViewById(R.id.containerDisputes);
        tvCommitmentsEmpty   = findViewById(R.id.tvCommitmentsEmpty);
        tvInspectionsEmpty   = findViewById(R.id.tvInspectionsEmpty);
        tvDisputesEmpty      = findViewById(R.id.tvDisputesEmpty);
        tvOverallScore       = findViewById(R.id.tvOverallScore);
        tvConfidence         = findViewById(R.id.tvConfidence);
        tvScoreBreakdown     = findViewById(R.id.tvScoreBreakdown);
        tvScoreMeta          = findViewById(R.id.tvScoreMeta);
        btnAddCommitment     = findViewById(R.id.btnAddCommitment);
        btnAddInspection     = findViewById(R.id.btnAddInspection);

        // Buttons
        btnShowMap.setOnClickListener(v -> openMap());
        btnRecordDelivery.setOnClickListener(v -> showRecordDeliveryDialog());
        btnEditSupplier.setOnClickListener(v -> showEditSupplierDialog());
        btnDelete.setOnClickListener(v -> confirmDelete());
        chipStatus.setOnClickListener(v -> showStatusChangeDialog());
    }

    private void setupToolbar() {
        com.afrinuts.farmos.ui.farm.FarmDetailChrome.apply(this,
                findViewById(R.id.farmDetailTitle), findViewById(R.id.farmDetailBack),
                findViewById(R.id.farmDetailMore), "Supplier details" );
        styleSupplierSectionHeadings();
        styleSectionActionButtons();
        com.afrinuts.farmos.ui.farm.FarmDetailChrome.addHero(this, R.id.tvSupplierName, R.id.tvSupplierType, R.id.chipStatus);
        addSupplierHeroRating();
        com.afrinuts.farmos.ui.farm.FarmDetailChrome.addStats(this,
                new String[]{"Trust score", "Batches", "Total volume"},
                new int[]{R.id.tvOverallScore, R.id.tvTotalBatches, R.id.tvTotalBatchVolume });
        reorderSupplierStats();
        stylePerformanceMetrics();
        com.afrinuts.farmos.ui.farm.FarmDetailChrome.addBottomActions(this, R.id.btnEditSupplier, R.id.btnRecordDelivery);

        // Supplier actions are content actions on this screen, so keep the chrome row
        // at the end of the scrollable content instead of pinning it below the scroll.
        View actions = findViewById(android.R.id.content).findViewWithTag("farm-detail-actions");
        LinearLayout actionContainer = findViewById(R.id.layoutBottomActions);
        if (actions != null && actions.getParent() instanceof android.view.ViewGroup && actionContainer != null) {
            ((android.view.ViewGroup) actions.getParent()).removeView(actions);
            actions.setPadding(0, 0, 0, 0);
            actions.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            if (actions instanceof LinearLayout && ((LinearLayout) actions).getChildCount() == 2) {
                View first = ((LinearLayout) actions).getChildAt(0);
                LinearLayout.LayoutParams firstParams = (LinearLayout.LayoutParams) first.getLayoutParams();
                firstParams.setMargins(0, 0, dp(12), 0);
                first.setLayoutParams(firstParams);
            }
            actionContainer.addView(actions, 0, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        styleSupplierBottomActions();

        ImageButton headerEdit = findViewById(R.id.farmDetailEdit);
        if (headerEdit != null) headerEdit.setVisibility(View.GONE);
        ImageButton more = findViewById(R.id.farmDetailMore);
        if (more != null) {
            more.setVisibility(View.VISIBLE);
            more.setOnClickListener(anchor -> {
                androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, anchor);
                popup.getMenu().add("Edit Batch").setOnMenuItemClickListener(item -> {
                    showSelectBatchToEditDialog();
                    return true;
                });
                popup.show();
            });
        }
    }

    private void reorderSupplierStats() {
        View row = findViewById(android.R.id.content).findViewWithTag("farm-detail-stats");
        if (!(row instanceof LinearLayout)) return;
        LinearLayout stats = (LinearLayout) row;
        for (int i = 0; i < stats.getChildCount(); i++) {
            if (!(stats.getChildAt(i) instanceof LinearLayout)) continue;
            LinearLayout card = (LinearLayout) stats.getChildAt(i);
            TextView value = null;
            TextView unit = null;
            TextView caption = null;
            for (int j = 0; j < card.getChildCount(); j++) {
                View child = card.getChildAt(j);
                if (!(child instanceof TextView)) continue;
                Object tag = child.getTag();
                if (tag instanceof String && ((String) tag).startsWith("farm-detail-stat-value:")) value = (TextView) child;
                else if (tag instanceof String && ((String) tag).startsWith("farm-detail-stat-unit:")) unit = (TextView) child;
                else caption = (TextView) child;
            }
            if (value != null && unit != null && caption != null) {
                card.removeAllViews();
                card.addView(caption, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                card.addView(value, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                card.addView(unit, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            }
        }
    }

    private void stylePerformanceMetrics() {
        CardView card = findViewById(R.id.cardPerformanceMetrics);
        if (card == null || !(card.getChildAt(0) instanceof LinearLayout)) return;
        LinearLayout body = (LinearLayout) card.getChildAt(0);
        View oldStats = body.getChildAt(1);
        if (!(oldStats instanceof LinearLayout)) return;
        LinearLayout stats = (LinearLayout) oldStats;
        TextView annualVolume = findViewById(R.id.tvAnnualVolume);
        TextView avgQuality = findViewById(R.id.tvAvgQualityScore);
        if (annualVolume == null || avgQuality == null) return;

        body.removeView(stats);
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        addMetricRow(rows, "Annual Volume", annualVolume);
        addMetricRow(rows, "Avg Quality", avgQuality);
        rows.removeViewAt(rows.getChildCount() - 1);
        body.addView(rows, 1, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout certification = findViewById(R.id.layoutCertification);
        if (certification != null) {
            ImageView icon = findViewById(R.id.ivCertificationIcon);
            if (icon != null) icon.setVisibility(View.GONE);
            certification.setOrientation(LinearLayout.HORIZONTAL);
            certification.setGravity(android.view.Gravity.CENTER_VERTICAL);
            certification.setPadding(0, dp(11), 0, dp(11));
            TextView label = new TextView(this);
            label.setText("Certification");
            styleMetricLabel(label);
            LinearLayout valueColumn = new LinearLayout(this);
            valueColumn.setOrientation(LinearLayout.VERTICAL);
            valueColumn.setGravity(android.view.Gravity.END);
            certification.removeView(findViewById(R.id.tvCertification));
            certification.removeView(findViewById(R.id.tvCertificationExpiry));
            TextView certName = findViewById(R.id.tvCertification);
            TextView certExpiry = findViewById(R.id.tvCertificationExpiry);
            if (certName != null) {
                certName.setTextColor(0xFF1C1B1A);
                certName.setTextSize(14.5f);
                valueColumn.addView(certName);
            }
            if (certExpiry != null) {
                certExpiry.setTextColor(0xFF59544C);
                certExpiry.setTextSize(12.5f);
                valueColumn.addView(certExpiry);
            }
            certification.addView(label, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            certification.addView(valueColumn, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
    }

    private void styleSectionActionButtons() {
        for (MaterialButton button : new MaterialButton[]{btnAddCommitment, btnAddInspection}) {
            if (button == null) continue;
            button.setMinHeight(dp(38));
            button.setMinimumHeight(dp(38));
            button.setInsetTop(0);
            button.setInsetBottom(0);
            button.setCornerRadius(dp(999));
            button.setTextSize(13.5f);
            button.setPadding(dp(14), 0, dp(14), 0);
            ViewGroup.LayoutParams params = button.getLayoutParams();
            if (params != null) {
                params.height = dp(38);
                button.setLayoutParams(params);
            }
        }
    }

    private void styleSupplierBottomActions() {
        View actions = findViewById(android.R.id.content).findViewWithTag("farm-detail-actions");
        if (actions instanceof LinearLayout) {
            LinearLayout row = (LinearLayout) actions;
            for (int i = 0; i < row.getChildCount(); i++) {
                View child = row.getChildAt(i);
                if (!(child instanceof MaterialButton)) continue;
                MaterialButton button = (MaterialButton) child;
                button.setElevation(0f);
                button.setStateListAnimator(null);
                button.setInsetTop(0);
                button.setInsetBottom(0);
                button.setCornerRadius(dp(999));
            }
        }
        if (btnDelete instanceof MaterialButton) {
            MaterialButton delete = (MaterialButton) btnDelete;
            delete.setElevation(0f);
            delete.setStateListAnimator(null);
            delete.setInsetTop(0);
            delete.setInsetBottom(0);
            delete.setCornerRadius(dp(999));
            delete.setMinHeight(dp(52));
            delete.setMinimumHeight(dp(52));
            ViewGroup.LayoutParams params = delete.getLayoutParams();
            if (params != null) {
                params.height = dp(52);
                delete.setLayoutParams(params);
            }
        }
    }

    private void addMetricRow(LinearLayout container, String labelText, TextView value) {
        if (value.getParent() instanceof ViewGroup) {
            ((ViewGroup) value.getParent()).removeView(value);
        }
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(11), 0, dp(11));
        TextView label = new TextView(this);
        label.setText(labelText);
        styleMetricLabel(label);
        value.setTextColor(0xFF1C1B1A);
        value.setTextSize(14.5f);
        value.setGravity(android.view.Gravity.END);
        row.addView(label, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(value, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        container.addView(row);
        View divider = new View(this);
        divider.setBackgroundColor(0xFFE9E1CF);
        container.addView(divider, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
    }

    private void styleMetricLabel(TextView label) {
        label.setTextColor(0xFF59544C);
        label.setTextSize(13);
        label.setTypeface(android.graphics.Typeface.create(
                androidx.core.content.res.ResourcesCompat.getFont(this, R.font.manrope_bold),
                android.graphics.Typeface.NORMAL));
    }

    private void styleSupplierSectionHeadings() {
        int[] ids = {R.id.headingContactInfo, R.id.headingAbout, R.id.headingPerformance,
                R.id.headingCommitments, R.id.headingInspections, R.id.headingDisputes,
                R.id.headingTrustScore};
        for (int id : ids) {
            TextView heading = findViewById(id);
            if (heading != null) heading.setTextColor(0xFF1C1B1A);
        }
    }

    private void addSupplierHeroRating() {
        View hero = findViewById(android.R.id.content).findViewWithTag("farm-detail-hero");
        if (!(hero instanceof CardView)) return;
        LinearLayout body = (LinearLayout) ((CardView) hero).getChildAt(0);
        if (body.findViewWithTag("supplier-hero-rating") != null) return;

        // Use the supplier-type icon in the chrome's avatar slot; the name/type/status
        // themselves continue to be copied by FarmDetailChrome from the source views.
        if (body.getChildCount() > 0 && body.getChildAt(0) instanceof LinearLayout) {
            LinearLayout top = (LinearLayout) body.getChildAt(0);
            if (top.getChildCount() > 0 && top.getChildAt(0) instanceof TextView) {
                TextView avatar = (TextView) top.getChildAt(0);
                android.graphics.drawable.GradientDrawable avatarBg = new android.graphics.drawable.GradientDrawable();
                avatarBg.setColor(0xFF0E6B3C);
                avatarBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                FrameLayout avatarFrame = new FrameLayout(this);
                avatarFrame.setTag("supplier-hero-avatar");
                avatarFrame.setBackground(avatarBg);
                ViewGroup.LayoutParams avatarParams = avatar.getLayoutParams();
                top.removeView(avatar);
                top.addView(avatarFrame, 0, avatarParams);
                setSupplierHeroIcon(R.drawable.ic_person);
            }
        }

        TextView stars = new TextView(this);
        stars.setTag("supplier-hero-rating");
        stars.setText(tvRating.getText());
        stars.setTextColor(0xFFD9A62E);
        stars.setTextSize(17);
        stars.setIncludeFontPadding(false);
        stars.setLetterSpacing(0.08f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(12);
        params.bottomMargin = dp(2);
        params.leftMargin = dp(68);
        body.addView(stars, Math.min(1, body.getChildCount()), params);
        TextView heroName = body.findViewWithTag("farm-detail-hero-name");
        if (heroName != null) {
            heroName.setTextSize(21);
            heroName.setMaxLines(2);
            heroName.setLineSpacing(dp(2), 1f);
            heroName.setTypeface(android.graphics.Typeface.create(
                    androidx.core.content.res.ResourcesCompat.getFont(this, R.font.manrope_extrabold),
                    android.graphics.Typeface.NORMAL));
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void updateSupplierHeroTypeIcon() {
        View hero = findViewById(android.R.id.content).findViewWithTag("farm-detail-hero");
        if (!(hero instanceof CardView) || supplier == null) return;
        LinearLayout body = (LinearLayout) ((CardView) hero).getChildAt(0);
        if (body.getChildCount() == 0 || !(body.getChildAt(0) instanceof LinearLayout)) return;
        LinearLayout top = (LinearLayout) body.getChildAt(0);
        View avatar = top.findViewWithTag("supplier-hero-avatar");
        if (!(avatar instanceof FrameLayout)) return;
        setSupplierHeroIcon(supplier.getTypeDisplay().toLowerCase(Locale.ROOT).contains("coop")
                ? R.drawable.ic_people : R.drawable.ic_person);
    }

    private void setSupplierHeroIcon(int iconResource) {
        View avatar = findViewById(android.R.id.content).findViewWithTag("supplier-hero-avatar");
        if (!(avatar instanceof FrameLayout)) return;
        FrameLayout frame = (FrameLayout) avatar;
        frame.removeAllViews();
        ImageView iconView = new ImageView(this);
        iconView.setImageResource(iconResource);
        iconView.setColorFilter(android.graphics.Color.WHITE);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(26), dp(26),
                android.view.Gravity.CENTER);
        frame.addView(iconView, params);
    }

    private void openMap() {
        if (supplier == null) {
            Toast.makeText(this, "No location data available", Toast.LENGTH_SHORT).show();
            return;
        }
        String location = supplier.getLocation();
        if (supplier.hasLocation()) location = supplier.getLatitude() + "," + supplier.getLongitude();
        if (TextUtils.isEmpty(location)) {
            Toast.makeText(this, "No location data available", Toast.LENGTH_SHORT).show();
            return;
        }
        String query = Uri.encode(location);
        String uri = supplier.hasLocation()
                ? "geo:" + supplier.getLatitude() + "," + supplier.getLongitude() + "?q=" + query
                : "geo:0,0?q=" + query;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadSupplierData() {
        supplierRepository.getSupplierById(supplierId).observe(this, supplier -> {
            if (supplier != null) {
                this.supplier = supplier;
                displaySupplierData();
                loadTrustSections();
            } else {
                Toast.makeText(this, "Supplier not found", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void loadSupplierBatches() {
        executor.execute(() -> {
            try {
                List<QualityBatchEntity> supplierBatches =
                        qualityBatchRepository.getBatchesBySupplierIdSync(supplierId);

                runOnUiThread(() -> {
                    batches.clear();
                    if (supplierBatches != null && !supplierBatches.isEmpty()) {
                        batches.addAll(supplierBatches);
                    }
                    batchAdapter.updateBatches(batches);
                    updateBatchStats();

                    Log.d("SupplierDetail", "Loaded " + batches.size() + " batches for supplier " + supplierId);
                });
            } catch (Exception e) {
                Log.e("SupplierDetail", "Error loading batches", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading batches", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateBatchStats() {
        if (batches.isEmpty()) {
            tvTotalBatches.setText("Total: 0 batches");
            tvTotalBatchVolume.setText("Total: 0 kg");
            tvAnnualVolume.setText("0 kg");
            return;
        }

        int count = batches.size();
        double totalVolume = 0;
        for (QualityBatchEntity batch : batches) {
            totalVolume += batch.getWeightKg();
        }

        tvTotalBatches.setText("Total: " + count + " batch" + (count > 1 ? "es" : ""));
        tvTotalBatchVolume.setText(String.format(Locale.getDefault(), "Total: %,.0f kg", totalVolume));
        tvAnnualVolume.setText(String.format(Locale.getDefault(), "%,.0f kg", totalVolume));
    }

    private void displaySupplierData() {
        tvSupplierName.setText(supplier.getName());
        tvSupplierType.setText(supplier.getTypeDisplay());
        tvRating.setText(supplier.getFormattedRating());
        updateSupplierHeroTypeIcon();
        TextView heroRating = findViewById(android.R.id.content).findViewWithTag("supplier-hero-rating");
        if (heroRating != null) heroRating.setText(supplier.getFormattedRating());

        updateStatusDisplay();

        // ✅ Set icons with vector drawables
        setVectorIcon(tvContactPerson, R.drawable.ic_person);
        setVectorIcon(tvPhone, R.drawable.ic_phone);
        setVectorIcon(tvEmail, R.drawable.ic_email);
        setVectorIcon(tvLocation, R.drawable.ic_location);

        tvContactPerson.setText(supplier.getContactPerson() != null ?
                supplier.getContactPerson() : "No contact");
        tvPhone.setText(supplier.getPhone() != null ? supplier.getPhone() : "No phone");
        tvEmail.setText(supplier.getEmail() != null ? supplier.getEmail() : "No email");
        tvLocation.setText(supplier.getLocation() != null ? supplier.getLocation() : "No location");
        btnShowMap.setVisibility(!TextUtils.isEmpty(supplier.getLocation()) || supplier.hasLocation()
                ? View.VISIBLE : View.GONE);
        setMapButtonIcon();

        tvAnnualVolume.setText(String.format(Locale.getDefault(),
                "%,.0f kg", supplier.getAnnualVolumeKg()));
        tvAvgQualityScore.setText(String.format(Locale.getDefault(),
                "%.0f", supplier.getAverageQualityScore()));

        // ✅ Certification with vector icon
        if (supplier.isCertified() && supplier.getCertificationType() != null) {
            layoutCertification.setVisibility(View.VISIBLE);

            // Set certification icon
            if (ivCertificationIcon != null) {
                ivCertificationIcon.setImageResource(R.drawable.ic_certified);
                ivCertificationIcon.setVisibility(View.VISIBLE);
            }

            tvCertification.setText(supplier.getCertificationType());

            if (supplier.getCertificationExpiryDate() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
                tvCertificationExpiry.setText("Expires: " +
                        sdf.format(supplier.getCertificationExpiryDate()));
            } else {
                tvCertificationExpiry.setText("No expiry");
            }
        } else {
            layoutCertification.setVisibility(View.GONE);
        }

        if (!sessionManager.canEditData()) {
            btnDelete.setVisibility(View.GONE);
        }

        // ✅ Fix: Use View instead of LinearLayout
        TextView tvYearsFarming = findViewById(R.id.tvYearsFarming);
        TextView tvFarmSize = findViewById(R.id.tvFarmSize);
        TextView tvQuote = findViewById(R.id.tvQuote);
        TextView tvBio = findViewById(R.id.tvBio);
        View layoutFirebreak = findViewById(R.id.layoutFirebreak);
        View layoutSustainable = findViewById(R.id.layoutSustainable);
        View layoutChildLabor = findViewById(R.id.layoutChildLabor);
        CardView layoutFarmerProfile = findViewById(R.id.layoutFarmerProfile);

        // Show farmer profile section only if there's data
        boolean hasProfileData = supplier.getFarmSize() > 0 ||
                !TextUtils.isEmpty(supplier.getQuote()) ||
                !TextUtils.isEmpty(supplier.getBio()) || supplier.isFirebreakProtection()
                || supplier.isSustainableHarvesting() || supplier.isNoChildLabor();

        if (hasProfileData) {
            layoutFarmerProfile.setVisibility(View.VISIBLE);
            findViewById(R.id.rowYearsFarming).setVisibility(View.GONE);
            tvYearsFarming.setText(supplier.getFormattedYearsFarming());
            tvFarmSize.setText(String.format(Locale.getDefault(), "%.1f ha", supplier.getFarmSize()));

            if (!TextUtils.isEmpty(supplier.getQuote())) {
                tvQuote.setText("\"" + supplier.getQuote() + "\"");
                tvQuote.setVisibility(View.VISIBLE);
            } else {
                tvQuote.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(supplier.getBio())) {
                tvBio.setText(supplier.getBio());
                tvBio.setVisibility(View.VISIBLE);
            } else {
                tvBio.setVisibility(View.GONE);
            }

            // Sustainability badges
            layoutFirebreak.setVisibility(supplier.isFirebreakProtection() ? View.VISIBLE : View.GONE);
            layoutSustainable.setVisibility(supplier.isSustainableHarvesting() ? View.VISIBLE : View.GONE);
            layoutChildLabor.setVisibility(supplier.isNoChildLabor() ? View.VISIBLE : View.GONE);
            stylePracticeChip(layoutFirebreak, true);
            stylePracticeChip(layoutSustainable, true);
            stylePracticeChip(layoutChildLabor, false);

        } else {
            layoutFarmerProfile.setVisibility(View.GONE);
        }
    }

    private void setMapButtonIcon() {
        Drawable icon = ContextCompat.getDrawable(this, R.drawable.ic_location);
        if (icon == null) return;
        icon = DrawableCompat.wrap(icon.mutate());
        DrawableCompat.setTint(icon, 0xFF0A5530);
        icon.setBounds(0, 0, dp(14), dp(14));
        btnShowMap.setCompoundDrawables(icon, null, null, null);
        btnShowMap.setCompoundDrawablePadding(dp(4));
        btnShowMap.setMinHeight(dp(34));
        btnShowMap.setMinimumHeight(dp(34));
        btnShowMap.setPadding(dp(14), 0, dp(14), 0);
        btnShowMap.setIncludeFontPadding(false);
        if (btnShowMap instanceof MaterialButton) {
            MaterialButton mapButton = (MaterialButton) btnShowMap;
            mapButton.setInsetTop(0);
            mapButton.setInsetBottom(0);
        }
    }

    private void stylePracticeChip(View view, boolean verified) {
        if (!(view instanceof LinearLayout)) return;
        LinearLayout chip = (LinearLayout) view;
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(android.view.Gravity.CENTER_VERTICAL);
        chip.setWeightSum(0);
        chip.setPadding(dp(12), dp(7), dp(12), dp(7));
        chip.setBackground(new android.graphics.drawable.GradientDrawable() {{
            setColor(verified ? 0xFFE3F0E7 : 0xFFF3EAD8);
            setCornerRadius(dp(999));
        }});
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) chip.getLayoutParams();
        params.width = LinearLayout.LayoutParams.WRAP_CONTENT;
        params.weight = 0;
        params.setMargins(0, 0, dp(7), 0);
        chip.setLayoutParams(params);
        if (chip.getChildCount() > 0) chip.getChildAt(0).setVisibility(View.GONE);
        if (chip.getChildCount() > 1 && chip.getChildAt(1) instanceof TextView) {
            TextView label = (TextView) chip.getChildAt(1);
            label.setTextSize(12.5f);
            label.setTypeface(android.graphics.Typeface.create(
                    androidx.core.content.res.ResourcesCompat.getFont(this, R.font.manrope_extrabold),
                    android.graphics.Typeface.NORMAL));
            label.setTextColor(verified ? 0xFF064526 : 0xFF60450E);
            label.setAlpha(1f);
        }
    }

    // ✅ Helper method to set compound drawable vector icons
    private void setVectorIcon(TextView textView, int iconRes) {
        Drawable drawable = ContextCompat.getDrawable(this, iconRes);
        if (drawable != null) {
            // Apply tint using DrawableCompat
            drawable = DrawableCompat.wrap(drawable);
            DrawableCompat.setTint(drawable, getColor(com.google.android.material.R.color.design_default_color_on_primary));
            drawable.setBounds(0, 0, 24, 24);
            textView.setCompoundDrawables(drawable, null, null, null);
            textView.setCompoundDrawablePadding(12);
        }
    }

    private void updateStatusDisplay() {
        String statusText = supplier.getStatusDisplay();
        chipStatus.setText(statusText);
        TextView heroStatus = getWindow().getDecorView().findViewWithTag("farm-detail-hero-status");
        if (heroStatus != null) {
            heroStatus.setText("•  " + statusText);
            int foreground;
            int background;
            switch (supplier.getStatus()) {
                case ACTIVE:
                    foreground = 0xFF0A5530;
                    background = 0xFFE3F0E7;
                    break;
                case PENDING:
                    foreground = 0xFF77580A;
                    background = 0xFFFFF2D8;
                    break;
                case SUSPENDED:
                    foreground = 0xFF8C1D18;
                    background = 0xFFFDE8E7;
                    break;
                case INACTIVE:
                default:
                    foreground = 0xFF59544C;
                    background = 0xFFF3EAD8;
                    break;
            }
            heroStatus.setTextColor(foreground);
            android.graphics.drawable.GradientDrawable pill = new android.graphics.drawable.GradientDrawable();
            pill.setColor(background);
            pill.setCornerRadius(999 * getResources().getDisplayMetrics().density);
            heroStatus.setBackground(pill);
        }

        // ✅ Set status icon
        if (ivStatusIcon != null) {
            switch (supplier.getStatus()) {
                case ACTIVE:
                    ivStatusIcon.setImageResource(R.drawable.ic_check_circle);
                    ivStatusIcon.setColorFilter(getColor(android.R.color.holo_green_light));
                    break;
                case PENDING:
                    ivStatusIcon.setImageResource(R.drawable.ic_hourglass);
                    ivStatusIcon.setColorFilter(getColor(android.R.color.holo_orange_light));
                    break;
                case SUSPENDED:
                    ivStatusIcon.setImageResource(R.drawable.ic_warning);
                    ivStatusIcon.setColorFilter(getColor(android.R.color.holo_red_light));
                    break;
                case INACTIVE:
                    ivStatusIcon.setImageResource(R.drawable.ic_cancel);
                    ivStatusIcon.setColorFilter(getColor(android.R.color.darker_gray));
                    break;
                default:
                    ivStatusIcon.setImageResource(R.drawable.ic_check_circle);
                    ivStatusIcon.setColorFilter(getColor(R.color.primary));
                    break;
            }
        }

        switch (supplier.getStatus()) {
            case ACTIVE:
                chipStatus.setChipBackgroundColorResource(android.R.color.holo_green_light);
                break;
            case PENDING:
                chipStatus.setChipBackgroundColorResource(android.R.color.holo_orange_light);
                break;
            case SUSPENDED:
                chipStatus.setChipBackgroundColorResource(android.R.color.holo_red_light);
                break;
            case INACTIVE:
                chipStatus.setChipBackgroundColorResource(android.R.color.darker_gray);
                break;
            default:
                chipStatus.setChipBackgroundColorResource(R.color.primary);
                break;
        }
    }

    private void showStatusChangeDialog() {
        if (supplier == null) return;
        StatusChangeDialog.newInstance(supplier.getStatus().ordinal())
                .show(getSupportFragmentManager(), "SupplierStatusChangeDialog");
    }

    private void updateSupplierStatus(SupplierEntity.SupplierStatus newStatus) {
        if (supplier == null) return;

        supplier.setStatus(newStatus);
        supplier.setActive(newStatus == SupplierEntity.SupplierStatus.ACTIVE);
        supplier.setUpdatedAt(System.currentTimeMillis());

        supplierRepository.updateSupplier(supplier, new SupplierRepository.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(() -> {
                    updateStatusDisplay();
                    Toast.makeText(SupplierDetailActivity.this,
                            "Status updated to: " + newStatus.getDisplayName(),
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(SupplierDetailActivity.this,
                                "Error updating status: " + error,
                                Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    private void showEditSupplierDialog() {
        EditSupplierDialog dialog = EditSupplierDialog.newInstance(supplierId);
        dialog.setOnSupplierUpdatedListener(() -> {
            loadSupplierData();
            Toast.makeText(this, "Supplier updated!", Toast.LENGTH_SHORT).show();
        });
        dialog.show(getSupportFragmentManager(), "EditSupplierDialog");
    }

    private void showSelectBatchToEditDialog() {
        if (batches.isEmpty()) {
            Toast.makeText(this, "No batches to edit. Record a delivery first.", Toast.LENGTH_SHORT).show();
            return;
        }

        SelectBatchToEditDialog dialog = SelectBatchToEditDialog.newInstance(supplierId);
        dialog.setOnBatchSelectedListener(batch -> {
            EditRecordDeliveryDialog editDialog = EditRecordDeliveryDialog.newInstance(batch.getId());
            editDialog.setOnDeliveryUpdatedListener(() -> {
                loadSupplierBatches();
                loadSupplierData();
                Toast.makeText(this, "Batch updated successfully!", Toast.LENGTH_SHORT).show();
            });
            editDialog.show(getSupportFragmentManager(), "EditRecordDeliveryDialog");
        });
        dialog.show(getSupportFragmentManager(), "SelectBatchToEditDialog");
    }

    private void confirmDelete() {
        if (supplier == null) return;
        DeleteSupplierDialog.newInstance(supplier.getName())
                .show(getSupportFragmentManager(), "DeleteSupplierDialog");
    }

    private void deleteSupplier() {
        if (supplier == null) return;

        Toast.makeText(this, "Deleting supplier...", Toast.LENGTH_SHORT).show();

        supplierRepository.deleteSupplier(supplier, new SupplierRepository.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(() -> {
                    Toast.makeText(SupplierDetailActivity.this,
                            "Supplier deleted", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(SupplierDetailActivity.this,
                                "Error deleting: " + error,
                                Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    private void showRecordDeliveryDialog() {
        RecordDeliveryDialog dialog = RecordDeliveryDialog.newInstance(supplierId);
        dialog.setOnDeliveryRecordedListener(() -> {
            loadSupplierData();
            loadSupplierBatches();
            Toast.makeText(this, "Delivery recorded!", Toast.LENGTH_SHORT).show();
        });
        dialog.show(getSupportFragmentManager(), "RecordDeliveryDialog");
    }

    // ============================================================
// TRUST SECTIONS — 6a
// ============================================================

    private void loadTrustSections() {
        if (supplier == null) return;

        // (Re)build repositories against the currently loaded farm
        if (commitmentRepository == null) {
            commitmentRepository = new SupplierCommitmentRepository(this, farmId);
            inspectionRepository = new SupplierInspectionRepository(this, farmId);
            disputeRepository    = new SupplierDisputeRepository(this, farmId);
            snapshotRepository   = new SupplierScoreSnapshotRepository(this, farmId);
        }

        loadCommitments();
        loadInspections();
        loadDisputes();
        loadScoreSnapshot();
        wireSectionActions();
    }

    private void wireSectionActions() {
        btnAddCommitment.setOnClickListener(v -> {
            if (supplier == null) return;
            RecordCommitmentDialog d = RecordCommitmentDialog.newInstance(supplier.getId(), farmId);
            d.setOnCommitmentCreatedListener(id -> loadCommitments());
            d.show(getSupportFragmentManager(), "RecordCommitmentDialog");
        });

        btnAddInspection.setOnClickListener(v -> {
            if (supplier == null) return;
            RecordInspectionDialog d = RecordInspectionDialog.newInstance(supplier.getId(), farmId);
            d.setOnInspectionCreatedListener(id -> loadInspections());
            d.show(getSupportFragmentManager(), "RecordInspectionDialog");
        });
    }

// ---------- Commitments ----------

    private void loadCommitments() {
        if (supplier == null) return;
        commitmentRepository.getBySupplierId(supplier.getId())
                .thenAccept(list -> runOnUiThread(() -> renderCommitments(list)));
    }

    private void renderCommitments(List<SupplierCommitmentEntity> list) {
        cardCommitments.setVisibility(View.VISIBLE);
        containerCommitments.removeAllViews();

        if (list == null || list.isEmpty()) {
            tvCommitmentsEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tvCommitmentsEmpty.setVisibility(View.GONE);

        for (SupplierCommitmentEntity c : list) {
            View row = getLayoutInflater().inflate(R.layout.item_commitment, containerCommitments, false);
            bindCommitmentRow(row, c);
            containerCommitments.addView(row);
        }
    }

    private void bindCommitmentRow(View row, SupplierCommitmentEntity c) {
        TextView tvContract = row.findViewById(R.id.tvContract);
        TextView tvSeason   = row.findViewById(R.id.tvSeason);
        TextView tvCommitted = row.findViewById(R.id.tvCommitted);
        TextView tvDelivered = row.findViewById(R.id.tvDelivered);
        TextView tvStatus   = row.findViewById(R.id.tvStatus);
        TextView tvDate     = row.findViewById(R.id.tvDate);
        View btnRev         = row.findViewById(R.id.btnAppendRevision);
        View btnLink        = row.findViewById(R.id.btnLinkBatches);
        View btnClose       = row.findViewById(R.id.btnCloseCommitment);
        View btnDispute     = row.findViewById(R.id.btnDisputeCommitment);

        String contract = (c.getContractId() == null || c.getContractId().isEmpty())
                ? "No contract" : c.getContractId();
        tvContract.setText(contract);
        tvSeason.setText("Season " + c.getSeason());
        tvCommitted.setText(String.format(Locale.getDefault(), "Committed: %,.0f kg", c.getCommittedKg()));
        tvDate.setText(shortDate.format(new Date(c.getCommittedDate())));
        tvStatus.setText(c.getStatus() != null ? c.getStatus().name() : "—");

        // Derived delivered kg — only shown once the repository answers
        commitmentRepository.getDeliveredKg(c.getId()).thenAccept(kg ->
                runOnUiThread(() -> tvDelivered.setText(
                        String.format(Locale.getDefault(), "Delivered: %,.0f kg", kg))));

        btnRev.setOnClickListener(v -> {
            AppendRevisionDialog d = AppendRevisionDialog.newInstance(c.getId(), farmId);
            d.setOnRevisionAppendedListener(this::loadCommitments);
            d.show(getSupportFragmentManager(), "AppendRevisionDialog");
        });

        btnLink.setOnClickListener(v -> {
            LinkBatchesDialog d = LinkBatchesDialog.newInstance(c.getId(), supplier.getId(), farmId);
            d.setOnBatchesLinkedListener(this::loadCommitments);
            d.show(getSupportFragmentManager(), "LinkBatchesDialog");
        });

        btnClose.setOnClickListener(v -> {
            CloseCommitmentDialog d = CloseCommitmentDialog.newInstance(c.getId(), farmId);
            d.setOnClosedListener(this::loadCommitments);
            d.show(getSupportFragmentManager(), "CloseCommitmentDialog");
        });

        btnDispute.setOnClickListener(v -> openDisputeDialog(
                SupplierDisputeEntity.TargetEntity.COMMITMENT,
                c.getId(),
                c.getContractId()));
    }

// ---------- Inspections ----------

    private void loadInspections() {
        if (supplier == null) return;
        inspectionRepository.getBySupplierId(supplier.getId())
                .thenAccept(list -> runOnUiThread(() -> renderInspections(list)));
    }

    private void renderInspections(List<SupplierInspectionEntity> list) {
        cardInspections.setVisibility(View.VISIBLE);
        containerInspections.removeAllViews();

        if (list == null || list.isEmpty()) {
            tvInspectionsEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tvInspectionsEmpty.setVisibility(View.GONE);

        for (SupplierInspectionEntity i : list) {
            View row = getLayoutInflater().inflate(R.layout.item_supplier_inspection, containerInspections, false);
            bindInspectionRow(row, i);
            containerInspections.addView(row);
        }
    }

    private void bindInspectionRow(View row, SupplierInspectionEntity i) {
        TextView tvType     = row.findViewById(R.id.tvInspectionType);
        TextView tvOutcome  = row.findViewById(R.id.tvOutcome);
        TextView tvFindings = row.findViewById(R.id.tvFindings);
        TextView tvDate     = row.findViewById(R.id.tvDate);
        TextView tvInspector = row.findViewById(R.id.tvInspector);
        View btnDispute     = row.findViewById(R.id.btnDisputeInspection);

        tvType.setText(i.getInspectionType() != null ? i.getInspectionType().name() : "—");
        tvOutcome.setText(i.getOutcome() != null ? i.getOutcome().name() : "—");
        tvFindings.setText(i.getFindings() != null ? i.getFindings() : "");
        tvDate.setText(shortDate.format(new Date(i.getInspectionDate())));
        tvInspector.setText(i.getInspector() != null ? i.getInspector() : "");

        btnDispute.setOnClickListener(v -> openDisputeDialog(
                SupplierDisputeEntity.TargetEntity.INSPECTION,
                i.getId(),
                (i.getInspectionType() != null ? i.getInspectionType().name() : "Inspection")
                        + " " + shortDate.format(new Date(i.getInspectionDate()))));
    }

// ---------- Disputes ----------

    private void loadDisputes() {
        if (supplier == null) return;
        disputeRepository.getBySupplierId(supplier.getId())
                .thenAccept(list -> runOnUiThread(() -> renderDisputes(list)));
    }

    private void renderDisputes(List<SupplierDisputeEntity> list) {
        cardDisputes.setVisibility(View.VISIBLE);
        containerDisputes.removeAllViews();

        if (list == null || list.isEmpty()) {
            tvDisputesEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tvDisputesEmpty.setVisibility(View.GONE);

        for (SupplierDisputeEntity d : list) {
            View row = getLayoutInflater().inflate(R.layout.item_supplier_dispute, containerDisputes, false);
            bindDisputeRow(row, d);
            containerDisputes.addView(row);
        }
    }

    private void bindDisputeRow(View row, SupplierDisputeEntity d) {
        TextView tvTarget = row.findViewById(R.id.tvTarget);
        TextView tvStatus = row.findViewById(R.id.tvStatus);
        TextView tvReason = row.findViewById(R.id.tvReason);
        TextView tvDate   = row.findViewById(R.id.tvDisputeDate);

        tvTarget.setText("Target: " + d.getTargetEntity().name() + " #" + d.getTargetId());
        tvStatus.setText(d.getReviewStatus() != null ? d.getReviewStatus().name() : "OPEN");
        tvReason.setText(d.getReason() != null ? d.getReason() : "");
        tvDate.setText(shortDate.format(new Date(d.getSubmittedDate())));
    }

    private void openDisputeDialog(SupplierDisputeEntity.TargetEntity entity,
                                   long targetId,
                                   String targetLabel) {
        if (supplier == null) return;
        RaiseDisputeDialog d = RaiseDisputeDialog.newInstance(
                supplier.getId(), farmId, entity, targetId, targetLabel);
        d.setOnDisputeRaisedListener(id -> loadDisputes());
        d.show(getSupportFragmentManager(), "RaiseDisputeDialog");
    }

// ---------- Latest snapshot (read-only) ----------

    private void loadScoreSnapshot() {
        if (supplier == null) return;
        snapshotRepository.getLatestLive(supplier.getId())
                .observe(this, this::renderSnapshot);
    }

    private void renderSnapshot(SupplierScoreSnapshotEntity s) {
        cardScoreSnapshot.setVisibility(View.VISIBLE);

        if (s == null) {
            tvOverallScore.setText("—");
            tvConfidence.setText("No score yet");
            tvScoreBreakdown.removeAllViews();
            tvScoreMeta.setText("Scores are computed on the portal from validated evidence.");
            return;
        }

        if (s.getOverall() == null) {
            tvOverallScore.setText("—");
        } else {
            tvOverallScore.setText(String.format(Locale.getDefault(), "%.0f", s.getOverall()));
        }
        String confidence = s.getConfidence() != null
                ? titleCase(s.getConfidence().name()) : "—";
        int verifiedCount = s.getVerifiedTransactions();
        String confidenceLead = "Confidence: " + confidence;
        android.text.SpannableString confidenceText = new android.text.SpannableString(
                confidenceLead + " — scored from " + verifiedCount
                        + (verifiedCount == 1 ? " batch" : " batches")
                        + " with delivery and traceability evidence on file.");
        confidenceText.setSpan(new android.text.style.ForegroundColorSpan(0xFF0A5530),
                0, confidenceLead.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        confidenceText.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                0, confidenceLead.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvConfidence.setText(confidenceText);
        tvConfidence.setTextColor(0xFF59544C);
        tvConfidence.setTextSize(13.5f);
        tvConfidence.setAlpha(1f);
        tvConfidence.setMaxWidth(dp(230));
        tvOverallScore.setTextSize(40);
        tvScoreBreakdown.removeAllViews();
        addScoreDimension("Quality", "25%", s.getQuality());
        addScoreDimension("Delivery", "20%", s.getDelivery());
        addScoreDimension("Traceability", "20%", s.getTraceability());
        addScoreDimension("Compliance", "15%", s.getCompliance());
        addScoreDimension("Sustainability", "10%", s.getSustainability());
        addScoreDimension("Cooperation", "10%", s.getCooperation());

        String meta = "v" + (s.getScoringVersion() != null ? s.getScoringVersion() : "?")
                + " · " + shortDate.format(new Date(s.getCalculatedAt()))
                + " · " + s.getVerifiedTransactions() + " verified transactions";
        tvScoreMeta.setText(meta);
    }

    private String titleCase(String value) {
        String lower = value.toLowerCase(Locale.getDefault()).replace('_', ' ');
        return lower.isEmpty() ? lower : Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private void addScoreDimension(String name, String weight, Double score) {
        if (score == null) return;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(7), 0, dp(7));

        LinearLayout labels = new LinearLayout(this);
        labels.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText(name);
        title.setTextColor(0xFF3F3C37);
        title.setTextSize(13.5f);
        title.setTypeface(android.graphics.Typeface.create(
                androidx.core.content.res.ResourcesCompat.getFont(this, R.font.manrope_bold),
                android.graphics.Typeface.NORMAL));
        labels.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        TextView percent = new TextView(this);
        percent.setText(weight);
        percent.setTextColor(0xFF8A857B);
        percent.setTextSize(11.5f);
        LinearLayout.LayoutParams percentParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        percentParams.leftMargin = dp(8);
        percent.setLayoutParams(percentParams);
        labels.addView(percent);
        View spacer = new View(this);
        labels.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));
        TextView value = new TextView(this);
        value.setText(String.format(Locale.getDefault(), "%.0f", score));
        value.setTextColor(0xFF1C1B1A);
        value.setTextSize(13.5f);
        value.setTypeface(android.graphics.Typeface.create(
                androidx.core.content.res.ResourcesCompat.getFont(this, R.font.manrope_extrabold),
                android.graphics.Typeface.NORMAL));
        labels.addView(value);
        row.addView(labels, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout track = new LinearLayout(this);
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setBackground(makePillDrawable(0xFFF3EAD8));
        track.setClipToOutline(true);
        LinearLayout.LayoutParams trackParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(8));
        trackParams.topMargin = dp(5);
        View fill = new View(this);
        fill.setBackground(makePillDrawable(0xFF0E6B3C));
        View remainder = new View(this);
        track.addView(fill, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, Math.max(0, Math.min(100, score.floatValue()))));
        track.addView(remainder, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, 100 - Math.max(0, Math.min(100, score.floatValue()))));
        row.addView(track, trackParams);
        tvScoreBreakdown.addView(row);
    }

    private android.graphics.drawable.GradientDrawable makePillDrawable(int color) {
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(999));
        return drawable;
    }

    private void appendDim(StringBuilder b, String name, Double value) {
        if (value == null) return;
        b.append(name).append(": ")
                .append(String.format(Locale.getDefault(), "%.0f", value))
                .append("   ");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (commitmentRepository != null) commitmentRepository.shutdown();
        if (inspectionRepository != null) inspectionRepository.shutdown();
        if (disputeRepository != null)    disputeRepository.shutdown();
        if (snapshotRepository != null)   snapshotRepository.shutdown();
        if (supplierRepository != null)   supplierRepository.shutdown();
        if (qualityBatchRepository != null) qualityBatchRepository.shutdown();
        executor.shutdown();
    }

    public static class StatusChangeDialog extends com.afrinuts.farmos.ui.farm.FarmDialogFragment {
        private static final String ARG_STATUS = "selected_status";

        static StatusChangeDialog newInstance(int status) {
            StatusChangeDialog dialog = new StatusChangeDialog();
            Bundle args = new Bundle();
            args.putInt(ARG_STATUS, status);
            dialog.setArguments(args);
            return dialog;
        }

        @NonNull @Override
        public android.app.Dialog onCreateDialog(Bundle state) {
            android.content.Context context = requireContext();
            LinearLayout content = dialogContent(context, "Change Status");
            LinearLayout choices = new LinearLayout(context);
            choices.setOrientation(LinearLayout.VERTICAL);
            String[] labels = {"Active — Currently supplying", "Pending — Awaiting approval",
                    "Suspended — Temporarily suspended", "Inactive — No longer supplying"};
            int[] selected = {getArguments().getInt(ARG_STATUS, 0)};
            TextView[] markers = new TextView[labels.length];
            for (int i = 0; i < labels.length; i++) {
                final int index = i;
                LinearLayout row = new LinearLayout(context);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                TextView marker = new TextView(context);
                markers[i] = marker;
                marker.setTextSize(20);
                marker.setTextColor(0xFF0E6B3C);
                marker.setMinWidth(dp(context, 32));
                TextView label = new TextView(context);
                label.setText(labels[i]);
                label.setTextSize(14);
                label.setTextColor(0xFF59544C);
                row.addView(marker);
                row.addView(label);
                row.setMinimumHeight(dp(context, 48));
                row.setOnClickListener(v -> {
                    selected[0] = index;
                    for (int j = 0; j < markers.length; j++) {
                        if (markers[j] != null) markers[j].setText(j == index ? "●" : "○");
                    }
                });
                choices.addView(row);
            }
            for (int i = 0; i < markers.length; i++) markers[i].setText(i == selected[0] ? "●" : "○");
            content.addView(choices);
            LinearLayout buttons = dialogButtons(context, "Update status");
            content.addView(buttons);
            buttons.getChildAt(0).setOnClickListener(v -> dismiss());
            buttons.getChildAt(1).setOnClickListener(v -> {
                SupplierEntity.SupplierStatus[] statuses = SupplierEntity.SupplierStatus.values();
                if (selected[0] >= 0 && selected[0] < statuses.length
                        && getActivity() instanceof SupplierDetailActivity) {
                    ((SupplierDetailActivity) getActivity()).updateSupplierStatus(statuses[selected[0]]);
                }
                dismiss();
            });
            return new com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                    .setView(content).create();
        }
    }

    public static class DeleteSupplierDialog extends com.afrinuts.farmos.ui.farm.FarmDialogFragment {
        private static final String ARG_NAME = "supplier_name";

        static DeleteSupplierDialog newInstance(String name) {
            DeleteSupplierDialog dialog = new DeleteSupplierDialog();
            Bundle args = new Bundle();
            args.putString(ARG_NAME, name);
            dialog.setArguments(args);
            return dialog;
        }

        @NonNull @Override
        public android.app.Dialog onCreateDialog(Bundle state) {
            android.content.Context context = requireContext();
            LinearLayout content = dialogContent(context, "Delete Supplier");
            TextView message = new TextView(context);
            message.setText("Are you sure you want to delete "
                    + getArguments().getString(ARG_NAME, "this supplier")
                    + "? This action cannot be undone.");
            message.setTextSize(14);
            message.setTextColor(0xFF59544C);
            content.addView(message);
            LinearLayout buttons = dialogButtons(context, "Delete supplier");
            content.addView(buttons);
            buttons.getChildAt(0).setOnClickListener(v -> dismiss());
            buttons.getChildAt(1).setOnClickListener(v -> {
                if (getActivity() instanceof SupplierDetailActivity) {
                    ((SupplierDetailActivity) getActivity()).deleteSupplier();
                }
                dismiss();
            });
            return new com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                    .setView(content).create();
        }
    }

    private static LinearLayout dialogContent(android.content.Context context, String title) {
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        TextView heading = new TextView(context);
        heading.setText(title);
        heading.setTextSize(20);
        heading.setTextColor(0xFF1C1B1A);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(context, 14);
        content.addView(heading, params);
        return content;
    }

    private static LinearLayout dialogButtons(android.content.Context context, String primaryLabel) {
        LinearLayout buttons = new LinearLayout(context);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        cancelParams.setMargins(0, 0, dp(context, 8), 0);
        android.widget.Button cancel = new android.widget.Button(context);
        cancel.setText("Cancel");
        buttons.addView(cancel, cancelParams);
        android.widget.Button primary = new android.widget.Button(context);
        primary.setText(primaryLabel);
        buttons.addView(primary, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return buttons;
    }

    private static int dp(android.content.Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
