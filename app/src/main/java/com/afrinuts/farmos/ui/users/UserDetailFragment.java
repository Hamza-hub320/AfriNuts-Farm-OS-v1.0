package com.afrinuts.farmos.ui.users;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.data.repository.BaseRepository;
import com.afrinuts.farmos.data.repository.UserRepository;
import com.afrinuts.farmos.utils.SessionManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Renders the existing user detail screen in the Users tablet master-detail pane. */
public class UserDetailFragment extends Fragment {
    private static final String ARG_USER_ID = "user_id";

    public interface Listener {
        void onUserChanged(long userId);
        void onUserDeleted(long userId);
    }

    public static UserDetailFragment newInstance(long userId) {
        UserDetailFragment fragment = new UserDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private long userId = -1;
    private UserEntity user;
    private AppDatabase database;
    private UserRepository userRepository;
    private SessionManager sessionManager;
    private Listener listener;

    private TextView tvUserInitials, tvUserName, tvUserRole, tvUserTitle;
    private Chip chipUserStatus;
    private Button btnDeactivate;
    private View btnEdit, btnDelete;
    private TextView tvAccessRole, tvAccessLevel, tvLastLogin, tvMemberSince, tvAuthMethod, tvAccessDescription;
    private ProgressBar accessProgress;
    private View layoutContactCard, layoutEmail, layoutPin, layoutProfileDetails;
    private TextView tvUserEmail, tvUserPhone, tvUserCompany, tvUserTitleDetail, tvUserBio;
    private View layoutCompany, layoutTitle, layoutBio, btnResetPin;
    private View tabletActions, tabletBtnEdit, tabletBtnDelete;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_user_detail, container, false);
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
        userId = getArguments() == null ? -1 : getArguments().getLong(ARG_USER_ID, -1);
        database = AppDatabase.getInstance(requireContext());
        userRepository = new UserRepository(requireContext(), 1L);
        sessionManager = new SessionManager(requireContext());
        bindViews(view);

        boolean isTablet = getResources().getConfiguration().screenWidthDp >= 600;
        if (isTablet) arrangeTabletHeader();

        tabletActions = requireActivity().findViewById(R.id.tabletUserActions);
        tabletBtnEdit = requireActivity().findViewById(R.id.tabletBtnEdit);
        tabletBtnDelete = requireActivity().findViewById(R.id.tabletBtnDelete);
        if (isTablet && tabletActions != null) {
            tabletActions.setVisibility(View.VISIBLE);
            tabletBtnEdit.setOnClickListener(v -> showEditUserDialog());
            tabletBtnDelete.setOnClickListener(v -> confirmDelete());
            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
        }

