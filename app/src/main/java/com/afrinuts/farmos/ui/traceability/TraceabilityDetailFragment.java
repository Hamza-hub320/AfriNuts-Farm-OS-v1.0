package com.afrinuts.farmos.ui.traceability;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;
import com.afrinuts.farmos.data.repository.BatchTraceabilityRepository;
import com.afrinuts.farmos.data.repository.QualityBatchRepository;
import com.afrinuts.farmos.data.repository.RevenueRepository;
import com.afrinuts.farmos.data.repository.SupplierRepository;
import com.afrinuts.farmos.ui.analytics.AnalyticsHubActivity;
import com.afrinuts.farmos.ui.quality.QualityBatchDetailActivity;
import com.afrinuts.farmos.utils.DocumentGenerator;
import com.afrinuts.farmos.utils.QRCodeGenerator;
import com.afrinuts.farmos.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Tablet detail pane for a traceability record, using the phone detail layout and actions. */
public class TraceabilityDetailFragment extends Fragment {
    private static final String ARG_ID = "traceability_id";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private long traceabilityId;
    private BatchTraceabilityRepository repository;
    private BatchTraceabilityEntity traceability;
    private QualityBatchEntity qualityBatch;
    private SessionManager session;
    private RevenueRepository revenueRepository;
    private View root;

