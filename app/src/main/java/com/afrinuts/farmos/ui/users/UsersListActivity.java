package com.afrinuts.farmos.ui.users;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.ui.BaseActivity;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.graphics.drawable.GradientDrawable;

import java.util.ArrayList;
import java.util.List;

public class UsersListActivity extends BaseActivity {   // ← BaseActivity, not BaseHubActivity

    private RecyclerView recyclerView;
    private RecyclerView.Adapter<?> adapter;
    private ProgressBar progressBar;
    private LinearLayout emptyView;
    private ChipGroup filterChipGroup;

    private AppDatabase database;
    private long farmId = 1L;
    private List<UserEntity> allUsers = new ArrayList<>();
    private List<UserEntity> filteredUsers = new ArrayList<>();

    private String currentFilter = "ALL"; // ALL, OWNERS, MANAGERS, WORKERS, INVESTORS

    @Override
    protected boolean checkRoleAccess() {
        return com.afrinuts.farmos.utils.AccessMap.canOpenScreen(
                getClass().getSimpleName(), sessionManager.getUserRole());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_list);   // ← set layout here

        database = AppDatabase.getInstance(this);

        setupToolbar();
        initViews();
        setupFilters();
        setupFab();
        loadData();
    }

    private void setupToolbar() {
        ImageButton back = findViewById(R.id.toolbarBack);
        if (back != null) back.setOnClickListener(v -> finish());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        filterChipGroup = findViewById(R.id.filterChipGroup);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupFilters() {
        Chip chipAll = findViewById(R.id.chipAll);
        Chip chipOwners = findViewById(R.id.chipOwners);
        Chip chipManagers = findViewById(R.id.chipManagers);
        Chip chipWorkers = findViewById(R.id.chipWorkers);
        Chip chipInvestors = findViewById(R.id.chipInvestors);

        chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "ALL";
                applyFilter();
            }
        });

        chipOwners.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "OWNERS";
                applyFilter();
            }
        });

        chipManagers.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "MANAGERS";
                applyFilter();
            }
        });

        chipWorkers.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "WORKERS";
                applyFilter();
            }
        });

        chipInvestors.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "INVESTORS";
                applyFilter();
            }
        });

        Chip[] chips = {chipAll, chipOwners, chipManagers, chipWorkers, chipInvestors};
        int[][] checkedStates = {new int[]{android.R.attr.state_checked}, new int[]{}};
        ColorStateList backgrounds = new ColorStateList(checkedStates, new int[]{0xFF0E6B3C, 0xFFF3EAD8});
        ColorStateList textColors = new ColorStateList(checkedStates, new int[]{0xFFFFFFFF, 0xFF59544C});
        float density = getResources().getDisplayMetrics().density;
        for (Chip chip : chips) {
            chip.setChipBackgroundColor(backgrounds);
            chip.setTextColor(textColors);
            chip.setChipStrokeColor(ColorStateList.valueOf(0xFFE9E1CF));
            chip.setChipStrokeWidth(density);
            chip.setChipCornerRadius(999 * density);
            chip.setEnsureMinTouchTargetSize(false);
        }
    }

    private void setupFab() {
        FloatingActionButton fab = findViewById(R.id.fabAddUser);
        fab.setOnClickListener(v -> {
            AddUserDialog dialog = AddUserDialog.newInstance();
            dialog.setOnUserAddedListener(() -> {
                loadData();
            });
            dialog.show(getSupportFragmentManager(), "AddUserDialog");
        });
    }

    private void loadData() {
        showLoading(true);

        new Thread(() -> {
            allUsers = database.userDao().getAllUsers();

            runOnUiThread(() -> {
                showLoading(false);

                if (allUsers.isEmpty()) {
                    showEmpty(true);
                } else {
                    showEmpty(false);
                    applyFilter();
                }
            });
        }).start();
    }

    private void applyFilter() {
        filteredUsers.clear();

        for (UserEntity user : allUsers) {
            switch (currentFilter) {
                case "ALL":
                    filteredUsers.add(user);
                    break;
                case "OWNERS":
                    if (user.getRole() == UserEntity.UserRole.OWNER) {
                        filteredUsers.add(user);
                    }
                    break;
                case "MANAGERS":
                    if (user.getRole() == UserEntity.UserRole.MANAGER) {
                        filteredUsers.add(user);
                    }
                    break;
                case "WORKERS":
                    if (user.getRole() == UserEntity.UserRole.WORKER) {
                        filteredUsers.add(user);
                    }
                    break;
                case "INVESTORS":
                    if (user.getRole() == UserEntity.UserRole.INVESTOR) {
                        filteredUsers.add(user);
                    }
                    break;
            }
        }

        adapter = new UserRowAdapter(filteredUsers, user -> {
            Intent intent = new Intent(UsersListActivity.this, UserDetailActivity.class);
            intent.putExtra(UserDetailActivity.EXTRA_USER_ID, user.getLocalId());
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
    }

    private static class UserRowAdapter extends RecyclerView.Adapter<UserRowAdapter.UserRowHolder> {
        private final List<UserEntity> users;
        private final UserAdapter.OnUserClickListener listener;

        UserRowAdapter(List<UserEntity> users, UserAdapter.OnUserClickListener listener) {
            this.users = users;
            this.listener = listener;
        }

        @NonNull
        @Override
        public UserRowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new UserRowHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_user, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull UserRowHolder holder, int position) {
            UserEntity user = users.get(position);
            holder.initials.setText(user.getInitials());
            int[] fills = {0xFFE3F0E7, 0xFFF3EAD8, 0xFFEFEAE0};
            int[] inks = {0xFF0A5530, 0xFF7A5F22, 0xFF6F6A61};
            int tintIndex = position % fills.length;
            GradientDrawable avatar = new GradientDrawable();
            avatar.setShape(GradientDrawable.OVAL);
            avatar.setColor(fills[tintIndex]);
            holder.initials.setBackground(avatar);
            holder.initials.setTextColor(inks[tintIndex]);

            holder.name.setText(user.getName());
            holder.role.setText(user.getDisplayRole());
            holder.roleIcon.setImageResource(roleIcon(user.getRole()));
            holder.roleIcon.setColorFilter(0xFF8A857B);
            holder.status.setImageResource(user.isActive() ? R.drawable.ic_check_circle : R.drawable.ic_cancel);
            holder.status.setColorFilter(user.isActive() ? 0xFF0E6B3C : 0xFFB9B2A2);
            holder.status.setContentDescription(user.isActive() ? "Active" : "Inactive");
            holder.card.setOnClickListener(v -> listener.onUserClick(user));
        }

        private static int roleIcon(UserEntity.UserRole role) {
            if (role == null) return R.drawable.ic_person;
            switch (role) {
                case OWNER: return R.drawable.ic_star;
                case CEO: return R.drawable.ic_farm;
                case COMPLIANCE_OFFICER: return R.drawable.ic_nav_shield_check;
                case MANAGER: return R.drawable.ic_assignment;
                case FIELD_OFFICER: return R.drawable.ic_nav_sprout;
                case INVESTOR: return R.drawable.ic_trending_up;
                case WORKER:
                default: return R.drawable.ic_person;
            }
        }

        @Override
        public int getItemCount() { return users.size(); }

        static class UserRowHolder extends RecyclerView.ViewHolder {
            final CardView card;
            final TextView initials;
            final TextView name;
            final TextView role;
            final ImageView roleIcon;
            final ImageView status;

            UserRowHolder(@NonNull View itemView) {
                super(itemView);
                card = (CardView) itemView;
                initials = itemView.findViewById(R.id.userInitials);
                name = itemView.findViewById(R.id.userName);
                role = itemView.findViewById(R.id.userRole);
                roleIcon = itemView.findViewById(R.id.roleIcon);
                status = itemView.findViewById(R.id.userStatus);
            }
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(View.GONE);
    }

    private void showEmpty(boolean show) {
        emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
