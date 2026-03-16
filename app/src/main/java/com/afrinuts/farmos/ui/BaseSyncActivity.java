package com.afrinuts.farmos.ui;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class BaseSyncActivity extends BaseActivity {

    private static final String TAG = "BaseSyncActivity";
    protected AppDatabase database;
    protected FirebaseFirestore firestore;
    protected FarmEntity currentFarm;
    protected final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    // Listeners for real-time updates
    private ListenerRegistration blocksListener;
    private ListenerRegistration expensesListener;
    private ListenerRegistration revenuesListener;
    private ListenerRegistration tasksListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = AppDatabase.getInstance(this);
        firestore = FirebaseFirestore.getInstance();
    }

    protected void startListeningForBlocks() {
        if (currentFarm == null) return;

        Log.d(TAG, "Starting blocks listener for farm: " + currentFarm.getId());

        Query query = firestore.collection("blocks")
                .whereEqualTo("farmId", currentFarm.getId());

        blocksListener = query.addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                Log.e(TAG, "Blocks listener failed", error);
                return;
            }

            if (snapshots != null) {
                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                    switch (dc.getType()) {
                        case ADDED:
                            Log.d(TAG, "New block added remotely");
                            handleBlockAdded(dc.getDocument().toObject(BlockEntity.class));
                            break;
                        case MODIFIED:
                            Log.d(TAG, "Block modified remotely");
                            handleBlockModified(dc.getDocument().toObject(BlockEntity.class));
                            break;
                        case REMOVED:
                            Log.d(TAG, "Block removed remotely");
                            handleBlockRemoved(dc.getDocument().toObject(BlockEntity.class));
                            break;
                    }
                }
            }
        });
    }

    protected void startListeningForExpenses() {
        if (currentFarm == null) return;

        Query query = firestore.collection("expenses")
                .whereEqualTo("farmId", currentFarm.getId());

        expensesListener = query.addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                Log.e(TAG, "Expenses listener failed", error);
                return;
            }

            if (snapshots != null) {
                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                    switch (dc.getType()) {
                        case ADDED:
                            handleExpenseAdded(dc.getDocument().toObject(ExpenseEntity.class));
                            break;
                        case MODIFIED:
                            handleExpenseModified(dc.getDocument().toObject(ExpenseEntity.class));
                            break;
                        case REMOVED:
                            handleExpenseRemoved(dc.getDocument().toObject(ExpenseEntity.class));
                            break;
                    }
                }
            }
        });
    }

    protected void startListeningForRevenues() {
        if (currentFarm == null) return;

        Query query = firestore.collection("revenues")
                .whereEqualTo("farmId", currentFarm.getId());

        revenuesListener = query.addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                Log.e(TAG, "Revenues listener failed", error);
                return;
            }

            if (snapshots != null) {
                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                    switch (dc.getType()) {
                        case ADDED:
                            handleRevenueAdded(dc.getDocument().toObject(RevenueEntity.class));
                            break;
                        case MODIFIED:
                            handleRevenueModified(dc.getDocument().toObject(RevenueEntity.class));
                            break;
                        case REMOVED:
                            handleRevenueRemoved(dc.getDocument().toObject(RevenueEntity.class));
                            break;
                    }
                }
            }
        });
    }

    protected void startListeningForTasks() {
        if (currentFarm == null) return;

        Query query = firestore.collection("tasks")
                .whereEqualTo("farmId", currentFarm.getId());

        tasksListener = query.addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                Log.e(TAG, "Tasks listener failed", error);
                return;
            }

            if (snapshots != null) {
                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                    switch (dc.getType()) {
                        case ADDED:
                            handleTaskAdded(dc.getDocument().toObject(TaskEntity.class));
                            break;
                        case MODIFIED:
                            handleTaskModified(dc.getDocument().toObject(TaskEntity.class));
                            break;
                        case REMOVED:
                            handleTaskRemoved(dc.getDocument().toObject(TaskEntity.class));
                            break;
                    }
                }
            }
        });
    }

    // Abstract methods for activities to implement
    protected abstract void handleBlockAdded(BlockEntity block);
    protected abstract void handleBlockModified(BlockEntity block);
    protected abstract void handleBlockRemoved(BlockEntity block);

    protected abstract void handleExpenseAdded(ExpenseEntity expense);
    protected abstract void handleExpenseModified(ExpenseEntity expense);
    protected abstract void handleExpenseRemoved(ExpenseEntity expense);

    protected abstract void handleRevenueAdded(RevenueEntity revenue);
    protected abstract void handleRevenueModified(RevenueEntity revenue);
    protected abstract void handleRevenueRemoved(RevenueEntity revenue);

    protected abstract void handleTaskAdded(TaskEntity task);
    protected abstract void handleTaskModified(TaskEntity task);
    protected abstract void handleTaskRemoved(TaskEntity task);

    protected void stopAllListeners() {
        if (blocksListener != null) blocksListener.remove();
        if (expensesListener != null) expensesListener.remove();
        if (revenuesListener != null) revenuesListener.remove();
        if (tasksListener != null) tasksListener.remove();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAllListeners();
        databaseExecutor.shutdown();
    }
}