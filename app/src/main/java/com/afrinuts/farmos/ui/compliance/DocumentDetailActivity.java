package com.afrinuts.farmos.ui.compliance;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.res.ResourcesCompat;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.CertificationEntity;
import com.afrinuts.farmos.data.local.entity.DocumentEntity;
import com.afrinuts.farmos.data.repository.CertificationRepository;
import com.afrinuts.farmos.data.repository.DocumentRepository;
import com.afrinuts.farmos.ui.BaseActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DocumentDetailActivity extends BaseActivity {

    public static final String EXTRA_DOCUMENT_ID = "document_id";

    private String documentId;
    private DocumentEntity document;
    private CertificationEntity certification;
    private DocumentRepository documentRepository;
    private CertificationRepository certificationRepository;
    private AppDatabase database;
    private long farmId = 1;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements
    private TextView tvDocumentId;
    private TextView tvDocumentType;
    private TextView tvDocumentName;
    private TextView tvStatus;
    private TextView tvIssueStat;
    private TextView tvExpiryStat;
    private LinearLayout detailsRows;
    private ImageButton btnEdit;
    private ImageButton btnDelete;
    private MaterialButton btnOpenFile;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.US);

    @Override
    protected boolean checkRoleAccess() {
        return com.afrinuts.farmos.utils.AccessMap.canOpenScreen(
                getClass().getSimpleName(), sessionManager.getUserRole());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_detail);
        com.afrinuts.farmos.ui.TabletPane.attachList(this, savedInstanceState, new com.afrinuts.farmos.ui.compliance.DocumentListFragment());

        documentId = getIntent().getStringExtra(EXTRA_DOCUMENT_ID);
        if (documentId == null || documentId.isEmpty()) {
            Toast.makeText(this, "Document not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        database = AppDatabase.getInstance(this);
        initViews();
        loadDocumentData();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        tvDocumentId = findViewById(R.id.tvDocumentId);
        tvDocumentType = findViewById(R.id.tvDocumentType);
        tvDocumentName = findViewById(R.id.tvDocumentName);
        tvStatus = findViewById(R.id.tvStatus);
        tvIssueStat = findViewById(R.id.tvIssueStat);
        tvExpiryStat = findViewById(R.id.tvExpiryStat);
        detailsRows = findViewById(R.id.detailsRows);

        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        btnOpenFile = findViewById(R.id.btnOpenFile);

        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        btnEdit.setOnClickListener(v -> showEditDialog());
        btnDelete.setOnClickListener(v -> confirmDelete());
        btnOpenFile.setOnClickListener(v -> openDocumentFile());
    }

    private void loadDocumentData() {
        executor.execute(() -> {
            try {
                farmId = database.farmDao().getFirstFarm().getId();
                documentRepository = new DocumentRepository(this, farmId);
                certificationRepository = new CertificationRepository(this, farmId);

                document = documentRepository.getDocumentByIdSync(documentId);

                if (document != null && document.getCertificationId() != null) {
                    certification = certificationRepository.getCertificationByIdSync(document.getCertificationId());
                }

                runOnUiThread(() -> {
                    if (document != null) {
                        displayDocumentData();
                    } else {
                        Toast.makeText(this, "Document not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading document", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void displayDocumentData() {
        tvDocumentId.setText(document.getDocumentId());
        tvDocumentType.setText(document.getDocumentTypeDisplay());
        tvDocumentName.setText(document.getDocumentName());

        String statusLabel = getStatusLabel();
        tvStatus.setText(statusLabel);
        setPillStyle(tvStatus, getStatusBackgroundColor(), getStatusForegroundColor());

        tvIssueStat.setText(formatStatDate(document.getIssueDate()));
        tvExpiryStat.setText(formatStatDate(document.getExpiryDate()));
        populateDetailsRows(statusLabel);

        boolean hasFile = document.getFileUrl() != null && !document.getFileUrl().trim().isEmpty();
        btnOpenFile.setEnabled(hasFile);
        btnOpenFile.setAlpha(hasFile ? 1f : 0.45f);

        if (!sessionManager.canEditData()) {
            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
        }
    }

    private void populateDetailsRows(String statusLabel) {
        detailsRows.removeAllViews();
        String certificationName = certification != null
                ? certification.getCertificationName() + " (" + certification.getCertificationId() + ")"
                : (document.getCertificationId() == null || document.getCertificationId().trim().isEmpty()
                ? "—" : document.getCertificationId());
        String fileUrl = document.getFileUrl() == null || document.getFileUrl().trim().isEmpty()
                ? "—" : document.getFileUrl().trim();

        String[] labels = {"Certification\nprogramme", "Document type", "Document name",
                "Issue date", "Expiry date", "File URL", "Status", "Created", "Updated"};
        String[] values = {certificationName, document.getDocumentTypeDisplay(), document.getDocumentName(),
                document.getFormattedIssueDate(), document.getFormattedExpiryDate(), fileUrl, statusLabel,
                formatMetadataDate(document.getCreatedAt()), formatMetadataDate(document.getUpdatedAt())};

        for (int i = 0; i < labels.length; i++) {
            boolean metadata = i >= 7;
            boolean isFileUrl = i == 5 && !"—".equals(fileUrl);
            View row = createDetailRow(labels[i], values[i], metadata, isFileUrl);
            detailsRows.addView(row);
            if (i < labels.length - 1) {
                View divider = new View(this);
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
                divider.setBackgroundColor(Color.parseColor("#E9E1CF"));
                detailsRows.addView(divider);
            }
        }
    }

    private View createDetailRow(String label, String valueText, boolean metadata, boolean fileLink) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(11), 0, dp(11));
        row.setClickable(fileLink);
        row.setFocusable(fileLink);
        if (fileLink) {
            row.setOnClickListener(v -> openDocumentFile());
        }

        TextView labelView = new TextView(this);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        labelView.setLayoutParams(labelParams);
        labelView.setText(label);
        labelView.setTextSize(13);
        labelView.setTextColor(Color.parseColor(metadata ? "#8A857B" : "#59544C"));
        labelView.setTypeface(ResourcesCompat.getFont(this, R.font.manrope_bold));
        row.addView(labelView);

        TextView valueView = new TextView(this);
        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        valueView.setLayoutParams(valueParams);
        valueView.setText(valueText);
        valueView.setGravity(Gravity.END);
        valueView.setTextSize(metadata ? 13.5f : 14.5f);
        valueView.setTextColor(Color.parseColor(fileLink ? "#0A5530" : metadata ? "#59544C" : "#1C1B1A"));
        int valueFont = metadata ? R.font.manrope_semibold
                : fileLink ? R.font.manrope_extrabold : R.font.manrope_bold;
        valueView.setTypeface(ResourcesCompat.getFont(this, valueFont));
        row.addView(valueView);
        return row;
    }

    private String getStatusLabel() {
        if (document.getStatus() == DocumentEntity.DocumentStatus.APPROVED) {
            if (document.isExpired()) return "Expired";
            if (document.isExpiringSoon()) return "Expiring soon";
            return "Valid";
        }
        return document.getStatusDisplay();
    }

    private int getStatusBackgroundColor() {
        if (document.getStatus() == DocumentEntity.DocumentStatus.APPROVED) {
            if (document.isExpired()) return Color.parseColor("#F8E4DD");
            if (document.isExpiringSoon()) return Color.parseColor("#F6ECD2");
            return Color.parseColor("#E3F0E7");
        }
        return Color.parseColor("#EFEAE0");
    }

    private int getStatusForegroundColor() {
        if (document.getStatus() == DocumentEntity.DocumentStatus.APPROVED) {
            if (document.isExpired()) return Color.parseColor("#B3402A");
            if (document.isExpiringSoon()) return Color.parseColor("#8A6414");
            return Color.parseColor("#0A5530");
        }
        return Color.parseColor("#6B675F");
    }

    private void setPillStyle(TextView view, int backgroundColor, int textColor) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(backgroundColor);
        background.setCornerRadius(dp(999));
        view.setBackground(background);
        view.setTextColor(textColor);
    }

    private CharSequence formatStatDate(long timestamp) {
        if (timestamp <= 0) return "—";
        String value = new SimpleDateFormat("MMM d yyyy", Locale.US).format(timestamp);
        SpannableString styled = new SpannableString(value);
        int yearStart = value.lastIndexOf(' ') + 1;
        if (yearStart > 0) {
            styled.setSpan(new RelativeSizeSpan(0.65f), yearStart, value.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return styled;
    }

    private String formatMetadataDate(long timestamp) {
        return timestamp <= 0 ? "—" : dateFormat.format(timestamp);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void openDocumentFile() {
        if (document == null || document.getFileUrl() == null || document.getFileUrl().trim().isEmpty()) {
            Toast.makeText(this, "No file is attached", Toast.LENGTH_SHORT).show();
            return;
        }
        String url = document.getFileUrl().trim();
        if (!url.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*")) {
            url = "https://" + url;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No app can open this file", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open file", Toast.LENGTH_SHORT).show();
        }
    }

    private void showEditDialog() {
        if (document == null) return;

        EditDocumentDialog dialog = EditDocumentDialog.newInstance(document.getDocumentId());
        dialog.setOnDocumentUpdatedListener(() -> {
            loadDocumentData();
            Toast.makeText(this, "Document updated!", Toast.LENGTH_SHORT).show();
        });
        dialog.show(getSupportFragmentManager(), "EditDocumentDialog");
    }

    private void confirmDelete() {
        if (!sessionManager.isOwner()) {
            Snackbar.make(findViewById(android.R.id.content),
                    "Only owners can delete documents", Snackbar.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Document")
                .setMessage("Are you sure you want to delete this document?")
                .setPositiveButton("Delete", (dialog, which) -> deleteDocument())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteDocument() {
        Toast.makeText(this, "Deleting document...", Toast.LENGTH_SHORT).show();

        documentRepository.deleteDocument(document, new DocumentRepository.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(() -> {
                    Toast.makeText(DocumentDetailActivity.this,
                            "Document deleted", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(DocumentDetailActivity.this,
                                "Error: " + error, Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (documentRepository != null) {
            documentRepository.shutdown();
        }
        if (certificationRepository != null) {
            certificationRepository.shutdown();
        }
        executor.shutdown();
    }
}
