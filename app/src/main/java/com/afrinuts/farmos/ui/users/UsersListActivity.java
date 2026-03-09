package com.afrinuts.farmos.ui.users;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.ui.BaseActivity;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class UsersListActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private UserAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout emptyView;
    private TextView totalUsersValue;
    private TextView ownersValue;
    private TextView activeUsersValue;
    private ChipGroup filterChipGroup;

    private AppDatabase database;
    private List<UserEntity> allUsers = new ArrayList<>();
    private List<UserEntity> filteredUsers = new ArrayList<>();

    private String currentFilter = "ALL"; // ALL, OWNERS, MANAGERS, WORKERS, INVESTORS

    @Override
    protected boolean checkRoleAccess() {
        // Only Owners can access UsersList
        return sessionManager.isOwner();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // This will trigger role check
        setContentView(R.layout.activity_users_list);

        // If we get here, user has permission (Owner only)

        // Initialize database
        database = AppDatabase.getInstance(this);

        // Initialize views
        initViews();

        // Setup toolbar
        setupToolbar();

        // Setup filter listeners
        setupFilters();

        // Setup FAB
        setupFab();

        // Load data
        loadData();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        totalUsersValue = findViewById(R.id.totalUsersValue);
        ownersValue = findViewById(R.id.ownersValue);
        activeUsersValue = findViewById(R.id.activeUsersValue);
        filterChipGroup = findViewById(R.id.filterChipGroup);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_home);
            getSupportActionBar().setTitle("Team Members");
        }
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
    }

    private void setupFab() {
        FloatingActionButton fab = findViewById(R.id.fabAddUser);
        fab.setOnClickListener(v -> {
            AddUserDialog dialog = AddUserDialog.newInstance();
            dialog.setOnUserAddedListener(() -> {
                loadData(); // Refresh list
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
                    updateSummaryStats();
                    applyFilter();
                }
            });
        }).start();
    }

    private void updateSummaryStats() {
        int total = allUsers.size();
        int owners = 0;
        int active = 0;

        for (UserEntity user : allUsers) {
            if (user.getRole() == UserEntity.UserRole.OWNER) {
                owners++;
            }
            if (user.isActive()) {
                active++;
            }
        }

        totalUsersValue.setText(String.valueOf(total));
        ownersValue.setText(String.valueOf(owners));
        activeUsersValue.setText(String.valueOf(active));
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

        adapter = new UserAdapter(filteredUsers, user -> {
            // Navigate to UserDetailActivity
            android.content.Intent intent = new android.content.Intent(this, UserDetailActivity.class);
            intent.putExtra(UserDetailActivity.EXTRA_USER_ID, user.getId());
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
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
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}