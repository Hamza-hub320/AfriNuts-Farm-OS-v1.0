package com.afrinuts.farmos.ui.compliance;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.ui.BaseHubActivity;
import com.afrinuts.farmos.ui.analytics.AnalyticsHubActivity;
import com.afrinuts.farmos.ui.farm.FarmHubActivity;
import com.afrinuts.farmos.ui.finance.FinanceHubActivity;
import com.afrinuts.farmos.ui.auth.LoginActivity;
import com.afrinuts.farmos.ui.settings.SettingsActivity;
import com.afrinuts.farmos.ui.users.UsersListActivity;
import com.afrinuts.farmos.ui.sync.SyncCenterActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.google.firebase.auth.FirebaseAuth;

public class ComplianceHubActivity extends BaseHubActivity
        implements CertificationListFragment.OnCertificationSelectedListener,
        ChecksFragment.OnCheckSelectedListener,
        DocumentListFragment.OnDocumentSelectedListener,
        DueExpiringFragment.OnDueSelectedListener {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private FloatingActionButton fabAdd;
    private CompliancePagerAdapter pagerAdapter;
    private View detailContainer;
    private View detailContentContainer;
    private View checkFiltersHost;
    private TextView detailEmptyState;
    private String selectedCertificationId;
    private int selectedCheckType = ChecksFragment.TYPE_NONE;
    private String selectedCheckId;
    private String selectedDocumentId;
    private String selectedDueType;
    private String selectedDueId;

    // Navigation items
    private LinearLayout navHome;
    private LinearLayout navFarm;
    private LinearLayout navFinance;
    private LinearLayout navAnalytics;
    private LinearLayout navCompliance;
    private ImageView navHomeIcon;
    private ImageView navFarmIcon;
    private ImageView navFinanceIcon;
    private ImageView navAnalyticsIcon;
    private ImageView navComplianceIcon;
    private TextView navHomeText;
    private TextView navFarmText;
    private TextView navFinanceText;
    private TextView navAnalyticsText;
    private TextView navComplianceText;

    // Track tabs
    private List<TabItem> tabs = new ArrayList<>();
    private int currentPosition = 0;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_compliance_hub;
    }

    @Override
    protected int getCurrentNavItem() {
        return 4; // Compliance is index 4
    }

    @Override
    protected void onHubCreate(Bundle savedInstanceState) {
        setupTabs();
        initViews();
        setupViewPager();
        setupFab();
        setupBottomNavigation();
    }

    private void setupTabs() {
        tabs.clear();
        for (String key : com.afrinuts.farmos.utils.AccessMap.hubTabs(4, sessionManager.getUserRole())) {
            switch (key) {
                case "PROGRAMMES": tabs.add(new TabItem("Programs", R.drawable.ic_certified, TabType.PROGRAMMES)); break;
                case "CHECKS": tabs.add(new TabItem("Checks", R.drawable.ic_audit, TabType.CHECKS)); break;
                case "DOCUMENTS": tabs.add(new TabItem("Docs", R.drawable.ic_document, TabType.DOCUMENTS)); break;
                case "DUE_EXPIRING": tabs.add(new TabItem("Due", R.drawable.ic_progress_clock, TabType.DUE_EXPIRING)); break;
                default: throw new IllegalStateException("Unknown Compliance tab: " + key);
            }
        }
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        fabAdd = findViewById(R.id.fabAdd);
        detailContainer = findViewById(R.id.detailContainer);
        detailContentContainer = findViewById(R.id.detailContentContainer);
        checkFiltersHost = findViewById(R.id.checkFiltersHost);
        detailEmptyState = findViewById(R.id.detailEmptyState);
        View profile = findViewById(R.id.complianceProfileButton);
        if (profile instanceof TextView) ((TextView) profile).setText(new com.afrinuts.farmos.data.local.entity.UserEntity(sessionManager.getUserName(), null, null, null, null).getInitials());
        if (profile != null) profile.setOnClickListener(v -> showAccountMenu(profile));
        TextView complianceDate = findViewById(R.id.complianceDateText);
        if (complianceDate != null) {
            complianceDate.setText(new SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(new Date()));
        }

        // Bottom navigation items
        navHome = findViewById(R.id.navHome);
        navFarm = findViewById(R.id.navFarm);
        navFinance = findViewById(R.id.navFinance);
        navAnalytics = findViewById(R.id.navAnalytics);
        navCompliance = findViewById(R.id.navCompliance);

        navHomeIcon = findViewById(R.id.navHomeIcon);
        navFarmIcon = findViewById(R.id.navFarmIcon);
        navFinanceIcon = findViewById(R.id.navFinanceIcon);
        navAnalyticsIcon = findViewById(R.id.navAnalyticsIcon);
        navComplianceIcon = findViewById(R.id.navComplianceIcon);

        navHomeText = findViewById(R.id.navHomeText);
        navFarmText = findViewById(R.id.navFarmText);
        navFinanceText = findViewById(R.id.navFinanceText);
        navAnalyticsText = findViewById(R.id.navAnalyticsText);
        navComplianceText = findViewById(R.id.navComplianceText);

        fabAdd.hide();
    }

    private void showAccountMenu(View anchor) {
        PopupWindow popup = new PopupWindow(this);
        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(dp(8), dp(8), dp(8), dp(8));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(20));
        bg.setStroke(dp(1), 0xFFE9E1CF);
        menu.setBackground(bg);
        if (sessionManager.canManageUsers()) addMenuRow(menu, popup, "Team", R.drawable.ic_person_add, () ->
                startActivity(new Intent(this, UsersListActivity.class)));
        addMenuRow(menu, popup, "Settings", R.drawable.ic_settings, () -> startActivity(new Intent(this, SettingsActivity.class)));
        if (sessionManager.isOwner() || sessionManager.isCEO() || sessionManager.isManager())
            addMenuRow(menu, popup, "Sync Center", R.drawable.ic_sync, () ->
                    startActivity(new Intent(this, SyncCenterActivity.class)));
        View divider = new View(this);
        divider.setBackgroundColor(0xFFE9E1CF);
        menu.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        addMenuRow(menu, popup, "Logout", R.drawable.ic_logout, () -> com.afrinuts.farmos.ui.LogoutConfirmationDialog.show(this, () -> {
                    sessionManager.logout();
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }));
        popup.setContentView(menu);
        popup.setWidth(dp(196));
        popup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        popup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        popup.setFocusable(true);
        popup.setElevation(dp(8));
        popup.showAsDropDown(anchor, -dp(152), dp(8));
    }

    private void addMenuRow(LinearLayout menu, PopupWindow popup, String title, int iconRes, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), 0, dp(12), 0);
        row.setBackground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0xFFEFEAE2),
                new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT), null));
        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter("Logout".equals(title) ? 0xFFB3402A : 0xFF1C1B1A);
        row.addView(icon, new LinearLayout.LayoutParams(dp(18), dp(18)));
        TextView text = new TextView(this);
        text.setText(title);
        text.setTextColor("Logout".equals(title) ? 0xFFB3402A : 0xFF1C1B1A);
        text.setTextSize(15);
        text.setTypeface(Typeface.create(ResourcesCompat.getFont(this, R.font.manrope_medium), Typeface.NORMAL));
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, -2, 1);
        textParams.setMarginStart(dp(12));
        row.addView(text, textParams);
        menu.addView(row, new LinearLayout.LayoutParams(-1, dp(48)));
        row.setOnClickListener(v -> { popup.dismiss(); action.run(); });
    }

    private void setupViewPager() {
        pagerAdapter = new CompliancePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(1);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            TabItem tabItem = tabs.get(position);
            tab.setText(tabItem.title);
        }).attach();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentPosition = position;
                updateFabForTab(position);
                updateCheckFiltersLayout(position);
                if (detailContainer != null) {
                    if (tabs.get(position).type == TabType.PROGRAMMES) {
                        if (selectedCertificationId != null) {
                            showCertificationDetail(selectedCertificationId);
                        } else {
                            showDetailEmptyState("Choose a programme to view its details.");
                        }
                    } else if (tabs.get(position).type == TabType.CHECKS) {
                        if (selectedCheckType == ChecksFragment.TYPE_AUDIT && selectedCheckId != null) {
                            showAuditDetail(selectedCheckId);
                        } else if (selectedCheckType == ChecksFragment.TYPE_INSPECTION && selectedCheckId != null) {
                            showInspectionDetail(selectedCheckId);
                        } else if (selectedCheckType == ChecksFragment.TYPE_VERIFICATION && selectedCheckId != null) {
                            showVerificationDetail(selectedCheckId);
                        } else {
                            showDetailEmptyState("Choose a check to view its details.");
                        }
                    } else if (tabs.get(position).type == TabType.DOCUMENTS) {
                        if (selectedDocumentId != null) showDocumentDetail(selectedDocumentId);
                        else showDetailEmptyState("Choose a document to view its details.");
                    } else if (tabs.get(position).type == TabType.DUE_EXPIRING) {
                        if (selectedDueType != null && selectedDueId != null) {
                            showDueRecordDetail(selectedDueType, selectedDueId);
                        } else {
                            showDetailEmptyState("Choose a due item to view its details.");
                        }
                    } else {
                        showDetailEmptyState("Choose a programme or check to view its details.");
                    }
                }
            }
        });
    }

    private void updateCheckFiltersLayout(int position) {
        if (checkFiltersHost == null || position >= tabs.size()) return;
        boolean checksSelected = tabs.get(position).type == TabType.CHECKS;
        checkFiltersHost.setVisibility(checksSelected ? View.VISIBLE : View.GONE);

        ViewGroup.LayoutParams pagerRawParams = viewPager.getLayoutParams();
        if (pagerRawParams instanceof android.widget.LinearLayout.LayoutParams) {
            android.widget.LinearLayout.LayoutParams pagerParams =
                    (android.widget.LinearLayout.LayoutParams) pagerRawParams;
            pagerParams.topMargin = dp(checksSelected ? 56 : 0);
            viewPager.setLayoutParams(pagerParams);
        }
        ViewGroup.LayoutParams detailRawParams = detailContainer.getLayoutParams();
        if (detailRawParams instanceof android.widget.FrameLayout.LayoutParams) {
            android.widget.FrameLayout.LayoutParams detailParams =
                    (android.widget.FrameLayout.LayoutParams) detailRawParams;
            detailParams.topMargin = dp(checksSelected ? 256 : 200);
            detailContainer.setLayoutParams(detailParams);
        }
    }

    @Override
    public void onCertificationSelected(String certificationId) {
        if (detailContainer == null || certificationId == null || certificationId.isEmpty()) return;
        selectedCertificationId = certificationId;
        if (currentPosition < tabs.size() && tabs.get(currentPosition).type == TabType.PROGRAMMES) {
            showCertificationDetail(certificationId);
        }
    }

    @Override
    public void onCheckSelected(int type, String checkId) {
        selectedCheckType = type;
        selectedCheckId = checkId;
        if (detailContainer == null || currentPosition >= tabs.size()
                || tabs.get(currentPosition).type != TabType.CHECKS) return;
        if (type == ChecksFragment.TYPE_AUDIT && checkId != null && !checkId.isEmpty()) {
            showAuditDetail(checkId);
        } else if (type == ChecksFragment.TYPE_INSPECTION && checkId != null && !checkId.isEmpty()) {
            showInspectionDetail(checkId);
        } else if (type == ChecksFragment.TYPE_VERIFICATION && checkId != null && !checkId.isEmpty()) {
            showVerificationDetail(checkId);
        } else {
            showDetailEmptyState("Choose a check to view its details.");
        }
    }

    @Override
    public void onDocumentSelected(String documentId) {
        selectedDocumentId = documentId;
        if (detailContentContainer == null || currentPosition >= tabs.size()
                || tabs.get(currentPosition).type != TabType.DOCUMENTS
                || documentId == null || documentId.isEmpty()) {
            if (currentPosition < tabs.size() && tabs.get(currentPosition).type == TabType.DOCUMENTS)
                showDetailEmptyState("Choose a document to view its details.");
            return;
        }
        showDocumentDetail(documentId);
    }

    @Override
    public void onDueSelected(String recordType, String recordId) {
        selectedDueType = recordType;
        selectedDueId = recordId;
        if (detailContainer == null || currentPosition >= tabs.size()
                || tabs.get(currentPosition).type != TabType.DUE_EXPIRING
                || recordType == null || recordType.isEmpty()
                || recordId == null || recordId.isEmpty()) {
            if (currentPosition < tabs.size() && tabs.get(currentPosition).type == TabType.DUE_EXPIRING) {
                showDetailEmptyState("Choose a due item to view its details.");
            }
            return;
        }
        showDueRecordDetail(recordType, recordId);
    }

    private void showDueRecordDetail(String recordType, String recordId) {
        switch (recordType) {
            case "certification": showCertificationDetail(recordId); break;
            case "document": showDocumentDetail(recordId); break;
            case "audit": showAuditDetail(recordId); break;
            case "inspection": showInspectionDetail(recordId); break;
            default: showDetailEmptyState("Choose a due item to view its details."); break;
        }
    }

    public void onDocumentDeleted(String documentId) {
        if (documentId != null && documentId.equals(selectedDocumentId)) selectedDocumentId = null;
        if (currentPosition < tabs.size() && tabs.get(currentPosition).type == TabType.DOCUMENTS)
            showDetailEmptyState("Choose a document to view its details.");
    }

    private void showAuditDetail(String auditId) {
        if (detailContentContainer == null) return;
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.detailContentContainer);
        if (current instanceof AuditDetailFragment && current.getArguments() != null
                && auditId.equals(current.getArguments().getString("audit_id"))) {
            detailEmptyState.setVisibility(View.GONE);
            return;
        }
        detailEmptyState.setVisibility(View.GONE);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.detailContentContainer, AuditDetailFragment.newInstance(auditId),
                        "compliance_audit_detail")
                .commit();
    }

    private void showInspectionDetail(String inspectionId) {
        if (detailContentContainer == null) return;
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.detailContentContainer);
        if (current instanceof InspectionDetailFragment && current.getArguments() != null
                && inspectionId.equals(current.getArguments().getString("inspection_id"))) {
            detailEmptyState.setVisibility(View.GONE);
            return;
        }
        detailEmptyState.setVisibility(View.GONE);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.detailContentContainer, InspectionDetailFragment.newInstance(inspectionId),
                        "compliance_inspection_detail")
                .commit();
    }

    private void showVerificationDetail(String verificationId) {
        if (detailContentContainer == null) return;
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.detailContentContainer);
        if (current instanceof VerificationDetailFragment && current.getArguments() != null
                && verificationId.equals(current.getArguments().getString("verification_id"))) {
            detailEmptyState.setVisibility(View.GONE);
            return;
        }
        detailEmptyState.setVisibility(View.GONE);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.detailContentContainer, VerificationDetailFragment.newInstance(verificationId),
                        "compliance_verification_detail")
                .commit();
    }

    private void showDocumentDetail(String documentId) {
        if (detailContentContainer == null) return;
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.detailContentContainer);
        if (current instanceof DocumentDetailFragment && current.getArguments() != null
                && documentId.equals(current.getArguments().getString("document_id"))) {
            detailEmptyState.setVisibility(View.GONE);
            return;
        }
        detailEmptyState.setVisibility(View.GONE);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.detailContentContainer, DocumentDetailFragment.newInstance(documentId),
                        "compliance_document_detail")
                .commit();
    }

    private void showCertificationDetail(String certificationId) {
        if (detailContentContainer == null) return;
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.detailContentContainer);
        if (current instanceof CertificationDetailFragment
                && certificationId.equals(current.getArguments().getString("certification_id"))) {
            detailEmptyState.setVisibility(View.GONE);
            return;
        }
        detailEmptyState.setVisibility(View.GONE);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.detailContentContainer,
                        CertificationDetailFragment.newInstance(certificationId),
                        "compliance_certification_detail")
                .commit();
    }

    private void showDetailEmptyState(String message) {
        if (detailContentContainer == null || detailEmptyState == null) return;
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.detailContentContainer);
        if (current != null) {
            getSupportFragmentManager().beginTransaction().remove(current).commit();
        }
        detailEmptyState.setText(message);
        detailEmptyState.setVisibility(View.VISIBLE);
    }

    private void updateFabForTab(int position) {
        if (position >= tabs.size()) {
            fabAdd.hide();
            return;
        }

        TabItem tabItem = tabs.get(position);

        // Keep the Docs FAB 16dp above the bottom navigation pill.
        android.view.ViewGroup.LayoutParams rawParams = fabAdd.getLayoutParams();
        if (rawParams instanceof android.widget.FrameLayout.LayoutParams) {
            android.widget.FrameLayout.LayoutParams fabParams =
                    (android.widget.FrameLayout.LayoutParams) rawParams;
            int bottomMargin = tabItem.type == TabType.DOCUMENTS ? 92 : 126;
            // Keep the tablet Checks FAB below the list card so it cannot cover an audit row.
            if (detailContainer != null && (tabItem.type == TabType.CHECKS
                    || tabItem.type == TabType.PROGRAMMES)) bottomMargin = 92;
            fabParams.bottomMargin = dp(bottomMargin);
            fabAdd.setLayoutParams(fabParams);
        }

        // Clear any existing listeners to prevent duplicates
        fabAdd.setOnClickListener(null);
        fabAdd.setImageResource(R.drawable.ic_add);

        // Set click listener based on tab type
        switch (tabItem.type) {
            case PROGRAMMES:
                if (!com.afrinuts.farmos.utils.AccessMap.canUseFab("ADD_CERTIFICATION", sessionManager.getUserRole())) { fabAdd.hide(); break; }
                fabAdd.setOnClickListener(v -> {
                    Fragment fragment = getSupportFragmentManager()
                            .findFragmentByTag("f" + viewPager.getCurrentItem());
                    if (fragment instanceof CertificationListFragment) {
                        ((CertificationListFragment) fragment).showAddCertificationDialog();
                    } else {
                        Toast.makeText(this, "Please switch to Certifications tab", Toast.LENGTH_SHORT).show();
                    }
                });
                fabAdd.show();
                break;

            case CHECKS:
                if (!com.afrinuts.farmos.utils.AccessMap.canUseFab("ADD_AUDIT", sessionManager.getUserRole())) { fabAdd.hide(); break; }
                fabAdd.setOnClickListener(v -> showCheckChooser());
                fabAdd.show();
                break;

            case DOCUMENTS:
                if (!com.afrinuts.farmos.utils.AccessMap.canUseFab("ADD_DOCUMENT", sessionManager.getUserRole())) { fabAdd.hide(); break; }
                fabAdd.setOnClickListener(v -> {
                    Fragment fragment = getSupportFragmentManager()
                            .findFragmentByTag("f" + viewPager.getCurrentItem());
                    if (fragment instanceof DocumentListFragment) {
                        ((DocumentListFragment) fragment).showAddDocumentDialog();
                    }
                });
                fabAdd.show();
                break;

            default:
                fabAdd.hide();
                break;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void showCheckChooser() {
        new CheckChooserDialog().show(getSupportFragmentManager(), "AddCheckChooserDialog");
    }

    void showCheckChoice(int which) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());
        if (!(fragment instanceof ChecksFragment)) return;
        ChecksFragment checks = (ChecksFragment) fragment;
        if (which == 0) checks.showAddAuditDialog();
        else if (which == 1) checks.showAddInspectionDialog();
        else if (which == 2) checks.showAddVerificationDialog();
    }

    private void setupFab() {
        if (tabs.isEmpty()) {
            fabAdd.hide();
            return;
        }

        viewPager.post(() -> {
            currentPosition = viewPager.getCurrentItem();
            updateFabForTab(currentPosition);
        });
    }

    private void setupBottomNavigation() {
        navHome.setOnClickListener(v -> {
            if (!com.afrinuts.farmos.utils.AccessMap.canOpenHub(0, sessionManager.getUserRole())) {
                Toast.makeText(this, "This section is not available for your role", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, com.afrinuts.farmos.MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });

        navFarm.setOnClickListener(v -> {
            if (!com.afrinuts.farmos.utils.AccessMap.canOpenHub(1, sessionManager.getUserRole())) {
                Toast.makeText(this, "This section is not available for your role", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, FarmHubActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });

        navFinance.setOnClickListener(v -> {
            if (!com.afrinuts.farmos.utils.AccessMap.canOpenHub(2, sessionManager.getUserRole())) {
                Toast.makeText(this, "This section is not available for your role", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, FinanceHubActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });

        navAnalytics.setOnClickListener(v -> {
            if (!com.afrinuts.farmos.utils.AccessMap.canOpenHub(3, sessionManager.getUserRole())) {
                Toast.makeText(this, "This section is not available for your role", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, AnalyticsHubActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });

        navCompliance.setOnClickListener(v -> {
            // Already on Compliance, refresh
            finish();
            startActivity(getIntent());
        });

        highlightNavItem(4);
    }

    private void highlightNavItem(int selectedIndex) {
        updateNavSelection(selectedIndex);
    }

    private void resetNavItem(ImageView icon, TextView text, int color) {
        icon.setColorFilter(color);
        text.setTextColor(color);
        text.setAlpha(0.6f);
    }

    private void setActiveNavItem(ImageView icon, TextView text, int color) {
        icon.setColorFilter(color);
        text.setTextColor(color);
        text.setAlpha(1.0f);
    }

    private int getThemeColor(int attr) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    @Override
    protected boolean checkRoleAccess() {
        return com.afrinuts.farmos.utils.AccessMap.canOpenScreen(
                getClass().getSimpleName(), sessionManager.getUserRole());
    }

    @Override
    protected String getAccessDeniedMessage() {
        return "Compliance is only accessible to Owners, Managers and CEO";
    }

    // Tab types
    private enum TabType { PROGRAMMES, CHECKS, DOCUMENTS, DUE_EXPIRING }

    // Tab item class
    private static class TabItem {
        String title;
        int iconRes;
        TabType type;

        TabItem(String title, int iconRes, TabType type) {
            this.title = title;
            this.iconRes = iconRes;
            this.type = type;
        }
    }

    // ViewPager Adapter
    private class CompliancePagerAdapter extends FragmentStateAdapter {

        public CompliancePagerAdapter(@NonNull ComplianceHubActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            TabItem tab = tabs.get(position);
            switch (tab.type) {
                case PROGRAMMES:
                    return new CertificationListFragment();
                case CHECKS:
                    return new ChecksFragment();
                case DOCUMENTS:
                    return new DocumentListFragment();
                case DUE_EXPIRING:
                    return new DueExpiringFragment();
                default:
                    return new CertificationListFragment();
            }
        }

        @Override
        public int getItemCount() {
            return tabs.size();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
    }
}
