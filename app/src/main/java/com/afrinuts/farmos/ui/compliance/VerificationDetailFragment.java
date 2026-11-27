package com.afrinuts.farmos.ui.compliance;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.CertificationEntity;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.data.local.entity.VerificationEntity;
import com.afrinuts.farmos.data.repository.CertificationRepository;
import com.afrinuts.farmos.data.repository.VerificationRepository;
import com.afrinuts.farmos.utils.DateFormatter;
import com.afrinuts.farmos.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Renders verification detail content directly in the Compliance tablet pane. */
public class VerificationDetailFragment extends Fragment {

    private static final String ARG_VERIFICATION_ID = "verification_id";

    public static VerificationDetailFragment newInstance(String verificationId) {
        VerificationDetailFragment fragment = new VerificationDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_VERIFICATION_ID, verificationId);
        fragment.setArguments(args);
        return fragment;
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private String verificationId;
    private long farmId = 1;
    private AppDatabase database;
    private VerificationRepository verificationRepository;
    private CertificationRepository certificationRepository;
    private VerificationEntity verification;
    private CertificationEntity certification;
    private boolean canEdit;

    private TextView tvVerificationId, tvSuperseded, tvVerificationType, tvHeroSubtitle, tvStatus;
    private TextView tvStatDate, tvStatCount, tvCertification, tvVerificationDate, tvVerifiedBy;
    private TextView tvEvidence, tvComment, tvCreatedAt, tvValidated, tvUpdatedAt;
    private MaterialButton btnCorrect;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_verification_detail_content, container, false);
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(28));
        view.setBackground(background);
        view.setClipToOutline(true);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        verificationId = getArguments() == null ? null : getArguments().getString(ARG_VERIFICATION_ID);
        database = AppDatabase.getInstance(requireContext());
        canEdit = new SessionManager(requireContext()).canEditData();
        bindViews(view);
        View scroll = ((ViewGroup) view).getChildAt(1);
        if (scroll instanceof android.widget.ScrollView) {
            android.widget.ScrollView scrollView = (android.widget.ScrollView) scroll;
            scrollView.setPadding(scrollView.getPaddingLeft(), dp(2),
                    scrollView.getPaddingRight(), scrollView.getPaddingBottom());
        }
        loadVerificationData();
    }

    private void bindViews(View view) {
        tvVerificationId = view.findViewById(R.id.tvVerificationId);
        tvSuperseded = view.findViewById(R.id.tvSuperseded);
        tvVerificationType = view.findViewById(R.id.tvVerificationType);
        tvHeroSubtitle = view.findViewById(R.id.tvHeroSubtitle);
        tvStatus = view.findViewById(R.id.tvStatus);
        tvStatDate = view.findViewById(R.id.tvStatDate);
        tvStatCount = view.findViewById(R.id.tvStatCount);
        tvCertification = view.findViewById(R.id.tvCertification);
        tvVerificationDate = view.findViewById(R.id.tvVerificationDate);
        tvVerifiedBy = view.findViewById(R.id.tvVerifiedBy);
        tvEvidence = view.findViewById(R.id.tvEvidence);
        tvComment = view.findViewById(R.id.tvComment);
        tvCreatedAt = view.findViewById(R.id.tvCreatedAt);
        tvValidated = view.findViewById(R.id.tvValidated);
        tvUpdatedAt = view.findViewById(R.id.tvUpdatedAt);
        btnCorrect = view.findViewById(R.id.btnCorrect);

        addDetailDividers();
        View btnBack = view.findViewById(R.id.btnBack);
        View btnEdit = view.findViewById(R.id.btnEdit);
        ViewGroup phoneHeader = (ViewGroup) btnBack.getParent();
        phoneHeader.removeView(btnEdit);
        phoneHeader.setVisibility(View.GONE);
        moveHeaderIntoHero((ImageButton) btnEdit);
        styleTabletKpis(tvStatDate);
        styleTabletText(view);
        styleTabletActions(btnCorrect);
        btnEdit.setVisibility(canEdit ? View.VISIBLE : View.GONE);
        btnCorrect.setVisibility(canEdit ? View.VISIBLE : View.GONE);
        btnEdit.setOnClickListener(v -> openCorrectionDialog());
        btnCorrect.setOnClickListener(v -> openCorrectionDialog());
    }

    private void moveHeaderIntoHero(ImageButton editButton) {
        ViewGroup heroContent = (ViewGroup) tvVerificationId.getParent();
        heroContent.removeView(tvVerificationId);
        heroContent.removeView(tvSuperseded);
        heroContent.removeView(tvVerificationType);
        heroContent.removeView(tvHeroSubtitle);
        heroContent.removeView(tvStatus);

        FrameLayout header = new FrameLayout(requireContext());
        LinearLayout textStack = new LinearLayout(requireContext());
        textStack.setOrientation(LinearLayout.VERTICAL);
        textStack.setPadding(0, 0, dp(62), 0);
        header.addView(textStack, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP | Gravity.START));
        tvVerificationId.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        textStack.addView(tvVerificationId);
        tvSuperseded.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        textStack.addView(tvSuperseded);
        tvVerificationType.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        textStack.addView(tvVerificationType);
        LinearLayout metadataRow = new LinearLayout(requireContext());
        metadataRow.setGravity(Gravity.CENTER_VERTICAL);
        tvHeroSubtitle.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
        metadataRow.addView(tvHeroSubtitle);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-2, -2);
        statusParams.setMarginStart(dp(8));
        tvStatus.setLayoutParams(statusParams);
        tvStatus.setSingleLine(true);
        metadataRow.addView(tvStatus);
        LinearLayout.LayoutParams metadataParams = new LinearLayout.LayoutParams(-1, -2);
        metadataParams.topMargin = dp(2);
        textStack.addView(metadataRow, metadataParams);

        editButton.setImageResource(R.drawable.ic_farm_edit);
        editButton.setColorFilter(0xFF1C1B1A);
        GradientDrawable buttonBackground = new GradientDrawable();
        buttonBackground.setShape(GradientDrawable.OVAL);
        buttonBackground.setColor(0xFFF6F1E7);
        buttonBackground.setStroke(dp(1), 0xFFDDC9A4);
        editButton.setBackground(buttonBackground);
        header.addView(editButton, new FrameLayout.LayoutParams(dp(48), dp(48), Gravity.TOP | Gravity.END));
        heroContent.addView(header, 0, new LinearLayout.LayoutParams(-1, -2));
        heroContent.setPadding(heroContent.getPaddingLeft(), heroContent.getPaddingTop(),
                heroContent.getPaddingRight(), dp(4));
        View heroCard = (View) heroContent.getParent();
        ViewGroup.LayoutParams cardParams = heroCard.getLayoutParams();
        if (cardParams instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) cardParams).bottomMargin = dp(4);
            heroCard.setLayoutParams(cardParams);
        }
    }

    private void styleTabletKpis(TextView firstStat) {
        ViewGroup row = (ViewGroup) firstStat.getParent().getParent().getParent();
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
                ViewGroup content = (ViewGroup) ((CardView) child).getChildAt(0);
                if (content != null && content.getChildCount() > 1) {
                    View label = content.getChildAt(0), value = content.getChildAt(1);
                    if (label instanceof TextView) ((TextView) label).setTextSize(14);
                    if (value instanceof TextView) ((TextView) value).setTextSize(21);
                }
            }
        });
    }

    private void styleTabletText(View view) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            if (text.getCurrentTextColor() == 0xFF6B675F) text.setTextColor(0xFF59544C);
            else if (view.getId() != R.id.tvVerificationId && text.getCurrentTextColor() == 0xFF0E6B3C) {
                text.setTextColor(0xFF151413);
            } else if (view.getId() != R.id.tvVerificationId && text.getCurrentTextColor() == 0xFF1C1B1A) {
                text.setTextColor(0xFF151413);
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) styleTabletText(group.getChildAt(i));
        }
        if (view.getId() == R.id.tvCreatedAt || view.getId() == R.id.tvValidated
                || view.getId() == R.id.tvUpdatedAt) view.setAlpha(1f);
    }

    private void styleTabletActions(MaterialButton correctButton) {
        LinearLayout content = (LinearLayout) correctButton.getParent();
        int buttonIndex = content.indexOfChild(correctButton);
        content.removeView(correctButton);
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        content.addView(row, buttonIndex, new LinearLayout.LayoutParams(-1, -2));
        MaterialButton editAction = new MaterialButton(requireContext());
        editAction.setText("Edit");
        editAction.setTextColor(0xFF1C1B1A);
        editAction.setAllCaps(false);
        editAction.setTextSize(16);
        editAction.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.manrope_bold));
        editAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
        editAction.setStrokeColor(android.content.res.ColorStateList.valueOf(0xFFE9E1CF));
        editAction.setStrokeWidth(dp(2));
        editAction.setCornerRadius(dp(999));
        editAction.setInsetTop(0);
        editAction.setInsetBottom(0);
        editAction.setOnClickListener(v -> openCorrectionDialog());
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(dp(128), dp(58));
        editParams.setMarginEnd(dp(8));
        row.addView(editAction, editParams);
        correctButton.setLayoutParams(new LinearLayout.LayoutParams(dp(172), dp(58)));
        row.addView(correctButton);
        editAction.setVisibility(canEdit ? View.VISIBLE : View.GONE);
    }

    private void addDetailDividers() {
        TextView[] values = {tvCertification, tvVerificationDate, tvVerifiedBy, tvEvidence,
                tvComment, tvCreatedAt, tvValidated, tvUpdatedAt};
        for (TextView value : values) {
            View row = (View) value.getParent();
            if (!(row.getParent() instanceof LinearLayout)) continue;
            LinearLayout details = (LinearLayout) row.getParent();
            View divider = new View(requireContext());
            divider.setBackgroundColor(0xFFE9E1CF);
            details.addView(divider, details.indexOfChild(row) + 1,
                    new LinearLayout.LayoutParams(-1, dp(1)));
        }
    }

    private void loadVerificationData() {
        if (verificationId == null || verificationId.isEmpty()) return;
        android.content.Context appContext = requireContext().getApplicationContext();
        executor.execute(() -> {
            try {
                farmId = database.farmDao().getFirstFarm().getId();
                if (verificationRepository == null) verificationRepository = new VerificationRepository(appContext, farmId);
                if (certificationRepository == null) certificationRepository = new CertificationRepository(appContext, farmId);
                verification = verificationRepository.getVerificationByIdSync(verificationId);
                certification = verification == null || verification.getCertificationId() == null ? null
                        : certificationRepository.getCertificationByIdSync(verification.getCertificationId());
                String validatorName = verification == null || verification.getValidatedAt() == null
                        ? null : resolveValidatorName(verification.getValidatedBy());
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    if (verification == null) {
                        Toast.makeText(requireContext(), "Verification not found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    displayVerificationData(validatorName);
                    loadSupersededState();
                });
            } catch (Exception e) {
                if (isAdded()) requireActivity().runOnUiThread(() -> {
                    if (isAdded()) Toast.makeText(requireContext(), "Error loading verification", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private String resolveValidatorName(String uid) {
        if (uid == null || uid.isEmpty()) return uid;
        try {
            UserEntity user = database.userDao().getUserByUid(uid);
            String name = user == null ? null : user.getName();
            return name == null || name.trim().isEmpty() ? uid : name;
        } catch (Exception ignored) { return uid; }
    }

    private void displayVerificationData(String validatorName) {
        tvVerificationId.setText(verification.getVerificationId());
        tvVerificationType.setText(verification.getVerificationTypeDisplay());
        tvStatus.setText(verification.getStatusDisplay().toUpperCase(Locale.ROOT));
        updateStatusStyle();
        if (certification != null) {
            tvHeroSubtitle.setText(certification.getCertificationName() + " · " + certification.getCertificationBody());
            tvCertification.setText(certification.getCertificationName() + " (" + certification.getCertificationId() + ")");
        } else {
            tvHeroSubtitle.setText(verification.getCertificationId() == null
                    ? verification.getVerificationTypeDisplay() : verification.getCertificationId());
            tvCertification.setText(verification.getCertificationId() == null ? "Not specified" : verification.getCertificationId());
        }
        tvVerificationDate.setText(verification.getFormattedVerificationDate());
        tvStatDate.setText(verification.getFormattedVerificationDate());
        List<String> evidence = verification.getEvidence();
        tvStatCount.setText((evidence == null ? 0 : evidence.size())
                + ((evidence == null ? 0 : evidence.size()) == 1 ? " file" : " files"));
        tvVerifiedBy.setText(verification.getVerifiedBy() == null ? "Not specified" : verification.getVerifiedBy());
        if (evidence != null && !evidence.isEmpty()) {
            tvEvidence.setText(String.join("\n", evidence));
            tvEvidence.setVisibility(View.VISIBLE);
        } else {
            tvEvidence.setText("—");
            tvEvidence.setVisibility(View.VISIBLE);
        }
        tvComment.setText(verification.getComment() == null ? "No comments" : verification.getComment());
        tvCreatedAt.setText(dateFormat.format(verification.getCreatedAt()));
        tvUpdatedAt.setText(dateFormat.format(verification.getUpdatedAt()));
        tvValidated.setText(verification.getValidatedAt() == null ? "—"
                : "Validated by " + validatorName + " on "
                + DateFormatter.formatDateOrDash(verification.getValidatedAt()));
    }

    private void updateStatusStyle() {
        int background, foreground;
        switch (verification.getStatus()) {
            case VERIFIED: background = 0xFFE3F0E7; foreground = 0xFF0A5530; break;
            case REJECTED: background = 0xFFFCE8E6; foreground = 0xFFA12820; break;
            case PENDING:
            default: background = 0xFFF3EAD8; foreground = 0xFF59544C; break;
        }
        GradientDrawable pill = new GradientDrawable();
        pill.setColor(background);
        pill.setCornerRadius(dp(999));
        tvStatus.setBackground(pill);
        tvStatus.setTextColor(foreground);
    }

    private void loadSupersededState() {
        VerificationEntity current = verification;
        if (current == null) return;
        executor.execute(() -> {
            String supersedingId = database.verificationDao().getSupersedingRecordId(current.getVerificationId());
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (!isAdded() || verification != current) return;
                if (supersedingId != null && !supersedingId.isEmpty()) {
                    tvSuperseded.setText("Superseded by " + supersedingId);
                    tvSuperseded.setVisibility(View.VISIBLE);
                } else tvSuperseded.setVisibility(View.GONE);
            });
        });
    }

    private void openCorrectionDialog() {
        if (verification == null || !canEdit) return;
        AddVerificationDialog correction = AddVerificationDialog.newInstanceCorrection(farmId,
                verification.getVerificationId());
        correction.setOnVerificationAddedListener(this::loadVerificationData);
        correction.show(getParentFragmentManager(), "AddVerificationCorrectionDialog");
    }

    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (verificationRepository != null) verificationRepository.shutdown();
        if (certificationRepository != null) certificationRepository.shutdown();
        executor.shutdown();
    }
}
