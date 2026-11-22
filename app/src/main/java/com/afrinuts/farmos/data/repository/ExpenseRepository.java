package com.afrinuts.farmos.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.google.android.gms.tasks.Task;

import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.remote.EntityMapper;
import com.afrinuts.farmos.data.sync.FirestoreSyncManager;

import java.util.List;

public class ExpenseRepository extends BaseRepository {
    private static final String TAG = "ExpenseRepository";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private FirestoreSyncManager firestoreSyncManager;

    public ExpenseRepository(Context context, long farmId) {
        super(context, farmId);
        this.firestoreSyncManager = FirestoreSyncManager.getInstance(context);
    }

    // ========================================
    // CREATE
    // ========================================

    public void insertExpense(ExpenseEntity expense, InsertCallback callback) {
        executor.execute(() -> {
            try {
                // 1. Save locally FIRST
                long id = database.expenseDao().insert(expense);
                expense.setId(id);

                Log.d(TAG, "Expense saved locally with ID: " + id +
                        " Batch: " + expense.getBatchNumber() +
                        " Supplier: " + expense.getSupplierName());

                // 2. Queue for sync (only if NOT a remote update)
                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("expenses", id, SyncEntity.SyncAction.CREATE);
                    Log.d(TAG, "Queued expense for sync (local change)");
                } else {
                    Log.d(TAG, "Skipping queue - this is a remote update");
                }

                // 3. Trigger auto-sync
                syncManager.triggerAutoSync();

                // 4. Return success to UI
                mainHandler.post(() -> callback.onSuccess(id));

            } catch (Exception e) {
                Log.e(TAG, "Error inserting expense", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // READ
    // ========================================

    public LiveData<List<ExpenseEntity>> getAllExpenses() {
        return database.expenseDao().getAllExpensesLiveData();
    }

    public LiveData<ExpenseEntity> getExpenseById(long expenseId) {
        return database.expenseDao().getExpenseByIdLiveData(expenseId);
    }

    public LiveData<List<ExpenseEntity>> getExpensesForFarm() {
        return database.expenseDao().getExpensesByFarmIdLiveData(farmId);
    }

    public LiveData<List<ExpenseEntity>> getExpensesForBlock(long blockId) {
        return database.expenseDao().getExpensesByBlockIdLiveData(blockId);
    }

    // For synchronous operations
    public List<ExpenseEntity> getExpensesSync() {
        return database.expenseDao().getExpensesByFarmId(farmId);
    }

    public List<ExpenseEntity> getExpensesByBlockSync(long blockId) {
        return database.expenseDao().getExpensesByBlockId(blockId);
    }

    public double getTotalExpenses() {
        return database.expenseDao().getTotalExpenses(farmId);
    }

    // ========================================
    // UPDATE
    // ========================================

    public void updateExpense(ExpenseEntity expense, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                database.expenseDao().update(expense);
                Log.d(TAG, "Expense updated locally: " + expense.getId());

                // Queue for sync (only if NOT a remote update)
                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("expenses", expense.getId(), SyncEntity.SyncAction.UPDATE);
                    Log.d(TAG, "Queued expense update for sync (local change)");
                } else {
                    Log.d(TAG, "Skipping queue - this is a remote update");
                }

                // The local write and sync queue are authoritative for UI completion.
                // Firestore can remain pending indefinitely while the device is offline.
                boolean remoteConfirmed = false;
                try {
                    Task<Void> remoteWrite = firestoreHelper.updateExpense(expense);
                    remoteWrite.addOnSuccessListener(aVoid -> Log.d(TAG, "Expense updated in Firestore"))
                            .addOnFailureListener(e -> Log.e(TAG, "Firestore update failed; queued for sync", e));
                    remoteConfirmed = remoteWrite.isComplete() && remoteWrite.isSuccessful();
                } catch (Exception remoteError) {
                    Log.e(TAG, "Could not start Firestore update; queued for sync", remoteError);
                }
                boolean serverConfirmed = remoteConfirmed;
                mainHandler.post(() -> callback.onSuccess(serverConfirmed));

            } catch (Exception e) {
                Log.e(TAG, "Error updating expense", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // DELETE
    // ========================================

    public void deleteExpense(ExpenseEntity expense, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                database.expenseDao().delete(expense);
                Log.d(TAG, "Expense deleted locally: " + expense.getId());

                // Queue for sync (only if NOT a remote update)
                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("expenses", expense.getId(), SyncEntity.SyncAction.DELETE);
                    Log.d(TAG, "Queued expense deletion for sync (local change)");
                } else {
                    Log.d(TAG, "Skipping queue - this is a remote update");
                }

                // Do not wait for a server acknowledgement before completing the local delete.
                boolean remoteConfirmed = false;
                try {
                    Task<Void> remoteDelete = firestoreHelper.deleteExpense(expense.getId());
                    remoteDelete.addOnSuccessListener(aVoid -> Log.d(TAG, "Expense deleted from Firestore"))
                            .addOnFailureListener(e -> Log.e(TAG, "Firestore delete failed; queued for sync", e));
                    remoteConfirmed = remoteDelete.isComplete() && remoteDelete.isSuccessful();
                } catch (Exception remoteError) {
                    Log.e(TAG, "Could not start Firestore delete; queued for sync", remoteError);
                }
                boolean serverConfirmed = remoteConfirmed;
                mainHandler.post(() -> callback.onSuccess(serverConfirmed));

            } catch (Exception e) {
                Log.e(TAG, "Error deleting expense", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // SYNC FROM FIRESTORE
    // ========================================

    public void syncExpenseFromFirestore(com.google.firebase.firestore.DocumentSnapshot document) {
        executor.execute(() -> {
            try {
                ExpenseEntity expense = EntityMapper.mapToExpense(document);
                if (expense != null) {
                    ExpenseEntity existing = database.expenseDao().getExpenseById(expense.getId());
                    if (existing == null) {
                        database.expenseDao().insert(expense);
                        Log.d(TAG, "Synced new expense from Firestore: " + expense.getId() +
                                " Batch: " + expense.getBatchNumber() +
                                " Supplier: " + expense.getSupplierName());
                    } else {
                        // Compare timestamps to avoid overwriting newer local data
                        if (expense.getUpdatedAt() > existing.getUpdatedAt()) {
                            database.expenseDao().update(expense);
                            Log.d(TAG, "Synced updated expense from Firestore: " + expense.getId());
                        } else {
                            Log.d(TAG, "Skipped older expense update from Firestore: " + expense.getId());
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error syncing expense from Firestore", e);
            }
        });
    }

    public void deleteExpenseFromSync(long expenseId) {
        executor.execute(() -> {
            database.expenseDao().deleteById(expenseId);
            Log.d(TAG, "Deleted expense from sync: " + expenseId);
        });
    }
}
