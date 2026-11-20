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
    private TextView tvCertification;
    private TextView tvIssueDate;
    private TextView tvExpiryDate;
    private TextView tvFileUrl;
    private TextView tvCreatedAt;
    private TextView tvUpdatedAt;
    private CardView cardStatus;

    private MaterialButton btnEdit;
    private MaterialButton btnDelete;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

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
        setupToolbar();
        loadDocumentData();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvDocumentId = findViewById(R.id.tvDocumentId);
        tvDocumentType = findViewById(R.id.tvDocumentType);
        tvDocumentName = findViewById(R.id.tvDocumentName);
        tvStatus = findViewById(R.id.tvStatus);
        tvCertification = findViewById(R.id.tvCertification);
        tvIssueDate = findViewById(R.id.tvIssueDate);
        tvExpiryDate = findViewById(R.id.tvExpiryDate);
        tvFileUrl = findViewById(R.id.tvFileUrl);
        tvCreatedAt = findViewById(R.id.tvCreatedAt);
        tvUpdatedAt = findViewById(R.id.tvUpdatedAt);
        cardStatus = findViewById(R.id.cardStatus);

        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);

        btnEdit.setOnClickListener(v -> showEditDialog());
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_backward);
            getSupportActionBar().setTitle("Document Details");
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

        tvStatus.setText(document.getStatusDisplay());
        updateStatusColor();

        if (certification != null) {
            tvCertification.setText(certification.getCertificationName() + " (" + certification.getCertificationId() + ")");
        } else {
            tvCertification.setText(document.getCertificationId());
        }

        tvIssueDate.setText(document.getFormattedIssueDate());
        tvExpiryDate.setText(document.getFormattedExpiryDate());

        if (document.getFileUrl() != null && !document.getFileUrl().isEmpty()) {
            tvFileUrl.setText(document.getFileUrl());
            tvFileUrl.setVisibility(View.VISIBLE);
        } else {
            tvFileUrl.setVisibility(View.GONE);
        }

        tvCreatedAt.setText(dateFormat.format(document.getCreatedAt()));
        tvUpdatedAt.setText(dateFormat.format(document.getUpdatedAt()));

        if (!sessionManager.canEditData()) {
            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
        }
    }

    private void updateStatusColor() {
        int colorRes;
        switch (document.getStatus()) {
            case APPROVED:
                colorRes = R.color.status_certified;
                break;
            case PENDING:
                colorRes = R.color.status_audit_ready;
                break;
            case DRAFT:
                colorRes = R.color.status_not_started;
                break;
            default:
                colorRes = R.color.status_not_started;
                break;
        }
        cardStatus.setCardBackgroundColor(getColor(colorRes));
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