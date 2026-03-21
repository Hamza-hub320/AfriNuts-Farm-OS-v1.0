package com.afrinuts.farmos.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.remote.EntityMapper;
import com.afrinuts.farmos.data.sync.FirestoreSyncManager;

import java.util.List;

public class ExpenseRepository extends BaseRepository {
    private static final String TAG = "ExpenseRepository";
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
                // 1. Insert into local database first
                long id = database.expenseDao().insert(expense);
                expense.setId(id);

                Log.d(TAG, "Expense saved locally with ID: " + id);

                // 2. Try to upload to Firestore immediately
                firestoreHelper.uploadExpense(expense)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Expense uploaded to Firestore: " + id);
                            runOnUiThread(() -> callback.onSuccess(id));
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Firestore upload failed, queuing for sync", e);

                            // Queue for later sync (only if NOT a remote update)
                            if (!firestoreSyncManager.isRemoteUpdate()) {
                                syncManager.queueForSync("expenses", id, SyncEntity.SyncAction.CREATE);
                                Log.d(TAG, "Queued expense for sync (local change)");
                            } else {
                                Log.d(TAG, "Skipping queue - this is a remote update");
                            }

                            runOnUiThread(() -> callback.onSuccess(id));
                        });

            } catch (Exception e) {
                Log.e(TAG, "Error inserting expense", e);
                runOnUiThread(() -> callback.onError(e.getMessage()));
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
                // 1. Update local database
                database.expenseDao().update(expense);

                Log.d(TAG, "Expense updated locally: " + expense.getId());

                // 2. Try to update Firestore
                firestoreHelper.updateExpense(expense)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Expense updated in Firestore: " + expense.getId());
                            runOnUiThread(() -> callback.onSuccess(true));
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Firestore update failed, queuing for sync", e);

                            // Queue for later sync (only if NOT a remote update)
                            if (!firestoreSyncManager.isRemoteUpdate()) {
                                syncManager.queueForSync("expenses", expense.getId(), SyncEntity.SyncAction.UPDATE);
                                Log.d(TAG, "Queued expense update for sync (local change)");
                            } else {
                                Log.d(TAG, "Skipping queue - this is a remote update");
                            }

                            runOnUiThread(() -> callback.onSuccess(true));
                        });

            } catch (Exception e) {
                Log.e(TAG, "Error updating expense", e);
                runOnUiThread(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // DELETE
    // ========================================

    public void deleteExpense(ExpenseEntity expense, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                // 1. Delete from local database
                database.expenseDao().delete(expense);

                Log.d(TAG, "Expense deleted locally: " + expense.getId());

                // 2. Try to delete from Firestore
                firestoreHelper.deleteExpense(expense.getId())
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Expense deleted from Firestore: " + expense.getId());
                            runOnUiThread(() -> callback.onSuccess(true));
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Firestore delete failed, queuing for sync", e);

                            // Queue for later sync (only if NOT a remote update)
                            if (!firestoreSyncManager.isRemoteUpdate()) {
                                syncManager.queueForSync("expenses", expense.getId(), SyncEntity.SyncAction.DELETE);
                                Log.d(TAG, "Queued expense delete for sync (local change)");
                            } else {
                                Log.d(TAG, "Skipping queue - this is a remote update");
                            }

                            runOnUiThread(() -> callback.onSuccess(true));
                        });

            } catch (Exception e) {
                Log.e(TAG, "Error deleting expense", e);
                runOnUiThread(() -> callback.onError(e.getMessage()));
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
                        Log.d(TAG, "Synced new expense from Firestore: " + expense.getId());
                    } else {
                        database.expenseDao().update(expense);
                        Log.d(TAG, "Synced updated expense from Firestore: " + expense.getId());
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

    private void runOnUiThread(Runnable action) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(action);
    }
}