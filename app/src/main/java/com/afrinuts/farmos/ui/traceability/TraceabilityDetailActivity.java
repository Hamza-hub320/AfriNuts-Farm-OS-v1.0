package com.afrinuts.farmos.ui.traceability;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;
import com.afrinuts.farmos.data.repository.BatchTraceabilityRepository;
import com.afrinuts.farmos.data.repository.QualityBatchRepository;
import com.afrinuts.farmos.data.repository.SupplierRepository;
import com.afrinuts.farmos.ui.BaseActivity;
import com.afrinuts.farmos.ui.quality.QualityBatchDetailActivity;
import com.afrinuts.farmos.ui.suppliers.SupplierDetailActivity;
import com.afrinuts.farmos.utils.DocumentGenerator;
import com.afrinuts.farmos.utils.QRCodeGenerator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TraceabilityDetailActivity extends BaseActivity {

    public static final String EXTRA_TRACEABILITY_ID = "traceability_id";
    public static final String EXTRA_BATCH_NUMBER = "batch_number";

    private long traceabilityId;
    private String batchNumber;
    private BatchTraceabilityEntity traceability;
    private QualityBatchEntity qualityBatch;
    private SupplierEntity supplier;

    private AppDatabase database;
    private BatchTraceabilityRepository repository;
    private QualityBatchRepository qualityBatchRepository;
    private SupplierRepository supplierRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private long farmId = 1;
    private static final String TAG = "TraceabilityDetail";

    // UI Elements
    private TextView tvBatchNumber;
    private TextView tvStatus;
    private TextView tvGrade;
    private TextView tvProcessingDate;
    private TextView tvExportDate;
    private TextView tvWeight;
    private TextView tvBuyer;
    private TextView tvDestination;
    private TextView tvSupplierName;
    private TextView tvBlockName;
    private TextView tvQualityGrade;
    private TextView tvQrUrl;
    private ImageView ivQrCode;

    private Button btnSetExportDate;
    private Button btnEditDestination;
    private Button btnUploadDocuments;
    private View layoutShipping;
    private TextView tvExportStatus;
    private TextView tvVerificationCode;
    private TextView tvShippingExportDate;
    private TextView tvShippingDestination;
    private TextView tvBuyerShipping;
    private TextView tvCertificationsShipping;
    private TextView tvHarvestYearShipping;
    private LinearLayout layoutAdditionalDocs;



    // Sustainability
    private TextView tvNoChildLabor;
    private TextView tvFarmOsMonitored;
    private TextView tvFirebreakProtection;
    private TextView tvSustainableHarvesting;
    private TextView tvVerifiedDate;
    private CardView layoutSustainability;

    // Documents
    private CardView layoutDocuments;
    private TextView tvQualityCertificate;
    private TextView tvPhytoSanitary;
    private TextView tvBillOfLading;
    private MaterialButton btnGenerateDocuments;
    private LinearLayout layoutCommercialInvoice;
    private TextView tvCommercialInvoice;
    private TextView tvInsuranceCertificate;

    // Buttons
    private MaterialButton btnViewBatch;
    private MaterialButton btnViewSupplier;
    private MaterialButton btnShareTraceability;
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
        setContentView(R.layout.activity_traceability_detail);

        traceabilityId = getIntent().getLongExtra(EXTRA_TRACEABILITY_ID, -1);
        batchNumber = getIntent().getStringExtra(EXTRA_BATCH_NUMBER);

        if (traceabilityId == -1 && (batchNumber == null || batchNumber.isEmpty())) {
            Toast.makeText(this, "Traceability record not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        database = AppDatabase.getInstance(this);
        repository = new BatchTraceabilityRepository(this, 1);

        initViews();
        setupToolbar();
        loadData();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Header
        tvBatchNumber = findViewById(R.id.tvBatchNumber);
        tvStatus = findViewById(R.id.tvStatus);
        tvGrade = findViewById(R.id.tvGrade);

        // Details
        tvProcessingDate = findViewById(R.id.tvProcessingDate);
        tvExportDate = findViewById(R.id.tvExportDate);
        tvWeight = findViewById(R.id.tvWeight);
        tvBuyer = findViewById(R.id.tvBuyer);
        tvDestination = findViewById(R.id.tvDestination);
        tvSupplierName = findViewById(R.id.tvSupplierName);
        tvBlockName = findViewById(R.id.tvBlockName);
        tvQualityGrade = findViewById(R.id.tvQualityGrade);

        // QR Code
        tvQrUrl = findViewById(R.id.tvQrUrl);
        ivQrCode = findViewById(R.id.ivQrCode);
        tvVerificationCode = findViewById(R.id.tvVerificationCode);

        tvBuyerShipping = findViewById(R.id.tvBuyerShipping);
        tvCertificationsShipping = findViewById(R.id.tvCertificationsShipping);
        tvHarvestYearShipping = findViewById(R.id.tvHarvestYearShipping);

        // Sustainability
        layoutSustainability = findViewById(R.id.layoutSustainability);
        tvNoChildLabor = findViewById(R.id.tvNoChildLabor);
        tvFarmOsMonitored = findViewById(R.id.tvFarmOsMonitored);
        tvFirebreakProtection = findViewById(R.id.tvFirebreakProtection);
        tvSustainableHarvesting = findViewById(R.id.tvSustainableHarvesting);
        tvVerifiedDate = findViewById(R.id.tvVerifiedDate);

        layoutShipping = findViewById(R.id.layoutShipping);
        tvExportStatus = findViewById(R.id.tvExportStatus);
        btnSetExportDate = findViewById(R.id.btnSetExportDate);
        btnEditDestination = findViewById(R.id.btnEditDestination);
        btnUploadDocuments = findViewById(R.id.btnUploadDocuments);

        // Documents
        layoutDocuments = findViewById(R.id.layoutDocuments);
        tvQualityCertificate = findViewById(R.id.tvQualityCertificate);
        tvPhytoSanitary = findViewById(R.id.tvPhytoSanitary);
        tvBillOfLading = findViewById(R.id.tvBillOfLading);
        layoutAdditionalDocs = findViewById(R.id.layoutAdditionalDocs);
        layoutAdditionalDocs = findViewById(R.id.layoutAdditionalDocs);
        layoutCommercialInvoice = findViewById(R.id.layoutCommercialInvoice);
        tvCommercialInvoice = findViewById(R.id.tvCommercialInvoice);
        tvInsuranceCertificate = findViewById(R.id.tvInsuranceCertificate);

        // Buttons
        btnViewBatch = findViewById(R.id.btnViewBatch);
        btnViewSupplier = findViewById(R.id.btnViewSupplier);
        btnShareTraceability = findViewById(R.id.btnShareTraceability);
        btnDelete = findViewById(R.id.btnDelete);
        btnGenerateDocuments = findViewById(R.id.btnGenerateDocuments);
        btnGenerateDocuments.setOnClickListener(v -> showDocumentGenerationDialog());

        btnViewBatch.setOnClickListener(v -> viewBatch());
        btnViewSupplier.setOnClickListener(v -> viewSupplier());
        btnShareTraceability.setOnClickListener(v -> shareTraceability());
        btnDelete.setOnClickListener(v -> confirmDelete());

        btnSetExportDate.setOnClickListener(v -> showEditShippingDialog());
        btnEditDestination.setOnClickListener(v -> showEditShippingDialog());
        btnUploadDocuments.setOnClickListener(v -> showUploadDocumentsDialog());

        tvShippingExportDate = findViewById(R.id.tvShippingExportDate);
        tvShippingDestination = findViewById(R.id.tvShippingDestination);
        btnSetExportDate = findViewById(R.id.btnSetExportDate);
        btnEditDestination = findViewById(R.id.btnEditDestination);
        btnUploadDocuments = findViewById(R.id.btnUploadDocuments);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Traceability Details");
        }
    }

    private void showEditShippingDialog() {
        EditShippingDialog dialog = EditShippingDialog.newInstance(traceability.getId());
        dialog.setOnShippingUpdatedListener(() -> {
            // Reload data
            loadData();
            Toast.makeText(this, "Shipping details updated!", Toast.LENGTH_SHORT).show();
        });
        dialog.show(getSupportFragmentManager(), "EditShippingDialog");
    }

    private void showUploadDocumentsDialog() {
        UploadDocumentDialog dialog = UploadDocumentDialog.newInstance(traceability.getId());
        dialog.setOnDocumentUploadedListener(() -> {
            // Reload data
            loadData();
            Toast.makeText(this, "Document uploaded successfully!", Toast.LENGTH_SHORT).show();
        });
        dialog.show(getSupportFragmentManager(), "UploadDocumentDialog");
    }

    private void displayShippingData() {
        if (traceability == null) return;

        layoutShipping.setVisibility(View.VISIBLE);

        // Export Date
        if (traceability.getExportDate() != null && traceability.getExportDate() > 0) {
            tvShippingExportDate.setText(traceability.getFormattedExportDate());
            tvExportStatus.setText("Exported");
            tvExportStatus.setVisibility(View.VISIBLE);
        } else {
            tvShippingExportDate.setText("Not set");
            tvExportStatus.setText("Pending");
            tvExportStatus.setVisibility(View.VISIBLE);
        }

        // ✅ Buyer
        if (traceability.getBuyer() != null && !traceability.getBuyer().isEmpty()) {
            tvBuyerShipping.setText(traceability.getBuyer());
        } else {
            tvBuyerShipping.setText("Not set");
        }

        // Destination
        if (traceability.getDestination() != null && !traceability.getDestination().isEmpty()) {
            tvShippingDestination.setText(traceability.getDestination());
        } else {
            tvShippingDestination.setText("Not set");
        }

        // ✅ Certifications
        if (traceability.getCertificationIds() != null && !traceability.getCertificationIds().isEmpty()) {
            tvCertificationsShipping.setText(traceability.getCertificationIds());
        } else {
            tvCertificationsShipping.setText("None");
        }

        // ✅ Harvest Year
        if (traceability.getHarvestYear() > 0) {
            tvHarvestYearShipping.setText(String.valueOf(traceability.getHarvestYear()));
        } else {
            tvHarvestYearShipping.setText("Not set");
        }

        // Documents
        if (traceability.hasDocuments()) {
            BatchTraceabilityEntity.Documents docs = traceability.getDocumentsObject();
            int docCount = 0;
            if (docs.getQualityCertificate() != null) docCount++;
            if (docs.getPhytoSanitary() != null) docCount++;
            if (docs.getBillOfLading() != null) docCount++;
            if (docs.getAdditionalDocs() != null) docCount += docs.getAdditionalDocs().size();
            btnUploadDocuments.setText(docCount + " Document" + (docCount > 1 ? "s" : ""));
        } else {
            btnUploadDocuments.setText("Upload Documents");
        }

        // Show edit buttons
        if (sessionManager.canEditData()) {
            btnSetExportDate.setVisibility(View.VISIBLE);
            btnEditDestination.setVisibility(View.VISIBLE);
            btnUploadDocuments.setVisibility(View.VISIBLE);
        } else {
            btnSetExportDate.setVisibility(View.GONE);
            btnEditDestination.setVisibility(View.GONE);
            btnUploadDocuments.setVisibility(View.GONE);
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

    private void loadData() {
        executor.execute(() -> {
            if (traceabilityId != -1) {
                traceability = repository.getTraceabilityByIdSync(traceabilityId);
            } else if (batchNumber != null) {
                traceability = repository.getByBatchNumber(batchNumber);
            }

            if (traceability == null) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Traceability record not found", Toast.LENGTH_SHORT).show();
                    finish();
                });
                return;
            }

            if (traceability.getQualityBatchId() != null) {
                qualityBatchRepository = new QualityBatchRepository(this, traceability.getFarmId());
                qualityBatch = qualityBatchRepository.getBatchByIdSync(traceability.getQualityBatchId());
            }

            if (traceability.getSupplierId() != null) {
                supplierRepository = new SupplierRepository(this, traceability.getFarmId());
                supplier = supplierRepository.getSupplierByIdSync(traceability.getSupplierId());
            }

            runOnUiThread(() -> displayTraceabilityData());
        });
    }

    private void displayTraceabilityData() {
        if (traceability == null) return;

        tvBatchNumber.setText("#" + traceability.getBatchNumber());
        tvStatus.setText(traceability.isExported() ? "Exported" : "Processing");
        tvGrade.setText(traceability.getGrade());

        tvProcessingDate.setText(traceability.getFormattedProcessingDate());
        tvExportDate.setText(traceability.getFormattedExportDate());
        tvWeight.setText(traceability.getFormattedWeight());
        displayProcessingDetails();

        tvBuyer.setText(traceability.getBuyer() != null ? traceability.getBuyer() : "—");
        tvDestination.setText(traceability.getDestination() != null ? traceability.getDestination() : "—");
        tvSupplierName.setText(traceability.getSupplierName() != null ? traceability.getSupplierName() : "—");
        tvQualityGrade.setText(traceability.getQualityGrade() != null ? traceability.getQualityGrade() : "—");

        // ✅ Processing Details
        TextView tvRawWeight = findViewById(R.id.tvRawWeight);
        TextView tvKernelWeight = findViewById(R.id.tvKernelWeight);
        TextView tvYieldPercentage = findViewById(R.id.tvYieldPercentage);
        TextView tvProcessingMethod = findViewById(R.id.tvProcessingMethod);
        TextView tvProcessingFacility = findViewById(R.id.tvProcessingFacility);

        if (traceability.getRawWeightKg() > 0) {
            tvRawWeight.setText(String.format(Locale.getDefault(), "%,.0f kg", traceability.getRawWeightKg()));
            tvRawWeight.setVisibility(View.VISIBLE);
        } else {
            tvRawWeight.setVisibility(View.GONE);
        }

        if (traceability.getKernelWeightKg() > 0) {
            tvKernelWeight.setText(String.format(Locale.getDefault(), "%,.0f kg", traceability.getKernelWeightKg()));
            tvKernelWeight.setVisibility(View.VISIBLE);
        } else {
            tvKernelWeight.setVisibility(View.GONE);
        }

        if (traceability.getYieldPercentage() > 0) {
            tvYieldPercentage.setText(String.format(Locale.getDefault(), "%.0f%%", traceability.getYieldPercentage()));
            tvYieldPercentage.setVisibility(View.VISIBLE);
        } else {
            tvYieldPercentage.setVisibility(View.GONE);
        }

        if (traceability.getProcessingMethod() != null && !traceability.getProcessingMethod().isEmpty()) {
            tvProcessingMethod.setText(traceability.getProcessingMethod());
            tvProcessingMethod.setVisibility(View.VISIBLE);
        } else {
            tvProcessingMethod.setVisibility(View.GONE);
        }

        if (traceability.getProcessingFacility() != null && !traceability.getProcessingFacility().isEmpty()) {
            tvProcessingFacility.setText(traceability.getProcessingFacility());
            tvProcessingFacility.setVisibility(View.VISIBLE);
        } else {
            tvProcessingFacility.setVisibility(View.GONE);
        }

        displayShippingData();
        displaySustainability();
        displayDocuments();
        displayDigitalSignature();

        String qrUrl = traceability.getQrCodeUrl();
        tvQrUrl.setText(qrUrl);
        generateQRCode(qrUrl);

        // View buttons
        if (qualityBatch != null) {
            btnViewBatch.setVisibility(View.VISIBLE);
        } else {
            btnViewBatch.setVisibility(View.GONE);
        }

        if (supplier != null) {
            btnViewSupplier.setVisibility(View.VISIBLE);
        } else {
            btnViewSupplier.setVisibility(View.GONE);
        }

        // Role-based UI
        if (!sessionManager.canEditData()) {
            btnDelete.setVisibility(View.GONE);
        }
    }

    // ✅ Add this new method for Digital Signature display
    private void displayDigitalSignature() {
        View layoutSignature = findViewById(R.id.layoutSignature);
        TextView tvVerificationStatus = findViewById(R.id.tvVerificationStatus);
        TextView tvVerificationCode = findViewById(R.id.tvVerificationCode);
        TextView tvDigitalSignature = findViewById(R.id.tvDigitalSignature);
        TextView tvSignedDate = findViewById(R.id.tvSignedDate);
        ImageView ivCopyCode = findViewById(R.id.ivCopyCode);


        if (traceability != null && traceability.isVerified()) {
            layoutSignature.setVisibility(View.VISIBLE);

            // Status
            tvVerificationStatus.setText("Verified");
            tvVerificationStatus.setTextColor(getColor(android.R.color.holo_green_light));

            // Verification Code
            String code = traceability.getVerificationCode();
            if (code != null && !code.isEmpty()) {
                tvVerificationCode.setText(code);
                tvVerificationCode.setVisibility(View.VISIBLE);
            } else {
                tvVerificationCode.setText("—");
            }

            // Digital Signature (show shortened version)
            String signature = traceability.getDigitalSignature();
            if (signature != null && !signature.isEmpty()) {
                // Show full signature in a smaller font
                tvDigitalSignature.setText(signature);
                tvDigitalSignature.setVisibility(View.VISIBLE);
            } else {
                tvDigitalSignature.setVisibility(View.GONE);
            }

            // Signed Date
            tvSignedDate.setText("Signed: " + traceability.getFormattedSignedDate());

            // Copy verification code
            if (ivCopyCode != null) {
                ivCopyCode.setOnClickListener(v -> {
                    if (traceability.getVerificationCode() != null) {
                        android.content.ClipboardManager clipboard =
                                (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        android.content.ClipData clip = android.content.ClipData.newPlainText(
                                "Verification Code", traceability.getVerificationCode());
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(this, "Verification code copied!", Toast.LENGTH_SHORT).show();
                    }
                });
            }


            // Make verification code clickable to copy
            tvVerificationCode.setOnClickListener(v -> {
                if (traceability.getVerificationCode() != null) {
                    android.content.ClipboardManager clipboard =
                            (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText(
                            "Verification Code", traceability.getVerificationCode());
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Verification code copied!", Toast.LENGTH_SHORT).show();
                }
            });

        } else {
            layoutSignature.setVisibility(View.GONE);
        }
    }

    private void displayProcessingDetails() {
        View layoutProcessingDetails = findViewById(R.id.layoutProcessingDetails);
        TextView tvRawWeight = findViewById(R.id.tvRawWeight);
        TextView tvKernelWeight = findViewById(R.id.tvKernelWeight);
        TextView tvYieldPercentage = findViewById(R.id.tvYieldPercentage);
        TextView tvProcessingMethod = findViewById(R.id.tvProcessingMethod);
        TextView tvProcessingFacility = findViewById(R.id.tvProcessingFacility);
        TextView tvProcessingLocation = findViewById(R.id.tvProcessingLocation);

        // Check if we have any processing data to show
        boolean hasData = traceability.getRawWeightKg() > 0 ||
                traceability.getKernelWeightKg() > 0 ||
                traceability.getProcessingMethod() != null ||
                traceability.getProcessingFacility() != null;

        if (hasData) {
            layoutProcessingDetails.setVisibility(View.VISIBLE);

            // Raw Weight
            if (traceability.getRawWeightKg() > 0) {
                tvRawWeight.setText(String.format(Locale.getDefault(), "%,.0f kg", traceability.getRawWeightKg()));
                tvRawWeight.setVisibility(View.VISIBLE);
            } else {
                tvRawWeight.setVisibility(View.GONE);
            }

            // Kernel Weight
            if (traceability.getKernelWeightKg() > 0) {
                tvKernelWeight.setText(String.format(Locale.getDefault(), "%,.0f kg", traceability.getKernelWeightKg()));
                tvKernelWeight.setVisibility(View.VISIBLE);
            } else {
                tvKernelWeight.setVisibility(View.GONE);
            }

            // Yield Percentage
            if (traceability.getYieldPercentage() > 0) {
                tvYieldPercentage.setText(String.format(Locale.getDefault(), "%.1f%%", traceability.getYieldPercentage()));
                tvYieldPercentage.setVisibility(View.VISIBLE);
            } else if (traceability.getRawWeightKg() > 0 && traceability.getKernelWeightKg() > 0) {
                // Calculate on the fly
                double yield = (traceability.getKernelWeightKg() / traceability.getRawWeightKg()) * 100;
                tvYieldPercentage.setText(String.format(Locale.getDefault(), "%.1f%%", yield));
                tvYieldPercentage.setVisibility(View.VISIBLE);
            } else {
                tvYieldPercentage.setVisibility(View.GONE);
            }

            // Processing Method
            if (traceability.getProcessingMethod() != null && !traceability.getProcessingMethod().isEmpty()) {
                tvProcessingMethod.setText(traceability.getProcessingMethod());
                tvProcessingMethod.setVisibility(View.VISIBLE);
            } else {
                tvProcessingMethod.setVisibility(View.GONE);
            }

            // Processing Facility
            if (traceability.getProcessingFacility() != null && !traceability.getProcessingFacility().isEmpty()) {
                tvProcessingFacility.setText(traceability.getProcessingFacility());
                tvProcessingFacility.setVisibility(View.VISIBLE);
            } else {
                tvProcessingFacility.setVisibility(View.GONE);
            }

            // Processing Location
            if (traceability.getProcessingLocation() != null && !traceability.getProcessingLocation().isEmpty()) {
                tvProcessingLocation.setText(traceability.getProcessingLocation());
                tvProcessingLocation.setVisibility(View.VISIBLE);
            } else {
                tvProcessingLocation.setVisibility(View.GONE);
            }

        } else {
            layoutProcessingDetails.setVisibility(View.GONE);
        }
    }

    private boolean isVerified() {
        if (qualityBatch != null) {
            return qualityBatch.isVerified();
        }
        return traceability != null && traceability.hasDocuments();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void printTraceability() {
        String htmlContent = generatePrintHtml();

        WebView webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);

        PrintManager printManager = (PrintManager) getSystemService(PRINT_SERVICE);
        PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter("Traceability_" + traceability.getBatchNumber());
        printManager.print("Traceability_" + traceability.getBatchNumber(), printAdapter, new PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(new PrintAttributes.Resolution("print", "print", 300, 300))
                .build());
    }

    private String generatePrintHtml() {
        return "<html><head><style>" +
                "body { font-family: Arial, sans-serif; padding: 20px; }" +
                ".header { text-align: center; border-bottom: 2px solid #5A7411; padding-bottom: 10px; }" +
                ".title { font-size: 24px; color: #5A7411; }" +
                ".batch { font-size: 18px; color: #333; }" +
                ".section { margin-top: 20px; }" +
                ".section-title { font-size: 16px; font-weight: bold; color: #5A7411; border-bottom: 1px solid #ddd; padding-bottom: 5px; }" +
                ".row { display: flex; padding: 5px 0; }" +
                ".label { width: 150px; font-weight: bold; color: #666; }" +
                ".value { flex: 1; }" +
                ".qr { text-align: center; margin: 20px 0; }" +
                ".footer { text-align: center; margin-top: 30px; font-size: 12px; color: #999; border-top: 1px solid #ddd; padding-top: 10px; }" +
                "</style></head><body>" +
                "<div class='header'>" +
                "<div class='title'>AfriNuts Export SARL</div>" +
                "<div class='batch'>Traceability Report</div>" +
                "</div>" +
                "<div class='section'>" +
                "<div class='section-title'>Batch Information</div>" +
                "<div class='row'><div class='label'>Batch Number</div><div class='value'>" + traceability.getBatchNumber() + "</div></div>" +
                "<div class='row'><div class='label'>Grade</div><div class='value'>" + traceability.getGrade() + "</div></div>" +
                "<div class='row'><div class='label'>Weight</div><div class='value'>" + traceability.getFormattedWeight() + "</div></div>" +
                "<div class='row'><div class='label'>Processing Date</div><div class='value'>" + traceability.getFormattedProcessingDate() + "</div></div>" +
                "<div class='row'><div class='label'>Export Date</div><div class='value'>" + traceability.getFormattedExportDate() + "</div></div>" +
                "</div>" +
                "<div class='section'>" +
                "<div class='section-title'>Source Information</div>" +
                "<div class='row'><div class='label'>Supplier</div><div class='value'>" + traceability.getSupplierName() + "</div></div>" +
                "</div>" +
                "<div class='qr'>" +
                "<img src='" + traceability.getQrCodeUrl() + "' width='200' height='200'/>" +
                "<div>Scan to verify</div>" +
                "</div>" +
                "<div class='footer'>" +
                "This document is generated from Farm OS - AfriNuts Export's proprietary traceability system.<br/>" +
                "Verified: " + (isVerified() ? "Yes" : "No") +
                "</div>" +
                "</body></html>";
    }

    private void displaySustainability() {
        if (qualityBatch != null && qualityBatch.isVerified()) {
            layoutSustainability.setVisibility(View.VISIBLE);

            tvNoChildLabor.setText(qualityBatch.isNoChildLabor() ? "Yes" : "No");
            tvFarmOsMonitored.setText(qualityBatch.isFarmOsMonitored() ? "Yes" : "No");
            tvFirebreakProtection.setText(qualityBatch.isFirebreakProtection() ? "Yes" : "No");
            tvSustainableHarvesting.setText(qualityBatch.isSustainableHarvesting() ? "Yes" : "No");
            tvVerifiedDate.setText("Verified: " + qualityBatch.getFormattedVerifiedDate());

            int colorYes = getColor(android.R.color.holo_green_light);
            int colorNo = getColor(android.R.color.holo_red_light);

            tvNoChildLabor.setTextColor(qualityBatch.isNoChildLabor() ? colorYes : colorNo);
            tvFarmOsMonitored.setTextColor(qualityBatch.isFarmOsMonitored() ? colorYes : colorNo);
            tvFirebreakProtection.setTextColor(qualityBatch.isFirebreakProtection() ? colorYes : colorNo);
            tvSustainableHarvesting.setTextColor(qualityBatch.isSustainableHarvesting() ? colorYes : colorNo);

        } else {
            layoutSustainability.setVisibility(View.GONE);
        }
    }

    // Update displayDocuments()
    private void displayDocuments() {
        if (traceability == null) return;

        BatchTraceabilityEntity.Documents docs = traceability.getDocumentsObject();
        boolean hasDocuments = docs.hasDocuments();

        if (hasDocuments) {
            layoutDocuments.setVisibility(View.VISIBLE);

            // ===== Farm OS Generated Documents =====

            // Certificate of Origin
            if (docs.getQualityCertificate() != null && !docs.getQualityCertificate().isEmpty()) {
                String fileName = getDocumentDisplayName(docs.getQualityCertificate());
                tvQualityCertificate.setText(fileName);
                tvQualityCertificate.setTextColor(getColor(android.R.color.holo_green_light));
                tvQualityCertificate.setClickable(true);
                tvQualityCertificate.setOnClickListener(v -> viewDocument(docs.getQualityCertificate()));
            } else {
                tvQualityCertificate.setText("Generate now");
                tvQualityCertificate.setTextColor(getColor(R.color.warning));
                tvQualityCertificate.setClickable(false);
            }

            // Packing List (✅ Now using dedicated field)
            if (docs.getPackingList() != null && !docs.getPackingList().isEmpty()) {
                String fileName = getDocumentDisplayName(docs.getPackingList());
                tvBillOfLading.setText(fileName);  // Reusing the TextView
                tvBillOfLading.setTextColor(getColor(android.R.color.holo_green_light));
                tvBillOfLading.setClickable(true);
                tvBillOfLading.setOnClickListener(v -> viewDocument(docs.getPackingList()));
            } else {
                tvBillOfLading.setText("Generate now");
                tvBillOfLading.setTextColor(getColor(R.color.warning));
                tvBillOfLading.setClickable(false);
            }

            // Commercial Invoice
            if (docs.getCommercialInvoice() != null && !docs.getCommercialInvoice().isEmpty()) {
                String fileName = getDocumentDisplayName(docs.getCommercialInvoice());
                tvCommercialInvoice.setText(fileName);
                tvCommercialInvoice.setTextColor(getColor(android.R.color.holo_green_light));
                layoutCommercialInvoice.setClickable(true);
                layoutCommercialInvoice.setOnClickListener(v -> viewDocument(docs.getCommercialInvoice()));
                layoutCommercialInvoice.setVisibility(View.VISIBLE);
            } else {
                tvCommercialInvoice.setText("Add revenue first");
                tvCommercialInvoice.setTextColor(getColor(R.color.warning));
                layoutCommercialInvoice.setClickable(false);
                layoutCommercialInvoice.setVisibility(View.VISIBLE);
            }

            // ===== External Documents =====

            // Phyto-Sanitary (Government issued)
            if (docs.getPhytoSanitary() != null && !docs.getPhytoSanitary().isEmpty()) {
                tvPhytoSanitary.setText(getDocumentDisplayName(docs.getPhytoSanitary()));
                tvPhytoSanitary.setTextColor(getColor(android.R.color.holo_green_light));
                tvPhytoSanitary.setClickable(true);
                tvPhytoSanitary.setOnClickListener(v -> viewDocument(docs.getPhytoSanitary()));
            } else {
                tvPhytoSanitary.setText("Upload from government");
                tvPhytoSanitary.setTextColor(getColor(R.color.warning));
                tvPhytoSanitary.setClickable(false);
            }

            // Bill of Lading (Shipping company issued)
            // Note: This is separate from Packing List
            if (docs.getBillOfLading() != null && !docs.getBillOfLading().isEmpty() &&
                    !docs.getBillOfLading().startsWith("content://")) {  // Skip local images
                // Check if it's a real bill of lading (not an image)
                tvInsuranceCertificate.setText("Bill of Lading");
                tvInsuranceCertificate.setTextColor(getColor(android.R.color.holo_green_light));
                tvInsuranceCertificate.setClickable(true);
                final String billUrl = docs.getBillOfLading();
                tvInsuranceCertificate.setOnClickListener(v -> viewDocument(billUrl));
            } else {
                tvInsuranceCertificate.setText("Upload from shipping company");
                tvInsuranceCertificate.setTextColor(getColor(R.color.warning));
                tvInsuranceCertificate.setClickable(false);
            }

            // ✅ Display Additional Documents (for other uploaded files)
            displayAdditionalDocuments(docs);

        } else {
            layoutDocuments.setVisibility(View.GONE);
        }
    }

    // ✅ Helper method to get display name from document URL
    private String getDocumentDisplayName(String url) {
        if (url == null || url.isEmpty()) return "Document";

        // Extract filename from URL
        String fileName = url.substring(url.lastIndexOf("/") + 1);
        try {
            fileName = java.net.URLDecoder.decode(fileName, "UTF-8");
            // Clean up filename
            fileName = fileName.replace(".txt", "");
            fileName = fileName.replace("_", " ");
            // Remove query parameters if present
            int queryIndex = fileName.indexOf("?");
            if (queryIndex > 0) {
                fileName = fileName.substring(0, queryIndex);
            }
            // Remove token if present
            int tokenIndex = fileName.indexOf("?token");
            if (tokenIndex > 0) {
                fileName = fileName.substring(0, tokenIndex);
            }
            return fileName;
        } catch (Exception e) {
            return fileName;
        }
    }

    // ✅ Add this method to display Commercial Invoice
    private void displayCommercialInvoice(BatchTraceabilityEntity.Documents docs) {
        // Find the commercial invoice view or create one
        LinearLayout layoutCommercialInvoice = findViewById(R.id.layoutCommercialInvoice);
        TextView tvCommercialInvoice = findViewById(R.id.tvCommercialInvoice);
        ImageView ivCommercialInvoiceIcon = findViewById(R.id.ivCommercialInvoiceIcon);
        ImageView ivCommercialInvoiceForward = findViewById(R.id.ivCommercialInvoiceForward);

        if (layoutCommercialInvoice == null) {
            // If the view doesn't exist in XML, we'll create it programmatically
            return;
        }

        if (docs.getCommercialInvoice() != null && !docs.getCommercialInvoice().isEmpty()) {
            layoutCommercialInvoice.setVisibility(View.VISIBLE);
            tvCommercialInvoice.setText("Commercial Invoice");
            tvCommercialInvoice.setTextColor(getColor(android.R.color.holo_green_light));
            layoutCommercialInvoice.setClickable(true);
            layoutCommercialInvoice.setOnClickListener(v -> viewDocument(docs.getCommercialInvoice()));
        } else {
            layoutCommercialInvoice.setVisibility(View.GONE);
        }
    }

    private void displayAdditionalDocuments(BatchTraceabilityEntity.Documents docs) {
        if (layoutAdditionalDocs == null) return;

        layoutAdditionalDocs.removeAllViews();

        // Collect all document URLs from dedicated fields that exist
        List<String> allDocs = new ArrayList<>();

        // Farm OS generated
        if (docs.getQualityCertificate() != null) allDocs.add(docs.getQualityCertificate());
        if (docs.getPackingList() != null) allDocs.add(docs.getPackingList());
        if (docs.getCommercialInvoice() != null) allDocs.add(docs.getCommercialInvoice());

        // External documents
        if (docs.getPhytoSanitary() != null) allDocs.add(docs.getPhytoSanitary());
        if (docs.getBillOfLading() != null && !docs.getBillOfLading().startsWith("content://")) {
            allDocs.add(docs.getBillOfLading());
        }
        if (docs.getInsuranceCertificate() != null) allDocs.add(docs.getInsuranceCertificate());

        // Additional docs
        if (docs.getAdditionalDocs() != null) {
            for (String doc : docs.getAdditionalDocs()) {
                // Skip if it's a content URI or already in the list
                if (!doc.startsWith("content://") && !allDocs.contains(doc)) {
                    allDocs.add(doc);
                }
            }
        }

        // Remove duplicates
        java.util.Set<String> uniqueDocs = new java.util.HashSet<>(allDocs);

        int count = 0;
        for (String docUrl : uniqueDocs) {
            // Skip content URIs
            if (docUrl.startsWith("content://")) continue;

            LinearLayout docRow = createDocumentRow(docUrl);
            layoutAdditionalDocs.addView(docRow);
            count++;
        }

        // Show/hide the additional docs section
        TextView tvAdditionalDocsHeader = findViewById(R.id.tvAdditionalDocsHeader);
        if (tvAdditionalDocsHeader != null) {
            tvAdditionalDocsHeader.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        }
    }

    // ✅ Helper method to create a styled document row
    private LinearLayout createDocumentRow(String docUrl) {
        LinearLayout docRow = new LinearLayout(this);
        docRow.setOrientation(LinearLayout.HORIZONTAL);
        docRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        docRow.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
        docRow.setBackgroundResource(R.drawable.bg_rounded_description);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dpToPx(8));
        docRow.setLayoutParams(params);
        docRow.setClickable(true);
        docRow.setFocusable(true);
        docRow.setOnClickListener(v -> viewDocument(docUrl));

        // Icon
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_document);
        icon.setColorFilter(getColor(R.color.secondary));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                dpToPx(24), dpToPx(24)
        );
        iconParams.setMargins(0, 0, dpToPx(12), 0);
        icon.setLayoutParams(iconParams);
        docRow.addView(icon);

        // Document Name
        TextView text = new TextView(this);
        String fileName = getDisplayNameFromUrl(docUrl);
        text.setText("📄 " + fileName);
        text.setTextColor(getColor(R.color.on_surface));
        text.setTextSize(14);
        text.setTypeface(null, android.graphics.Typeface.BOLD);

        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        );
        text.setLayoutParams(textParams);
        docRow.addView(text);

        // Forward arrow
        ImageView forward = new ImageView(this);
        forward.setImageResource(R.drawable.ic_forward);
        forward.setColorFilter(getColor(R.color.on_surface));
        forward.setAlpha(0.6f);
        LinearLayout.LayoutParams forwardParams = new LinearLayout.LayoutParams(
                dpToPx(20), dpToPx(20)
        );
        forward.setLayoutParams(forwardParams);
        docRow.addView(forward);

        return docRow;
    }

    // ✅ Helper to get display name from URL
    private String getDisplayNameFromUrl(String url) {
        if (url == null) return "Document";

        // Handle content URIs
        if (url.startsWith("content://")) {
            return "Uploaded Image";
        }

        String fileName = url.substring(url.lastIndexOf("/") + 1);
        try {
            fileName = java.net.URLDecoder.decode(fileName, "UTF-8");
            // Clean up filename
            fileName = fileName.replace(".txt", "");
            fileName = fileName.replace("_", " ");
            fileName = fileName.replace("?alt=media", "");
            // Remove token if present
            int tokenIndex = fileName.indexOf("?token");
            if (tokenIndex > 0) {
                fileName = fileName.substring(0, tokenIndex);
            }
            return fileName;
        } catch (Exception e) {
            return fileName;
        }
    }

    // ✅ Helper method to convert dp to px
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    private void showDocumentGenerationDialog() {
        String[] options = {
                "Certificate of Origin",
                "Packing List",
                "Commercial Invoice",
                "Full Document Package"
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle("Generate Document")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            generateDocument("Certificate of Origin");
                            break;
                        case 1:
                            generateDocument("Packing List");
                            break;
                        case 2:
                            generateDocument("Commercial Invoice");
                            break;
                        case 3:
                            generateDocument("Document Package");
                            break;
                    }
                })
                .show();
    }

    private void generateDocument(String documentType) {
        Toast.makeText(this, "Generating " + documentType + "...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                // Get data
                SupplierEntity supplier = null;
                RevenueEntity revenue = null;

                if (traceability.getSupplierId() != null) {
                    supplier = supplierRepository.getSupplierByIdSync(traceability.getSupplierId());
                }

                // ✅ Try to get revenue from harvestCycleId or directly from batch
                if (traceability.getRevenueId() != null) {
                    revenue = database.revenueDao().getRevenueById(traceability.getRevenueId());
                }

                // ✅ If no revenue found, try to get revenue linked to the quality batch
                if (revenue == null && qualityBatch != null) {
                    List<RevenueEntity> revenues = database.revenueDao()
                            .getRevenuesByBatchId(qualityBatch.getId());
                    if (revenues != null && !revenues.isEmpty()) {
                        revenue = revenues.get(0);
                        Log.d(TAG, "Found revenue from batch: " + revenue.getId());
                    }
                }

                final long currentFarmId = farmId;
                final RevenueEntity finalRevenue = revenue;

                DocumentGenerator.DocumentUploadCallback callback = new DocumentGenerator.DocumentUploadCallback() {
                    @Override
                    public void onSuccess(String downloadUrl) {
                        updateTraceabilityWithDocument(documentType, downloadUrl);

                        runOnUiThread(() -> {
                            Toast.makeText(TraceabilityDetailActivity.this,
                                    documentType + " generated and uploaded to cloud!", Toast.LENGTH_LONG).show();
                            showDocumentPreviewDialog(documentType, downloadUrl);
                            loadData();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(TraceabilityDetailActivity.this,
                                    "Error: " + error, Toast.LENGTH_LONG).show();
                        });
                    }

                    @Override
                    public void onProgress(int progress) {
                        Log.d(TAG, "Upload progress: " + progress + "%");
                    }
                };

                // Generate and upload based on document type
                switch (documentType) {
                    case "Certificate of Origin":
                        DocumentGenerator.generateAndUploadCertificateOfOrigin(
                                this, supplier, qualityBatch, traceability, String.valueOf(currentFarmId), callback);
                        break;

                    case "Packing List":
                        DocumentGenerator.generateAndUploadPackingList(
                                this, qualityBatch, supplier, traceability, String.valueOf(currentFarmId), callback);
                        break;

                    case "Commercial Invoice":
                        if (finalRevenue != null && finalRevenue.getPricePerKg() > 0) {
                            DocumentGenerator.generateAndUploadCommercialInvoice(
                                    this, qualityBatch, finalRevenue, supplier, traceability, String.valueOf(currentFarmId), callback);
                        } else {
                            runOnUiThread(() -> showAddRevenueDialog());
                        }
                        break;

                    case "Document Package":
                        DocumentGenerator.generateAndUploadDocumentPackage(
                                this, supplier, qualityBatch, finalRevenue, traceability, String.valueOf(currentFarmId), callback);
                        break;

                    default:
                        runOnUiThread(() -> Toast.makeText(this, "Unknown document type", Toast.LENGTH_SHORT).show());
                        break;
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    private void updateTraceabilityWithDocument(String documentType, String downloadUrl) {
        try {
            BatchTraceabilityEntity.Documents docs = traceability.getDocumentsObject();

            switch (documentType) {
                case "Certificate of Origin":
                    docs.setQualityCertificate(downloadUrl);
                    break;

                case "Packing List":
                    docs.setPackingList(downloadUrl);
                    break;

                case "Commercial Invoice":
                    docs.setCommercialInvoice(downloadUrl);
                    break;

                case "Phyto-Sanitary":
                    docs.setPhytoSanitary(downloadUrl);
                    break;

                case "Bill of Lading":
                    docs.setBillOfLading(downloadUrl);
                    break;

                case "Insurance Certificate":
                    docs.setInsuranceCertificate(downloadUrl);
                    break;

                default:
                    // For unknown types, add to additional docs
                    if (docs.getAdditionalDocs() == null) {
                        docs.setAdditionalDocs(new ArrayList<>());
                    }
                    if (!docs.getAdditionalDocs().contains(downloadUrl)) {
                        docs.getAdditionalDocs().add(downloadUrl);
                    }
                    break;
            }

            traceability.setDocumentsObject(docs);
            traceability.setUpdatedAt(System.currentTimeMillis());

            repository.updateTraceability(traceability, new BatchTraceabilityRepository.RepositoryCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    Log.d(TAG, "Traceability updated with document: " + documentType);
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Failed to update traceability with document: " + error);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error updating traceability with document URL", e);
        }
    }

    // ✅ Show document preview with the download URL
    private void showDocumentPreviewDialog(String title, String downloadUrl) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage("Document uploaded to cloud storage.\n\nURL: " + downloadUrl)
                .setPositiveButton("Open", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(this, "Unable to open document", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Copy URL", (dialog, which) -> {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("Document URL", downloadUrl);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "URL copied to clipboard", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showAddRevenueDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Revenue Required")
                .setMessage("Commercial Invoice requires price data. Would you like to add revenue for this batch?")
                .setPositiveButton("Add Revenue", (dialog, which) -> openAddRevenueDialog())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openAddRevenueDialog() {
        if (qualityBatch == null) {
            Toast.makeText(this, "Batch not found", Toast.LENGTH_SHORT).show();
            return;
        }

        AddRevenueFromTraceabilityDialog dialog =
                AddRevenueFromTraceabilityDialog.newInstance(qualityBatch.getId());
        dialog.setOnRevenueAddedListener(revenue -> {
            // Refresh data
            loadData();
            Toast.makeText(this, "Revenue added! You can now generate the Commercial Invoice.", Toast.LENGTH_LONG).show();
        });
        dialog.show(getSupportFragmentManager(), "AddRevenueFromTraceabilityDialog");
    }
    private void shareDocument(String content, String title) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, content);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
        startActivity(Intent.createChooser(shareIntent, "Share Document"));
    }

    private void generateQRCode(String url) {
        Bitmap qrBitmap = QRCodeGenerator.generateQRCode(url);
        if (qrBitmap != null) {
            ivQrCode.setImageBitmap(qrBitmap);
        } else {
            ivQrCode.setImageResource(R.drawable.ic_qr_code);
        }
    }

    private void saveQRCodeToGallery() {
        if (ivQrCode.getDrawable() == null) {
            Toast.makeText(this, "No QR code to save", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap qrBitmap = ((BitmapDrawable) ivQrCode.getDrawable()).getBitmap();

        new Thread(() -> {
            String savedPath = QRCodeGenerator.saveQRCodeToGallery(this, qrBitmap, traceability.getBatchNumber());
            runOnUiThread(() -> {
                if (savedPath != null) {
                    Toast.makeText(this, "QR Code saved to gallery: " + savedPath, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Failed to save QR code", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void shareQRCode() {
        if (ivQrCode.getDrawable() == null) {
            Toast.makeText(this, "No QR code to share", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap qrBitmap = ((BitmapDrawable) ivQrCode.getDrawable()).getBitmap();

        File cacheFile = new File(getCacheDir(), "qr_" + traceability.getBatchNumber() + ".png");
        try {
            FileOutputStream fos = new FileOutputStream(cacheFile);
            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            Uri uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", cacheFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            String shareText = "🌿 AfriNuts Traceability\n" +
                    "Batch: " + traceability.getBatchNumber() + "\n" +
                    "Grade: " + traceability.getGrade() + "\n" +
                    "Weight: " + traceability.getFormattedWeight() + "\n" +
                    "🔗 Trace: " + traceability.getTraceabilityUrl();

            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

            startActivity(Intent.createChooser(shareIntent, "Share Traceability"));

        } catch (Exception e) {
            Toast.makeText(this, "Error sharing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void viewDocument(String url) {
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "Document not available", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open document", Toast.LENGTH_SHORT).show();
        }
    }

    private void viewBatch() {
        if (qualityBatch == null) {
            Toast.makeText(this, "Batch not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, QualityBatchDetailActivity.class);
        intent.putExtra(QualityBatchDetailActivity.EXTRA_BATCH_ID, qualityBatch.getId());
        startActivity(intent);
    }

    private void viewSupplier() {
        if (supplier == null) {
            Toast.makeText(this, "Supplier not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, SupplierDetailActivity.class);
        intent.putExtra(SupplierDetailActivity.EXTRA_SUPPLIER_ID, supplier.getId());
        startActivity(intent);
    }

    private void shareTraceability() {
        StringBuilder shareText = new StringBuilder();
        shareText.append("AfriNuts Traceability\n");
        shareText.append("===========================\n");
        shareText.append("Batch: ").append(traceability.getBatchNumber()).append("\n");
        shareText.append("Grade: ").append(traceability.getGrade()).append("\n");
        shareText.append("Weight: ").append(traceability.getFormattedWeight()).append("\n");

        if (traceability.getSupplierName() != null) {
            shareText.append("Supplier: ").append(traceability.getSupplierName()).append("\n");
        }

        shareText.append("Processing Date: ").append(traceability.getFormattedProcessingDate()).append("\n");

        if (traceability.isExported()) {
            shareText.append("Export Date: ").append(traceability.getFormattedExportDate()).append("\n");
            if (traceability.getBuyer() != null) {
                shareText.append("Buyer: ").append(traceability.getBuyer()).append("\n");
            }
            if (traceability.getDestination() != null) {
                shareText.append("Destination: ").append(traceability.getDestination()).append("\n");
            }
        }

        shareText.append("\n🔗 Trace URL: ").append(traceability.getQrCodeUrl());

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());
        startActivity(Intent.createChooser(shareIntent, "Share Traceability"));
    }

    private void confirmDelete() {
        if (!sessionManager.isOwner()) {
            Toast.makeText(this, "Only owners can delete traceability records", Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Traceability")
                .setMessage("Are you sure you want to delete this traceability record?")
                .setPositiveButton("Delete", (dialog, which) -> deleteTraceability())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteTraceability() {
        Toast.makeText(this, "Deleting...", Toast.LENGTH_SHORT).show();

        repository.deleteTraceability(traceability, new BatchTraceabilityRepository.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(() -> {
                    Toast.makeText(TraceabilityDetailActivity.this,
                            "Traceability record deleted", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(TraceabilityDetailActivity.this,
                                "Error deleting: " + error, Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (repository != null) {
            repository.shutdown();
        }
        executor.shutdown();
    }
}