package com.afrinuts.farmos.ui.compliance;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.CertificationEntity;
import com.afrinuts.farmos.data.local.entity.DocumentEntity;
import com.afrinuts.farmos.data.repository.CertificationRepository;
import com.afrinuts.farmos.data.repository.DocumentRepository;
import com.afrinuts.farmos.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Displays an existing document detail layout inside the tablet Docs pane. */
public class DocumentDetailFragment extends Fragment {
    private static final String ARG_DOCUMENT_ID = "document_id";

    public static DocumentDetailFragment newInstance(String documentId) {
        DocumentDetailFragment fragment = new DocumentDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DOCUMENT_ID, documentId);
        fragment.setArguments(args);
        return fragment;
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.US);
    private String documentId;
    private long farmId = 1;
    private AppDatabase database;
    private DocumentRepository documentRepository;
    private CertificationRepository certificationRepository;
    private DocumentEntity document;
    private CertificationEntity certification;
    private SessionManager sessionManager;
    private TextView tvDocumentId, tvDocumentName, tvDocumentType, tvStatus, tvIssueStat, tvExpiryStat;
    private LinearLayout detailsRows;
    private ImageButton btnEdit, btnDelete;
    private MaterialButton btnOpenFile;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_document_detail_content, container, false);
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(28));
        view.setBackground(background);
        view.setClipToOutline(true);
        return view;
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        documentId = getArguments() == null ? null : getArguments().getString(ARG_DOCUMENT_ID);
        database = AppDatabase.getInstance(requireContext());
        sessionManager = new SessionManager(requireContext());
        bindViews(view);
        View scroll = ((ViewGroup) view).getChildAt(1);
        if (scroll instanceof android.widget.ScrollView) {
            android.widget.ScrollView scrollView = (android.widget.ScrollView) scroll;
            scrollView.setPadding(scrollView.getPaddingLeft(), dp(2), scrollView.getPaddingRight(),
                    scrollView.getPaddingBottom());
        }
        loadDocumentData();
    }

    private void bindViews(View view) {
        tvDocumentId = view.findViewById(R.id.tvDocumentId);
        tvDocumentName = view.findViewById(R.id.tvDocumentName);
        tvDocumentType = view.findViewById(R.id.tvDocumentType);
        tvStatus = view.findViewById(R.id.tvStatus);
        tvIssueStat = view.findViewById(R.id.tvIssueStat);
        tvExpiryStat = view.findViewById(R.id.tvExpiryStat);
        detailsRows = view.findViewById(R.id.detailsRows);
        btnEdit = view.findViewById(R.id.btnEdit);
        btnDelete = view.findViewById(R.id.btnDelete);
        btnOpenFile = view.findViewById(R.id.btnOpenFile);

        View btnBack = view.findViewById(R.id.btnBack);
        ViewGroup phoneHeader = (ViewGroup) btnBack.getParent();
        phoneHeader.removeView(btnEdit);
        phoneHeader.removeView(btnDelete);
        phoneHeader.setVisibility(View.GONE);
        moveHeaderIntoHero();
        styleKpis();
        styleTabletActions();
        btnEdit.setVisibility(sessionManager.canEditData() ? View.VISIBLE : View.GONE);
        btnDelete.setVisibility(sessionManager.canEditData() ? View.VISIBLE : View.GONE);
        btnEdit.setOnClickListener(v -> showEditDialog());
        btnDelete.setOnClickListener(v -> confirmDelete());
        btnOpenFile.setOnClickListener(v -> openDocumentFile());
    }

    private void moveHeaderIntoHero() {
        ViewGroup content = (ViewGroup) tvDocumentId.getParent();
        content.removeView(tvDocumentId);
        content.removeView(tvDocumentName);
        content.removeView(tvDocumentType);
        content.removeView(tvStatus);
        FrameLayout header = new FrameLayout(requireContext());
        LinearLayout stack = new LinearLayout(requireContext());
        stack.setOrientation(LinearLayout.VERTICAL);
        stack.setPadding(0, 0, dp(112), 0);
        header.addView(stack, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP | Gravity.START));
        stack.addView(tvDocumentId, new LinearLayout.LayoutParams(-1, -2));
        stack.addView(tvDocumentName, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout meta = new LinearLayout(requireContext());
        meta.setGravity(Gravity.CENTER_VERTICAL);
        meta.addView(tvDocumentType, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-2, -2);
        statusParams.setMarginStart(dp(8));
        tvStatus.setLayoutParams(statusParams);
        meta.addView(tvStatus);
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(-1, -2);
        metaParams.topMargin = dp(2);
        stack.addView(meta, metaParams);

        styleActionCircle(btnEdit, 0xFFF6F1E7, 0xFFDDC9A4, 0xFF1C1B1A);
        styleActionCircle(btnDelete, 0xFFFCE7E2, 0xFFE9B3A3, 0xFFB83D2A);
        FrameLayout.LayoutParams editParams = new FrameLayout.LayoutParams(dp(48), dp(48), Gravity.TOP | Gravity.END);
        editParams.rightMargin = dp(56);
        header.addView(btnEdit, editParams);
        header.addView(btnDelete, new FrameLayout.LayoutParams(dp(48), dp(48), Gravity.TOP | Gravity.END));
        content.addView(header, 0, new LinearLayout.LayoutParams(-1, -2));
        content.setPadding(content.getPaddingLeft(), content.getPaddingTop(), content.getPaddingRight(), 0);
        View hero = (View) content.getParent();
        ViewGroup.MarginLayoutParams heroParams = (ViewGroup.MarginLayoutParams) hero.getLayoutParams();
        heroParams.bottomMargin = 0;
        hero.setLayoutParams(heroParams);
    }

    private void styleActionCircle(ImageButton button, int fill, int stroke, int tint) {
        button.setImageResource(button == btnEdit ? R.drawable.ic_farm_edit : R.drawable.ic_delete);
        button.setColorFilter(tint);
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(fill);
        background.setStroke(dp(1), stroke);
        button.setBackground(background);
    }

    private void styleKpis() {
        ViewGroup row = (ViewGroup) tvIssueStat.getParent().getParent().getParent();
        if (row instanceof LinearLayout) ((LinearLayout) row).setGravity(Gravity.START);
        row.post(() -> {
            int gap = dp(12);
            int width = Math.min(dp(280), Math.max(dp(240), (row.getWidth() - gap) / 2));
            for (int i = 0; i < row.getChildCount(); i++) {
                View child = row.getChildAt(i);
                if (!(child instanceof CardView)) continue;
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) child.getLayoutParams();
                params.width = width;
                params.weight = 0;
                params.setMarginStart(i == 0 ? 0 : gap);
                params.setMarginEnd(0);
                child.setLayoutParams(params);
                if (i > 0) ((CardView) child).setCardBackgroundColor(0xFFF6F1E7);
            }
        });
    }

    private void styleTabletActions() {
        ViewGroup parent = (ViewGroup) btnOpenFile.getParent();
        int index = parent.indexOfChild(btnOpenFile);
        parent.removeView(btnOpenFile);
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        parent.addView(row, index, new LinearLayout.LayoutParams(-1, -2));

        MaterialButton edit = new MaterialButton(requireContext());
        edit.setText("Edit");
        edit.setAllCaps(false);
        edit.setTextSize(16);
        edit.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.manrope_bold));
        edit.setTextColor(0xFF1C1B1A);
        edit.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
        edit.setStrokeColor(android.content.res.ColorStateList.valueOf(0xFFE9E1CF));
        edit.setStrokeWidth(dp(2));
        edit.setCornerRadius(dp(999));
        edit.setInsetTop(0);
        edit.setInsetBottom(0);
        edit.setOnClickListener(v -> showEditDialog());
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(dp(128), dp(58));
        editParams.setMarginEnd(dp(8));
        row.addView(edit, editParams);

        btnOpenFile.setText("Open file");
        btnOpenFile.setAllCaps(false);
        btnOpenFile.setTextSize(16);
        btnOpenFile.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.manrope_bold));
        btnOpenFile.setTextColor(Color.WHITE);
        btnOpenFile.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF0E6B3C));
        btnOpenFile.setCornerRadius(dp(999));
        btnOpenFile.setInsetTop(0);
        btnOpenFile.setInsetBottom(0);
        row.addView(btnOpenFile, new LinearLayout.LayoutParams(dp(172), dp(58)));
        edit.setVisibility(sessionManager.canEditData() ? View.VISIBLE : View.GONE);
    }

    private void loadDocumentData() {
        if (documentId == null || documentId.isEmpty()) return;
        android.content.Context context = requireContext();
        executor.execute(() -> {
            try {
                farmId = database.farmDao().getFirstFarm().getId();
                documentRepository = new DocumentRepository(context, farmId);
                certificationRepository = new CertificationRepository(context, farmId);
                document = documentRepository.getDocumentByIdSync(documentId);
                certification = document == null || document.getCertificationId() == null ? null
                        : certificationRepository.getCertificationByIdSync(document.getCertificationId());
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    if (document == null) {
                        Toast.makeText(requireContext(), "Document not found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    displayDocumentData();
                });
            } catch (Exception error) {
                if (isAdded()) requireActivity().runOnUiThread(() -> {
                    if (isAdded()) Toast.makeText(requireContext(), "Error loading document", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void displayDocumentData() {
        tvDocumentId.setText(document.getDocumentId());
        tvDocumentName.setText(document.getDocumentName());
        tvDocumentType.setText(document.getDocumentTypeDisplay());
        String status = statusLabel();
        tvStatus.setText(status);
        styleStatus();
        tvIssueStat.setText(formatStatDate(document.getIssueDate()));
        tvExpiryStat.setText(formatStatDate(document.getExpiryDate()));
        populateDetailsRows(status);
        boolean hasFile = document.getFileUrl() != null && !document.getFileUrl().trim().isEmpty();
        btnOpenFile.setEnabled(hasFile);
        btnOpenFile.setAlpha(hasFile ? 1f : 0.45f);
        btnEdit.setVisibility(sessionManager.canEditData() ? View.VISIBLE : View.GONE);
        btnDelete.setVisibility(sessionManager.canEditData() ? View.VISIBLE : View.GONE);
    }

    private String statusLabel() {
        if (document.getStatus() == DocumentEntity.DocumentStatus.APPROVED) {
            if (document.isExpired()) return "Expired";
            if (document.isExpiringSoon()) return "Expiring soon";
            return "Valid";
        }
        return document.getStatusDisplay();
    }

    private void styleStatus() {
        int bg, fg;
        if ("Expired".equals(tvStatus.getText().toString())) { bg = 0xFFF8E4DD; fg = 0xFFB3402A; }
        else if ("Expiring soon".equals(tvStatus.getText().toString())) { bg = 0xFFF6ECD2; fg = 0xFF8A6414; }
        else if ("Valid".equals(tvStatus.getText().toString())) { bg = 0xFFE3F0E7; fg = 0xFF0A5530; }
        else { bg = 0xFFEFEAE0; fg = 0xFF6B675F; }
        GradientDrawable pill = new GradientDrawable();
        pill.setColor(bg);
        pill.setCornerRadius(dp(999));
        tvStatus.setBackground(pill);
        tvStatus.setTextColor(fg);
        tvStatus.setTextSize(12.5f);
        tvStatus.setPadding(dp(12), dp(6), dp(12), dp(6));
    }

    private void populateDetailsRows(String status) {
        detailsRows.removeAllViews();
        String certName = certification != null ? certification.getCertificationName()
                + " (" + certification.getCertificationId() + ")"
                : (document.getCertificationId() == null || document.getCertificationId().trim().isEmpty()
                ? "—" : document.getCertificationId());
        String url = document.getFileUrl() == null || document.getFileUrl().trim().isEmpty()
                ? "—" : document.getFileUrl().trim();
        String[] labels = {"Certification programme", "Document type", "Document name", "Issue date",
                "Expiry date", "File URL", "Status", "Created", "Updated"};
        String[] values = {certName, document.getDocumentTypeDisplay(), document.getDocumentName(),
                document.getFormattedIssueDate(), document.getFormattedExpiryDate(), url, status,
                metadataDate(document.getCreatedAt()), metadataDate(document.getUpdatedAt())};
        for (int i = 0; i < labels.length; i++) {
            final boolean fileLink = i == 5 && !"—".equals(url);
            boolean metadata = i >= 7;
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(11), 0, dp(11));
            if (fileLink) row.setOnClickListener(v -> openDocumentFile());
            TextView label = detailText(labels[i], metadata ? 13.5f : 14.5f,
                    metadata ? 0xFF8A857B : 0xFF59544C, R.font.manrope_bold);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, -2, 1f);
            row.addView(label, labelParams);
            TextView value = detailText(values[i], metadata ? 13.5f : 14.5f,
                    fileLink ? 0xFF0A5530 : metadata ? 0xFF59544C : 0xFF1C1B1A,
                    metadata ? R.font.manrope_semibold : R.font.manrope_bold);
            value.setGravity(Gravity.END);
            row.addView(value, new LinearLayout.LayoutParams(0, -2, 1f));
            detailsRows.addView(row, new LinearLayout.LayoutParams(-1, -2));
            if (i < labels.length - 1) {
                View divider = new View(requireContext());
                divider.setBackgroundColor(0xFFE9E1CF);
                detailsRows.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
            }
        }
    }

    private TextView detailText(String text, float size, int color, int font) {
        TextView view = new TextView(requireContext());
        view.setText(text);
        view.setTextColor(color);
        view.setTextSize(size);
        view.setTypeface(ResourcesCompat.getFont(requireContext(), font));
        view.setIncludeFontPadding(false);
        return view;
    }

    private String metadataDate(long timestamp) { return timestamp <= 0 ? "—" : dateFormat.format(timestamp); }
    private String formatStatDate(long timestamp) {
        return timestamp <= 0 ? "—" : new SimpleDateFormat("MMM d, yyyy", Locale.US).format(timestamp);
    }

    private void showEditDialog() {
        if (document == null || !sessionManager.canEditData()) return;
        EditDocumentDialog dialog = EditDocumentDialog.newInstance(documentId);
        dialog.setOnDocumentUpdatedListener(this::loadDocumentData);
        dialog.show(getParentFragmentManager(), "EditDocumentDialog");
    }

    private void confirmDelete() {
        if (document == null) return;
        if (!sessionManager.isOwner()) {
            Snackbar.make(requireView(), "Only owners can delete documents", Snackbar.LENGTH_SHORT).show();
            return;
        }
        new MaterialAlertDialogBuilder(requireContext()).setTitle("Delete Document")
                .setMessage("Are you sure you want to delete this document?")
                .setPositiveButton("Delete", (dialog, which) -> deleteDocument())
                .setNegativeButton("Cancel", null).show();
    }

    private void deleteDocument() {
        if (documentRepository == null || document == null) return;
        documentRepository.deleteDocument(document, new DocumentRepository.RepositoryCallback<Boolean>() {
            @Override public void onSuccess(Boolean result) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Document deleted", Toast.LENGTH_SHORT).show();
                    if (getActivity() instanceof ComplianceHubActivity)
                        ((ComplianceHubActivity) getActivity()).onDocumentDeleted(documentId);
                });
            }
            @Override public void onError(String error) {
                if (isAdded()) requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void openDocumentFile() {
        if (document == null || document.getFileUrl() == null || document.getFileUrl().trim().isEmpty()) {
            Toast.makeText(requireContext(), "No file is attached", Toast.LENGTH_SHORT).show();
            return;
        }
        String url = document.getFileUrl().trim();
        if (!url.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*")) url = "https://" + url;
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (ActivityNotFoundException e) { Toast.makeText(requireContext(), "No app can open this file", Toast.LENGTH_SHORT).show(); }
        catch (Exception e) { Toast.makeText(requireContext(), "Unable to open file", Toast.LENGTH_SHORT).show(); }
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override public void onDestroy() {
        super.onDestroy();
        if (documentRepository != null) documentRepository.shutdown();
        if (certificationRepository != null) certificationRepository.shutdown();
        executor.shutdown();
    }
}
