package com.afrinuts.farmos.ui.compliance;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.CertificationEntity;
import com.afrinuts.farmos.data.local.entity.VerificationEntity;
import com.afrinuts.farmos.data.repository.CertificationRepository;
import com.afrinuts.farmos.data.repository.VerificationRepository;
import com.afrinuts.farmos.ui.BaseActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VerificationDetailActivity extends BaseActivity {

    public static final String EXTRA_VERIFICATION_ID = "verification_id";

    private String verificationId;
    private VerificationEntity verification;
    private CertificationEntity certification;
    private VerificationRepository verificationRepository;
    private CertificationRepository certificationRepository;
    private AppDatabase database;
    private long farmId = 1;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements
    private TextView tvVerificationId;
    private TextView tvVerificationType;
    private TextView tvStatus;
    private TextView tvCertification;
    private TextView tvVerificationDate;
    private TextView tvVerifiedBy;
    private TextView tvEvidence;
    private TextView tvComment;
    private TextView tvCreatedAt;
    private TextView tvUpdatedAt;
    private CardView cardStatus;
    private TextView tvSuperseded;
    private MaterialButton btnCorrect;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Override
    protected boolean checkRoleAccess() {
        return !sessionManager.isInvestor();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification_detail);

        verificationId = getIntent().getStringExtra(EXTRA_VERIFICATION_ID);
        if (verificationId == null || verificationId.isEmpty()) {
            Toast.makeText(this, "Verification not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        database = AppDatabase.getInstance(this);
        initViews();
        setupToolbar();
        loadVerificationData();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvVerificationId = findViewById(R.id.tvVerificationId);
        tvVerificationType = findViewById(R.id.tvVerificationType);
        tvStatus = findViewById(R.id.tvStatus);
        tvCertification = findViewById(R.id.tvCertification);
        tvVerificationDate = findViewById(R.id.tvVerificationDate);
        tvVerifiedBy = findViewById(R.id.tvVerifiedBy);
        tvEvidence = findViewById(R.id.tvEvidence);
        tvComment = findViewById(R.id.tvComment);
        tvCreatedAt = findViewById(R.id.tvCreatedAt);
        tvUpdatedAt = findViewById(R.id.tvUpdatedAt);
        cardStatus = findViewById(R.id.cardStatus);

        loadSupersededState();

        btnCorrect = findViewById(R.id.btnCorrect);
        btnCorrect.setOnClickListener(v -> openCorrectionDialog());
        tvSuperseded = findViewById(R.id.tvSuperseded);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_backward);
            getSupportActionBar().setTitle("Verification Details");
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadVerificationData() {
        executor.execute(() -> {
            try {
                farmId = database.farmDao().getFirstFarm().getId();
                verificationRepository = new VerificationRepository(this, farmId);
                certificationRepository = new CertificationRepository(this, farmId);

                verification = verificationRepository.getVerificationByIdSync(verificationId);

                if (verification != null && verification.getCertificationId() != null) {
                    certification = certificationRepository.getCertificationByIdSync(verification.getCertificationId());
                }

                runOnUiThread(() -> {
                    if (verification != null) {
                        displayVerificationData();
                    } else {
                        Toast.makeText(this, "Verification not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading verification", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }
    private void loadSupersededState() {
        executor.execute(() -> {
            String supersedingId = database.verificationDao()
                    .getSupersedingRecordId(verification.getVerificationId());
            runOnUiThread(() -> {
                if (supersedingId != null) {
                    tvSuperseded.setText("Superseded by " + supersedingId);
                    tvSuperseded.setVisibility(View.VISIBLE);
                } else {
                    tvSuperseded.setVisibility(View.GONE);
                }
            });
        });
    }
    private void displayVerificationData() {
        tvVerificationId.setText(verification.getVerificationId());
        tvVerificationType.setText(verification.getVerificationTypeDisplay());

        tvStatus.setText(verification.getStatusDisplay());
        updateStatusColor();

        if (certification != null) {
            tvCertification.setText(certification.getCertificationName() + " (" + certification.getCertificationId() + ")");
        } else {
            tvCertification.setText(verification.getCertificationId());
        }

        tvVerificationDate.setText(verification.getFormattedVerificationDate());
        tvVerifiedBy.setText(verification.getVerifiedBy() != null ? verification.getVerifiedBy() : "Not specified");

        // Evidence
        List<String> evidence = verification.getEvidence();
        if (evidence != null && !evidence.isEmpty()) {
            tvEvidence.setText(String.join("\n", evidence));
            tvEvidence.setVisibility(View.VISIBLE);
        } else {
            tvEvidence.setVisibility(View.GONE);
        }

        tvComment.setText(verification.getComment() != null ? verification.getComment() : "No comments");

        tvCreatedAt.setText(dateFormat.format(verification.getCreatedAt()));
        tvUpdatedAt.setText(dateFormat.format(verification.getUpdatedAt()));
    }

    private void updateStatusColor() {
        int colorRes;
        switch (verification.getStatus()) {
            case VERIFIED:
                colorRes = R.color.status_certified;
                break;
            case PENDING:
                colorRes = R.color.status_audit_ready;
                break;
            case REJECTED:
                colorRes = R.color.status_expired;
                break;
            default:
                colorRes = R.color.status_not_started;
                break;
        }
        cardStatus.setCardBackgroundColor(getColor(colorRes));
    }

    private void openCorrectionDialog() {
        AddVerificationDialog correction = AddVerificationDialog.newInstanceCorrection(farmId, verification.getVerificationId());
        correction.setOnVerificationAddedListener(this::loadVerificationData);
        correction.show(getSupportFragmentManager(), "AddVerificationCorrectionDialog");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (verificationRepository != null) {
            verificationRepository.shutdown();
        }
        if (certificationRepository != null) {
            certificationRepository.shutdown();
        }
        executor.shutdown();
    }
}