        View toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) toolbar.setVisibility(View.GONE);
        ViewGroup root = (ViewGroup) view;
        View scroll = root.getChildAt(1);
        if (scroll instanceof android.widget.ScrollView) {
            android.widget.ScrollView scrollView = (android.widget.ScrollView) scroll;
            scrollView.setPadding(scrollView.getPaddingLeft(), dp(2), scrollView.getPaddingRight(),
                    scrollView.getPaddingBottom());
        }
        loadUserData();
    }

    private void arrangeTabletHeader() {
        if (!(chipUserStatus.getParent() instanceof ViewGroup)) return;
        ViewGroup statusRow = (ViewGroup) chipUserStatus.getParent();
        if (!(statusRow.getParent() instanceof ViewGroup)) return;
        ViewGroup headerColumn = (ViewGroup) statusRow.getParent();
        if (headerColumn.getChildCount() == 0 || !(headerColumn.getChildAt(0) instanceof ViewGroup)) return;
        ViewGroup headerRow = (ViewGroup) headerColumn.getChildAt(0);

        headerColumn.setPadding(headerColumn.getPaddingLeft(), headerColumn.getPaddingTop(),
                headerColumn.getPaddingRight(), dp(8));
        if (headerColumn.getParent() instanceof View) {
            View headerCard = (View) headerColumn.getParent();
            if (headerCard.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams cardParams = (ViewGroup.MarginLayoutParams) headerCard.getLayoutParams();
                cardParams.bottomMargin = dp(8);
                headerCard.setLayoutParams(cardParams);
            }
        }
        GradientDrawable initialsBackground = new GradientDrawable();
        initialsBackground.setColor(0xFFE3F0E7);
        initialsBackground.setCornerRadius(dp(27));
        tvUserInitials.setBackground(initialsBackground);
        tvUserInitials.setTextColor(0xFF0A5530);

        headerColumn.removeView(statusRow);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = dp(14);
        statusRow.setLayoutParams(params);
        ((LinearLayout) statusRow).setGravity(android.view.Gravity.CENTER_VERTICAL);
        headerRow.addView(statusRow);
    }

    public long getUserId() { return userId; }

    private void bindViews(View view) {
        tvUserInitials = view.findViewById(R.id.tvUserInitials);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserRole = view.findViewById(R.id.tvUserRole);
        tvUserTitle = view.findViewById(R.id.tvUserTitle);
        chipUserStatus = view.findViewById(R.id.chipUserStatus);
        btnDeactivate = view.findViewById(R.id.btnDeactivate);
        btnEdit = view.findViewById(R.id.btnEdit);
        btnDelete = view.findViewById(R.id.btnDelete);
        tvAccessRole = view.findViewById(R.id.tvAccessRole);
        tvAccessLevel = view.findViewById(R.id.tvAccessLevel);
        tvLastLogin = view.findViewById(R.id.tvLastLogin);
        tvMemberSince = view.findViewById(R.id.tvMemberSince);
        tvAuthMethod = view.findViewById(R.id.tvAuthMethod);
        accessProgress = view.findViewById(R.id.accessProgress);
        tvAccessDescription = view.findViewById(R.id.tvAccessDescription);
        layoutContactCard = view.findViewById(R.id.layoutContactCard);
        layoutEmail = view.findViewById(R.id.layoutEmail);
        layoutPin = view.findViewById(R.id.layoutPin);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvUserPhone = view.findViewById(R.id.tvUserPhone);
        btnResetPin = view.findViewById(R.id.btnResetPin);
        layoutProfileDetails = view.findViewById(R.id.layoutProfileDetails);
        layoutCompany = view.findViewById(R.id.layoutCompany);
        layoutTitle = view.findViewById(R.id.layoutTitle);
        layoutBio = view.findViewById(R.id.layoutBio);
        tvUserCompany = view.findViewById(R.id.tvUserCompany);
        tvUserTitleDetail = view.findViewById(R.id.tvUserTitleDetail);
        tvUserBio = view.findViewById(R.id.tvUserBio);

        btnEdit.setOnClickListener(v -> showEditUserDialog());
        btnDeactivate.setOnClickListener(v -> toggleUserStatus());
        btnDelete.setOnClickListener(v -> confirmDelete());
        btnResetPin.setOnClickListener(v -> showResetPinDialog());
    }

    private void loadUserData() {
        userRepository.getUserById(userId).observe(getViewLifecycleOwner(), loadedUser -> {
            user = loadedUser;
            if (user != null) displayUserData();
            else {
                Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayUserData() {
        tvUserInitials.setText(user.getInitials());
        tvUserName.setText(user.getName());
        tvUserRole.setText(user.getDisplayRole());
        if (user.getTitle() != null && !user.getTitle().isEmpty()) {
            tvUserTitle.setText(user.getTitle());
            tvUserTitle.setVisibility(View.VISIBLE);
        } else tvUserTitle.setVisibility(View.GONE);

        int chipFill = user.isActive() ? 0xFFE3F0E7 : 0xFFEFEAE0;
        int chipInk = user.isActive() ? 0xFF0A5530 : 0xFF6F6A61;
        chipUserStatus.setText(user.isActive() ? "Active" : "Inactive");
        chipUserStatus.setChipBackgroundColor(ColorStateList.valueOf(chipFill));
        chipUserStatus.setTextColor(chipInk);
        chipUserStatus.setChipIconResource(user.isActive() ? R.drawable.ic_check_circle : R.drawable.ic_cancel);
        chipUserStatus.setChipIconTint(ColorStateList.valueOf(chipInk));
        btnDeactivate.setText(user.isActive() ? "Deactivate" : "Activate");

        tvUserCompany.setText(nonEmpty(user.getCompany(), "—"));
        layoutCompany.setVisibility(View.VISIBLE);
        tvUserTitleDetail.setText(nonEmpty(user.getTitle(), "—"));
        layoutTitle.setVisibility(View.VISIBLE);
        tvUserBio.setText(nonEmpty(user.getBio(), "No bio available."));
        layoutBio.setVisibility(View.VISIBLE);
        layoutProfileDetails.setVisibility(View.VISIBLE);

        tvUserPhone.setText(user.getPhone() != null ? user.getPhone() : "No phone");
        if (user.getAuthMethod() == UserEntity.AuthMethod.PASSWORD) {
            layoutEmail.setVisibility(View.VISIBLE);
            layoutPin.setVisibility(View.GONE);
            tvUserEmail.setText(user.getEmail() != null ? user.getEmail() : "No email");
        } else {
            layoutEmail.setVisibility(View.GONE);
            layoutPin.setVisibility(View.VISIBLE);
        }

        boolean isCurrentUser = user.getLocalId() == sessionManager.getUserId();
        if (user.getRole() == UserEntity.UserRole.OWNER && isCurrentUser) {
            btnDeactivate.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
            if (tabletBtnDelete != null) tabletBtnDelete.setVisibility(View.GONE);
        } else {
            btnDeactivate.setVisibility(View.VISIBLE);
            int deleteVisibility = sessionManager.isOwner() ? View.VISIBLE : View.GONE;
            btnDelete.setVisibility(deleteVisibility);
            if (tabletBtnDelete != null) tabletBtnDelete.setVisibility(deleteVisibility);
        }
        if (getResources().getConfiguration().screenWidthDp >= 600) {
            // The tablet footer owns these actions; keep the copies in the scroll content hidden.
            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
        }

        tvAccessRole.setText(user.getDisplayRole());
        tvAccessLevel.setText(user.isActive() ? "Active" : "Inactive");
        // The reused phone layout uses white backgrounds for the secondary KPI cards.
        // Give them the warm sand fill from the tablet master-detail wireframe.
        if (getResources().getConfiguration().screenWidthDp >= 600) {
            setKpiCardColor(tvAccessLevel, 0xFFF5F1E8);
            setKpiCardColor(tvLastLogin, 0xFFF5F1E8);
        }
        if (user.getRole() != null) {
            accessProgress.setProgress(user.getRole().getAccessLevel());
            tvAccessDescription.setText(user.getRole().getDescription());
        }
        tvMemberSince.setText(dateFormat.format(new Date(user.getCreatedAt())));
        tvLastLogin.setText(user.getLastLoginAt() > 0
                ? dateFormat.format(new Date(user.getLastLoginAt())) : "Never");
        tvAuthMethod.setText(user.getAuthMethod() == null ? "—" : user.getAuthMethod().getDisplayName());
    }

    private void setKpiCardColor(View valueView, int color) {
        if (valueView == null || !(valueView.getParent() instanceof View)) return;
        View content = (View) valueView.getParent();
        if (content.getParent() instanceof CardView) {
            ((CardView) content.getParent()).setCardBackgroundColor(color);
        }
    }

    private String nonEmpty(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }

    private void toggleUserStatus() {
        if (user.getLocalId() == sessionManager.getUserId()) {
            Toast.makeText(requireContext(), "You cannot deactivate your own account", Toast.LENGTH_SHORT).show();
            return;
        }
        String action = user.isActive() ? "deactivate" : "activate";
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(user.isActive() ? "Deactivate User" : "Activate User")
                .setMessage("Are you sure you want to " + action + " " + user.getName() + "?")
                .setPositiveButton(user.isActive() ? "Deactivate" : "Activate", (dialog, which) -> {
                    user.setActive(!user.isActive());
                    userRepository.updateUser(user, new BaseRepository.RepositoryCallback<Boolean>() {
                        @Override public void onSuccess(Boolean result) {
                            if (!isAdded()) return;
                            displayUserData();
                            notifyChanged();
                            Toast.makeText(requireContext(), "User " + action + "d successfully", Toast.LENGTH_SHORT).show();
                        }
                        @Override public void onError(String error) {
                            if (isAdded()) Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDelete() {
        if (user.getLocalId() == sessionManager.getUserId()) {
            Toast.makeText(requireContext(), "You cannot delete your own account", Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete " + user.getName() + "? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> userRepository.deleteUser(user,
                        new BaseRepository.RepositoryCallback<Boolean>() {
                            @Override public void onSuccess(Boolean result) {
                                if (!isAdded()) return;
                                Toast.makeText(requireContext(), "User deleted successfully", Toast.LENGTH_SHORT).show();
                                if (listener != null) listener.onUserDeleted(userId);
                            }
                            @Override public void onError(String error) {
                                if (isAdded()) Toast.makeText(requireContext(), "Error deleting user: " + error, Toast.LENGTH_SHORT).show();
                            }
                        }))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditUserDialog() {
        EditUserDialog dialog = EditUserDialog.newInstance(userId);
        dialog.setOnUserUpdatedListener(() -> executor.execute(() -> {
            UserEntity refreshed = database.userDao().getUserById(userId);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (!isAdded() || refreshed == null) return;
                user = refreshed;
                displayUserData();
                notifyChanged();
            });
        }));
        dialog.show(getParentFragmentManager(), "EditUserDialog");
    }

    private void notifyChanged() {
        if (listener != null) listener.onUserChanged(userId);
    }

    private void showResetPinDialog() {
        Toast.makeText(requireContext(), "Reset PIN coming soon", Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (context instanceof Listener) listener = (Listener) context;
    }

    @Override public void onDetach() {
        listener = null;
        super.onDetach();
    }

    @Override public void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}