    public static TraceabilityDetailFragment newInstance(long id) {
        TraceabilityDetailFragment fragment = new TraceabilityDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ID, id);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.activity_traceability_detail, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        root = view;
        traceabilityId = getArguments() == null ? -1 : getArguments().getLong(ARG_ID, -1);
        session = new SessionManager(requireContext());
        repository = new BatchTraceabilityRepository(requireContext(), 1);
        styleTabletPane(view);
        configureActions(view);
        loadData();
    }

    private void styleTabletPane(View view) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(26));
        view.setBackground(background);
        view.setClipToOutline(true);
        View toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) toolbar.setVisibility(View.GONE);
        View scroll = view.findViewById(R.id.traceabilityDetailScroll);
        if (scroll != null) scroll.setPadding(dp(12), dp(8), dp(12), dp(12));
        hide(view, R.id.layoutProcessingDetails);
        hide(view, R.id.layoutSustainability);
        hide(view, R.id.layoutSignature);

        ViewGroup content = view.findViewById(R.id.traceabilityDetailContent);
        if (content != null) {
            styleTabletHeaderAndKpis(content);
            View qr = findAncestorCard(view.findViewById(R.id.ivQrCode));
            View docs = view.findViewById(R.id.layoutDocuments);
            if (qr != null && docs != null && qr.getParent() == content && docs.getParent() == content) {
                int docsIndex = content.indexOfChild(docs);
                content.removeView(qr);
                content.addView(qr, docsIndex);
            }
            View header = view.findViewById(R.id.cardTraceabilityHero);
            if (header != null) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) header.getLayoutParams();
                lp.topMargin = 0;
                lp.bottomMargin = 0;
                header.setLayoutParams(lp);
            }
            View origin = view.findViewById(R.id.cardTraceabilityOrigin);
            View shipment = view.findViewById(R.id.layoutShipping);
            View documents = view.findViewById(R.id.layoutDocuments);
            styleTabletSection(origin);
            styleTabletSection(shipment);
            styleTabletSection(documents);
            styleTabletDocumentAction(view);
        }
        moveFooterActions(view);
    }

    private void styleTabletHeaderAndKpis(ViewGroup content) {
        CardView hero = content.findViewById(R.id.cardTraceabilityHero);
        if (hero != null && hero.getChildAt(0) instanceof LinearLayout) {
            LinearLayout body = (LinearLayout) hero.getChildAt(0);
            body.setPadding(dp(16), dp(8), dp(16), dp(2));
            TextView title = body.findViewById(R.id.tvBatchNumber);
            TextView subtitle = body.findViewById(R.id.tvHeroSubtitle);
            TextView status = body.findViewById(R.id.tvStatus);
            TextView grade = body.findViewById(R.id.tvGrade);
            if (title != null) title.setTextSize(27f);
            if (subtitle != null) {
                subtitle.setTextSize(15.5f);
                subtitle.setSingleLine(true);
                subtitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
            }
            if (subtitle != null && status != null && grade != null
                    && subtitle.getParent() == body && status.getParent() instanceof ViewGroup) {
                ViewGroup badgeParent = (ViewGroup) status.getParent();
                if (badgeParent != body) {
                    int subtitleIndex = body.indexOfChild(subtitle);
                    body.removeView(subtitle);
                    body.removeView(badgeParent);
                    if (status.getParent() instanceof ViewGroup) ((ViewGroup) status.getParent()).removeView(status);
                    if (grade.getParent() instanceof ViewGroup) ((ViewGroup) grade.getParent()).removeView(grade);
                    LinearLayout headerLine = new LinearLayout(requireContext());
                    headerLine.setGravity(Gravity.CENTER_VERTICAL);
                    headerLine.addView(subtitle, new LinearLayout.LayoutParams(0, -2, 1f));
                    status.setPadding(dp(10), dp(4), dp(10), dp(4));
                    grade.setPadding(dp(10), dp(4), dp(10), dp(4));
                    LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-2, -2);
                    statusParams.setMarginStart(dp(8));
                    headerLine.addView(status, statusParams);
                    LinearLayout.LayoutParams gradeParams = new LinearLayout.LayoutParams(-2, -2);
                    gradeParams.setMarginStart(dp(6));
                    headerLine.addView(grade, gradeParams);
                    body.addView(headerLine, Math.min(subtitleIndex, body.getChildCount()));
                }
            }
        }
        if (content.getChildCount() > 1 && content.getChildAt(1) instanceof LinearLayout) {
            LinearLayout stats = (LinearLayout) content.getChildAt(1);
            for (int i = 0; i < stats.getChildCount(); i++) {
                View child = stats.getChildAt(i);
                if (!(child instanceof CardView)) continue;
                CardView card = (CardView) child;
                card.setRadius(dp(24));
                card.setCardElevation(0);
                card.setCardBackgroundColor(i == 0 ? 0xFF0E6B3C : 0xFFF5F0E6);
                TextView value = card.findViewById(i == 0 ? R.id.tvHeroWeight
                        : i == 1 ? R.id.tvHeroQualityGrade : R.id.tvHeroExportDate);
                if (value != null) value.setTextSize(23f);
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) card.getLayoutParams();
                lp.height = dp(112);
                card.setLayoutParams(lp);
            }
            ViewGroup.MarginLayoutParams statsParams = (ViewGroup.MarginLayoutParams) stats.getLayoutParams();
            statsParams.topMargin = -dp(4);
            statsParams.bottomMargin = dp(8);
            stats.setLayoutParams(statsParams);
        }
    }

    /** Tablet detail uses plain sections; keep the heading and rows without nested cards or icons. */
    private void styleTabletSection(View section) {
        if (!(section instanceof CardView)) return;
        CardView card = (CardView) section;
        card.setCardElevation(0);
        card.setRadius(0);
        card.setCardBackgroundColor(Color.TRANSPARENT);
        ViewGroup.MarginLayoutParams cardParams = (ViewGroup.MarginLayoutParams) card.getLayoutParams();
        cardParams.bottomMargin = dp(8);
        card.setLayoutParams(cardParams);
        if (!(card.getChildAt(0) instanceof ViewGroup)) return;
        ViewGroup body = (ViewGroup) card.getChildAt(0);
        body.setBackgroundColor(Color.TRANSPARENT);
        body.setPadding(0, 0, 0, 0);
        if (body.getChildCount() == 0 || !(body.getChildAt(0) instanceof ViewGroup)) return;
        ViewGroup heading = (ViewGroup) body.getChildAt(0);
        if (heading.getChildCount() > 1) heading.getChildAt(0).setVisibility(View.GONE);
        heading.setPadding(0, 0, 0, dp(4));
        if (heading.getChildCount() > 1 && heading.getChildAt(1) instanceof TextView) {
            TextView title = (TextView) heading.getChildAt(1);
            title.setTextColor(0xFF1C1B1A);
        }
    }

    private void styleTabletDocumentAction(View view) {
        View action = view.findViewById(R.id.btnGenerateDocuments);
        if (!(action instanceof MaterialButton)) return;
        MaterialButton button = (MaterialButton) action;
        ViewGroup.LayoutParams params = button.getLayoutParams();
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.height = dp(58);
        button.setLayoutParams(params);
        button.setPadding(dp(24), 0, dp(24), 0);
        button.setTextColor(0xFF1C1B1A);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
        button.setStrokeWidth(dp(1));
        button.setStrokeColor(android.content.res.ColorStateList.valueOf(0xFFE9E1CF));
        ViewGroup parent = (ViewGroup) button.getParent();
        if (parent instanceof LinearLayout) {
            LinearLayout.LayoutParams actionParams = (LinearLayout.LayoutParams) button.getLayoutParams();
            actionParams.gravity = Gravity.START;
            actionParams.topMargin = dp(12);
            button.setLayoutParams(actionParams);
        }
    }

    private View findAncestorCard(View view) {
        View current = view;
        while (current != null && !(current instanceof CardView)) {
            if (!(current.getParent() instanceof View)) return null;
            current = (View) current.getParent();
        }
        return current;
    }

    private void hide(View root, int id) {
        View view = root.findViewById(id);
        if (view != null) view.setVisibility(View.GONE);
    }

    private void moveFooterActions(View view) {
        LinearLayout content = view.findViewById(R.id.traceabilityDetailContent);
        if (content == null) return;
        View oldFooter = view.findViewById(R.id.btnViewBatch);
        View oldRow = oldFooter == null ? null : (View) oldFooter.getParent();
        if (oldRow != null) oldRow.setVisibility(View.GONE);
        hide(view, R.id.btnDelete);
        LinearLayout actions = new LinearLayout(requireContext());
        actions.setOrientation(LinearLayout.VERTICAL);
        actions.setPadding(0, dp(8), 0, dp(8));
        LinearLayout footerRow = new LinearLayout(requireContext());
        addAction(footerRow, "Share", () -> shareTraceability(), false);
        addAction(footerRow, "View Batch", () -> viewBatch(), false);
        actions.addView(footerRow);
        MaterialButton delete = makeButton("Delete", false, true);
        delete.setVisibility(session.canEditData() ? View.VISIBLE : View.GONE);
        delete.setOnClickListener(v -> confirmDelete());
        LinearLayout.LayoutParams deleteLp = new LinearLayout.LayoutParams(-1, dp(58));
        deleteLp.topMargin = dp(8);
        actions.addView(delete, deleteLp);
        content.addView(actions);
    }

    private void addAction(LinearLayout row, String title, Runnable action, boolean primary) {
        MaterialButton button = makeButton(title, primary, false);
        button.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(58), 1f);
        lp.setMargins(dp(3), dp(4), dp(3), dp(4));
        row.addView(button, lp);
    }

    private MaterialButton makeButton(String title, boolean primary, boolean danger) {
        MaterialButton button = new MaterialButton(requireContext());
        button.setText(title);
        button.setTextSize(13);
        button.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.manrope_extrabold));
        button.setMinHeight(0);
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setCornerRadius(dp(999));
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                danger ? 0xFFF9E4DE : primary ? 0xFF0E6B3C : Color.WHITE));
        button.setTextColor(danger ? 0xFFB3402A : primary ? Color.WHITE : 0xFF1C1B1A);
        if (!primary && !danger) {
            button.setStrokeWidth(dp(1));
            button.setStrokeColor(android.content.res.ColorStateList.valueOf(0xFFE9E1CF));
        }
        return button;
    }

    private void configureActions(View view) {
        View back = view.findViewById(R.id.btnHeaderBack);
        if (back != null) back.setVisibility(View.GONE);
        View share = view.findViewById(R.id.btnHeaderShare);
        if (share != null) share.setOnClickListener(v -> shareTraceability());
        View edit = view.findViewById(R.id.btnHeaderEdit);
        if (edit != null) edit.setOnClickListener(v -> showEditDialog());
        View generate = view.findViewById(R.id.btnGenerateDocuments);
        if (generate != null) generate.setOnClickListener(v -> showGenerateDialog());
        View copy = view.findViewById(R.id.ivCopyCode);
        if (copy != null) copy.setOnClickListener(v -> Toast.makeText(requireContext(), "Verification code copied", Toast.LENGTH_SHORT).show());
    }

    private void loadData() {
        executor.execute(() -> {
            BatchTraceabilityEntity loaded = repository.getTraceabilityByIdSync(traceabilityId);
            QualityBatchEntity batch = null;
            if (loaded != null && loaded.getQualityBatchId() != null) {
                batch = new QualityBatchRepository(requireContext(), loaded.getFarmId()).getBatchByIdSync(loaded.getQualityBatchId());
            }
            BatchTraceabilityEntity finalLoaded = loaded;
            QualityBatchEntity finalBatch = batch;
            if (isAdded()) requireActivity().runOnUiThread(() -> display(finalLoaded, finalBatch));
        });
    }

    private void display(BatchTraceabilityEntity item, QualityBatchEntity batch) {
        if (item == null || !isAdded()) return;
        traceability = item;
        qualityBatch = batch;
        setText(R.id.tvBatchNumber, item.getBatchNumber());
        setText(R.id.tvHeroSubtitle, value(item.getBuyer(), "Buyer not set") + " · "
                + value(item.getDestination(), "Destination not set") + " · "
                + (item.isExported() ? item.getFormattedExportDate() : item.getFormattedProcessingDate()));
        setText(R.id.tvHeroWeight, item.getFormattedWeight());
        setText(R.id.tvHeroQualityGrade, value(item.getQualityGrade(), item.getGrade()));
        String date = item.isExported() ? item.getFormattedExportDate() : "Not exported";
        setText(R.id.tvHeroExportDate, date);
        setText(R.id.tvStatus, item.isExported() ? "• Exported" : "• Processing");
        TextView status = root.findViewById(R.id.tvStatus);
        status.setBackgroundResource(item.isExported() ? R.drawable.bg_traceability_status_exported : R.drawable.bg_traceability_status_processing);
        status.setTextColor(item.isExported() ? 0xFF0A5530 : 0xFF8A6414);
        setText(R.id.tvGrade, value(item.getGrade(), "—"));
        setText(R.id.tvSupplierName, value(item.getSupplierName(), "—"));
        setText(R.id.tvQualityGrade, value(item.getQualityGrade(), item.getGrade()));
        setText(R.id.tvOriginSourceType, batch != null && batch.getSourceType() != null ? batch.getSourceType().getDisplayName() : "—");
        setText(R.id.tvOriginProcessingDate, item.getFormattedProcessingDate());
        setText(R.id.tvShippingDestination, value(item.getDestination(), "Not set"));
        setText(R.id.tvBuyerShipping, value(item.getBuyer(), "Not set"));
        setText(R.id.tvShippingExportDate, item.isExported() ? item.getFormattedExportDate() : "Not set");
        setText(R.id.tvExportStatus, item.isExported() ? "Exported" : "Processing");
        TextView exportStatus = root.findViewById(R.id.tvExportStatus);
        if (exportStatus != null) {
            exportStatus.setBackgroundColor(Color.WHITE);
            exportStatus.setTextColor(0xFF1C1B1A);
            exportStatus.setPadding(0, 0, 0, 0);
        }
        setText(R.id.tvVerificationCode, value(item.getVerificationCode(), "—"));
        setText(R.id.tvShipmentVerificationCode, value(item.getVerificationCode(), "—"));
        setText(R.id.tvCertificationsShipping, value(item.getCertificationIds(), "None"));
        setText(R.id.tvHarvestYearShipping, item.getHarvestYear() > 0 ? String.valueOf(item.getHarvestYear()) : "Not set");
        setText(R.id.tvQrUrl, value(item.getQrCodeUrl(), item.getTraceabilityUrl()));
        android.widget.ImageView qr = root.findViewById(R.id.ivQrCode);
        android.graphics.Bitmap bitmap = QRCodeGenerator.generateQRCode(item.getQrCodeUrl());
        if (bitmap != null) qr.setImageBitmap(bitmap);
        View shipping = root.findViewById(R.id.layoutShipping);
        if (shipping != null) shipping.setVisibility(View.VISIBLE);
        View docs = root.findViewById(R.id.layoutDocuments);
        if (docs != null) docs.setVisibility(View.VISIBLE);
        View viewBatch = root.findViewById(R.id.btnViewBatch);
        if (viewBatch != null) viewBatch.setVisibility(batch != null ? View.VISIBLE : View.GONE);
        boolean canEdit = session.canEditData();
        View headerEdit = root.findViewById(R.id.btnHeaderEdit);
        if (headerEdit != null) headerEdit.setVisibility(canEdit ? View.VISIBLE : View.GONE);
        setShippingAction(R.id.btnSetExportDate, "Set export date", canEdit, false);
        setShippingAction(R.id.btnEditDestination, "Edit destination", canEdit, false);
        setShippingAction(R.id.btnUploadDocuments, "Upload Documents", canEdit, true);
        renderDocumentRows(item.getDocumentsObject());
    }

    private void setShippingAction(int id, String label, boolean visible, boolean primary) {
        MaterialButton button = root.findViewById(id);
        if (button == null) return;
        button.setVisibility(visible ? View.VISIBLE : View.GONE);
        button.setText(label);
        button.setTextSize(13);
        button.setMinHeight(0);
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setCornerRadius(dp(999));
        button.setPadding(dp(16), 0, dp(16), 0);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primary ? 0xFF0E6B3C : 0xFFF3EAD8));
        button.setTextColor(primary ? Color.WHITE : 0xFF1C1B1A);
        ViewGroup.LayoutParams raw = button.getLayoutParams();
        raw.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        raw.height = dp(48);
        if (raw instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams rowParams = (LinearLayout.LayoutParams) raw;
            rowParams.weight = 0;
            rowParams.setMargins(dp(3), 0, dp(3), 0);
        }
        button.setLayoutParams(raw);
        button.setOnClickListener(v -> {
            if (id == R.id.btnUploadDocuments) showUploadDialog();
            else showEditDialog();
        });
    }

    private String value(String value, String fallback) { return value == null || value.trim().isEmpty() ? fallback : value; }
    private void setText(int id, String value) { TextView v = root.findViewById(id); if (v != null) v.setText(value == null ? "—" : value); }

    private void renderDocumentRows(BatchTraceabilityEntity.Documents docs) {
        if (docs == null) return;
        bindGeneratedDoc(R.id.layoutCertificateOfOriginRow, R.id.tvCertificateOfOriginBadge,
                findAdditionalDocument(docs, "Certificate_of_Origin_"));
        bindGeneratedDoc(R.id.layoutPackingListRow, R.id.tvPackingListBadge, docs.getPackingList());
        bindGeneratedDoc(R.id.layoutCommercialInvoice, R.id.tvCommercialInvoiceBadge, docs.getCommercialInvoice());
        bindGeneratedDoc(R.id.layoutDocumentPackageRow, R.id.tvDocumentPackageBadge,
                findAdditionalDocument(docs, "Document_Package_"));
        bindDoc(R.id.layoutQualityCertificateRow, R.id.tvQualityCertificate, R.id.tvQualityCertificateBadge, "Quality certificate", docs.getQualityCertificate());
        bindDoc(R.id.layoutPhytoSanitaryRow, R.id.tvPhytoSanitary, R.id.tvPhytoSanitaryBadge, "Phytosanitary certificate", docs.getPhytoSanitary());
        bindDoc(R.id.layoutBillOfLadingRow, R.id.tvBillOfLading, R.id.tvBillOfLadingBadge, "Bill of lading", docs.getBillOfLading());
        bindDoc(R.id.layoutCommercialInvoice, R.id.tvCommercialInvoice, R.id.tvCommercialInvoiceBadge, "Commercial invoice", docs.getCommercialInvoice());
        bindDoc(R.id.layoutInsuranceCertificateRow, R.id.tvInsuranceCertificate, R.id.tvInsuranceCertificateBadge, "Insurance certificate", docs.getInsuranceCertificate());
    }

    private String findAdditionalDocument(BatchTraceabilityEntity.Documents docs, String prefix) {
        if (docs.getAdditionalDocs() == null) return null;
        for (String url : docs.getAdditionalDocs()) {
            if (url != null && fileName(url).toLowerCase(Locale.US).startsWith(prefix.toLowerCase(Locale.US))) return url;
        }
        return null;
    }

    private String fileName(String url) {
        String clean = url;
        int query = clean.indexOf('?');
        if (query >= 0) clean = clean.substring(0, query);
        try { clean = java.net.URLDecoder.decode(clean, "UTF-8"); } catch (Exception ignored) { }
        return clean.substring(Math.max(clean.lastIndexOf('/'), clean.lastIndexOf('\\')) + 1);
    }

    private void bindGeneratedDoc(int rowId, int badgeId, String url) {
        View row = root.findViewById(rowId);
        TextView badge = root.findViewById(badgeId);
        if (row == null || badge == null) return;
        String fileName = url == null || url.isEmpty() ? "" : fileName(url);
        int dot = fileName.lastIndexOf('.');
        badge.setText(dot >= 0 && dot < fileName.length() - 1
                ? fileName.substring(dot + 1).toUpperCase(Locale.US) : "Generate");
        row.setOnClickListener(v -> {
            if (url != null && !url.isEmpty()) {
                try { startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))); }
                catch (Exception ignored) { Toast.makeText(requireContext(), "Unable to open document", Toast.LENGTH_SHORT).show(); }
            } else showGenerateDialog();
        });
    }

    private void bindDoc(int rowId, int titleId, int badgeId, String title, String url) {
        LinearLayout row = root.findViewById(rowId);
        TextView label = root.findViewById(titleId);
        TextView badge = root.findViewById(badgeId);
        if (row == null || label == null || badge == null) return;
        label.setText(title);
        badge.setText(url == null || url.isEmpty() ? "PDF" : "PDF");
        row.setOnClickListener(v -> {
            if (url != null && !url.isEmpty()) {
                try { startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))); }
                catch (Exception ignored) { Toast.makeText(requireContext(), "Unable to open document", Toast.LENGTH_SHORT).show(); }
            } else showUploadDialog();
        });
    }

    private void showEditDialog() {
        EditShippingDialog dialog = EditShippingDialog.newInstance(traceabilityId);
        dialog.setOnShippingUpdatedListener(this::loadData);
        dialog.show(getParentFragmentManager(), "TraceEditShipping");
    }
    private void showUploadDialog() {
        UploadDocumentDialog dialog = UploadDocumentDialog.newInstance(traceabilityId);
        dialog.setOnDocumentUploadedListener(this::loadData);
        dialog.show(getParentFragmentManager(), "TraceUploadDocuments");
    }
    private void showGenerateDialog() {
        GenerateDocumentDialog dialog = GenerateDocumentDialog.newInstance();
        dialog.setListener(this::generateDocument);
        dialog.show(getParentFragmentManager(), "TraceGenerateDocuments");
    }

    private void generateDocument(String type) {
        if (traceability == null || qualityBatch == null) {
            Toast.makeText(requireContext(), "Batch details are not available", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(requireContext(), "Generating " + type + "…", Toast.LENGTH_SHORT).show();
        executor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                SupplierEntity supplier = traceability.getSupplierId() == null ? null
                        : new SupplierRepository(requireContext(), traceability.getFarmId())
                        .getSupplierByIdSync(traceability.getSupplierId());
                RevenueEntity revenue = traceability.getRevenueId() == null ? null
                        : db.revenueDao().getRevenueById(traceability.getRevenueId());
                if (revenueRepository == null) revenueRepository = new RevenueRepository(requireContext(), traceability.getFarmId());
                if (("Commercial Invoice".equals(type) || "Document Package".equals(type))
                        && (revenue == null || revenue.getPricePerKg() <= 0)) {
                    requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "A linked sale is required to generate this document", Toast.LENGTH_LONG).show());
                    return;
                }
                DocumentGenerator.DocumentUploadCallback callback = new DocumentGenerator.DocumentUploadCallback() {
                    @Override public void onSuccess(String url) {
                        saveGeneratedDocument(type, url);
                        if (isAdded()) requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), type + " generated", Toast.LENGTH_LONG).show();
                            loadData();
                        });
                    }
                    @Override public void onError(String error) {
                        if (isAdded()) requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_LONG).show());
                    }
                    @Override public void onProgress(int progress) { }
                };
                String farm = String.valueOf(traceability.getFarmId());
                switch (type) {
                    case "Certificate of Origin":
                        DocumentGenerator.generateAndUploadCertificateOfOrigin(requireContext(), supplier, qualityBatch, traceability, farm, callback); break;
                    case "Packing List":
                        DocumentGenerator.generateAndUploadPackingList(requireContext(), qualityBatch, supplier, traceability, farm, callback); break;
                    case "Commercial Invoice":
                        DocumentGenerator.generateAndUploadCommercialInvoice(requireContext(), qualityBatch, revenue, supplier, traceability, farm, revenueRepository, callback); break;
                    case "Document Package":
                        DocumentGenerator.generateAndUploadDocumentPackage(requireContext(), supplier, qualityBatch, revenue, traceability, farm, revenueRepository, callback); break;
                    default:
                        requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Unknown document type", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                if (isAdded()) requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void saveGeneratedDocument(String type, String url) {
        BatchTraceabilityEntity.Documents docs = traceability.getDocumentsObject();
        if (docs == null) docs = new BatchTraceabilityEntity.Documents();
        switch (type) {
            case "Packing List": docs.setPackingList(url); break;
            case "Commercial Invoice": docs.setCommercialInvoice(url); break;
            default:
                if (docs.getAdditionalDocs() == null) docs.setAdditionalDocs(new java.util.ArrayList<>());
                if (!docs.getAdditionalDocs().contains(url)) docs.getAdditionalDocs().add(url);
                break;
        }
        traceability.setDocumentsObject(docs);
        traceability.setUpdatedAt(System.currentTimeMillis());
        repository.updateTraceability(traceability, new BatchTraceabilityRepository.RepositoryCallback<Boolean>() {
            @Override public void onSuccess(Boolean result) { }
            @Override public void onError(String error) {
                if (isAdded()) requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Could not save document: " + error, Toast.LENGTH_LONG).show());
            }
        });
    }
    private void shareTraceability() {
        if (traceability == null) return;
        String text = "AfriNuts Traceability\nBatch: " + traceability.getBatchNumber()
                + "\nGrade: " + traceability.getGrade() + "\nWeight: " + traceability.getFormattedWeight()
                + "\nTrace URL: " + traceability.getQrCodeUrl();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(intent, "Share Traceability"));
    }
    private void viewBatch() {
        if (qualityBatch == null || !(requireActivity() instanceof AnalyticsHubActivity)) return;
        AnalyticsHubActivity hub = (AnalyticsHubActivity) requireActivity();
        hub.openQualityBatch(qualityBatch.getId());
    }
    private void confirmDelete() {
        if (!session.isOwner()) { Toast.makeText(requireContext(), "Only owners can delete traceability records", Toast.LENGTH_SHORT).show(); return; }
        new MaterialAlertDialogBuilder(requireContext()).setTitle("Delete Traceability")
                .setMessage("Are you sure you want to delete this traceability record?")
                .setPositiveButton("Delete", (d, w) -> repository.deleteTraceability(traceability, new BatchTraceabilityRepository.RepositoryCallback<Boolean>() {
                    @Override public void onSuccess(Boolean result) {
                        if (getActivity() instanceof AnalyticsHubActivity) {
                            AnalyticsHubActivity hub = (AnalyticsHubActivity) getActivity();
                            hub.onTraceabilitySelected(-1);
                            hub.refreshTraceabilityList();
                        }
                        Toast.makeText(requireContext(), "Traceability record deleted", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onError(String error) { Toast.makeText(requireContext(), "Error deleting: " + error, Toast.LENGTH_LONG).show(); }
                })).setNegativeButton("Cancel", null).show();
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override public void onDestroy() {
        super.onDestroy();
        if (repository != null) repository.shutdown();
        executor.shutdown();
    }
}
