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
import com.afrinuts.farmos.data.remote.EntityMapper;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

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

        String farmId = String.valueOf(currentFarm.getId());
        Log.d(TAG, "Starting blocks listener for farm: " + farmId);

        // FIX: Listen to subcollection under farms
        blocksListener = firestore.collection("farms")
                .document(farmId)
                .collection("blocks")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Blocks listener failed", error);
                        return;
                    }

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            DocumentSnapshot doc = dc.getDocument();

                            switch (dc.getType()) {
                                case ADDED:
                                    Log.d(TAG, "New block added remotely: " + doc.getId());
                                    BlockEntity newBlock = documentToBlock(doc);
                                    if (newBlock != null) {
                                        handleBlockAdded(newBlock);
                                    }
                                    break;
                                case MODIFIED:
                                    Log.d(TAG, "Block modified remotely: " + doc.getId());
                                    BlockEntity modifiedBlock = documentToBlock(doc);
                                    if (modifiedBlock != null) {
                                        handleBlockModified(modifiedBlock);
                                    }
                                    break;
                                case REMOVED:
                                    Log.d(TAG, "Block removed remotely: " + doc.getId());
                                    BlockEntity removedBlock = documentToBlock(doc);
                                    if (removedBlock != null) {
                                        handleBlockRemoved(removedBlock);
                                    }
                                    break;
                            }
                        }
                    }
                });
    }

    protected void startListeningForExpenses() {
        if (currentFarm == null) return;

        String farmId = String.valueOf(currentFarm.getId());

        // FIX: Listen to subcollection under farms
        expensesListener = firestore.collection("farms")
                .document(farmId)
                .collection("expenses")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Expenses listener failed", error);
                        return;
                    }

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            DocumentSnapshot doc = dc.getDocument();

                            switch (dc.getType()) {
                                case ADDED:
                                    ExpenseEntity newExpense = documentToExpense(doc);
                                    if (newExpense != null) {
                                        handleExpenseAdded(newExpense);
                                    }
                                    break;
                                case MODIFIED:
                                    ExpenseEntity modifiedExpense = documentToExpense(doc);
                                    if (modifiedExpense != null) {
                                        handleExpenseModified(modifiedExpense);
                                    }
                                    break;
                                case REMOVED:
                                    ExpenseEntity removedExpense = documentToExpense(doc);
                                    if (removedExpense != null) {
                                        handleExpenseRemoved(removedExpense);
                                    }
                                    break;
                            }
                        }
                    }
                });
    }

    protected void startListeningForRevenues() {
        if (currentFarm == null) return;

        String farmId = String.valueOf(currentFarm.getId());

        // FIX: Listen to subcollection under farms
        revenuesListener = firestore.collection("farms")
                .document(farmId)
                .collection("revenues")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Revenues listener failed", error);
                        return;
                    }

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            DocumentSnapshot doc = dc.getDocument();

                            switch (dc.getType()) {
                                case ADDED:
                                    RevenueEntity newRevenue = documentToRevenue(doc);
                                    if (newRevenue != null) {
                                        handleRevenueAdded(newRevenue);
                                    }
                                    break;
                                case MODIFIED:
                                    RevenueEntity modifiedRevenue = documentToRevenue(doc);
                                    if (modifiedRevenue != null) {
                                        handleRevenueModified(modifiedRevenue);
                                    }
                                    break;
                                case REMOVED:
                                    RevenueEntity removedRevenue = documentToRevenue(doc);
                                    if (removedRevenue != null) {
                                        handleRevenueRemoved(removedRevenue);
                                    }
                                    break;
                            }
                        }
                    }
                });
    }

    protected void startListeningForTasks() {
        if (currentFarm == null) return;

        String farmId = String.valueOf(currentFarm.getId());

        // FIX: Listen to subcollection under farms
        tasksListener = firestore.collection("farms")
                .document(farmId)
                .collection("tasks")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Tasks listener failed", error);
                        return;
                    }

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            DocumentSnapshot doc = dc.getDocument();

                            switch (dc.getType()) {
                                case ADDED:
                                    TaskEntity newTask = documentToTask(doc);
                                    if (newTask != null) {
                                        handleTaskAdded(newTask);
                                    }
                                    break;
                                case MODIFIED:
                                    TaskEntity modifiedTask = documentToTask(doc);
                                    if (modifiedTask != null) {
                                        handleTaskModified(modifiedTask);
                                    }
                                    break;
                                case REMOVED:
                                    TaskEntity removedTask = documentToTask(doc);
                                    if (removedTask != null) {
                                        handleTaskRemoved(removedTask);
                                    }
                                    break;
                            }
                        }
                    }
                });
    }

    // Helper methods to convert Firestore documents to entities
    private BlockEntity documentToBlock(DocumentSnapshot doc) {
        try {
            long id = doc.getLong("id");
            long farmId = doc.getLong("farmId");
            String blockName = doc.getString("blockName");
            double hectareSize = doc.getDouble("hectareSize");
            String notes = doc.getString("notes");

            BlockEntity block = new BlockEntity(farmId, blockName, hectareSize, notes);
            block.setId(id);
            block.setStatus(BlockEntity.BlockStatus.valueOf(doc.getString("status")));
            block.setClearedDate(doc.getLong("clearedDate"));
            block.setPlowedDate(doc.getLong("plowedDate"));
            block.setPlantedDate(doc.getLong("plantedDate"));
            block.setSurvivalRate(doc.getDouble("survivalRate"));
            block.setReplacementCount(doc.getLong("replacementCount").intValue());
            return block;
        } catch (Exception e) {
            Log.e(TAG, "Error converting block document", e);
            return null;
        }
    }

    private ExpenseEntity documentToExpense(DocumentSnapshot doc) {
        try {
            long id = doc.getLong("id");
            long farmId = doc.getLong("farmId");
            Long blockId = doc.contains("blockId") ? doc.getLong("blockId") : null;
            double amount = doc.getDouble("amount");
            long date = doc.getLong("date");
            String description = doc.getString("description");

            ExpenseEntity expense = new ExpenseEntity(
                    farmId,
                    blockId,
                    ExpenseEntity.ExpenseCategory.valueOf(doc.getString("category")),
                    amount,
                    date,
                    description
            );
            expense.setId(id);
            return expense;
        } catch (Exception e) {
            Log.e(TAG, "Error converting expense document", e);
            return null;
        }
    }

    private RevenueEntity documentToRevenue(DocumentSnapshot doc) {
        try {
            long id = doc.getLong("id");
            long farmId = doc.getLong("farmId");
            Long blockId = doc.contains("blockId") ? doc.getLong("blockId") : null;
            double quantityKg = doc.getDouble("quantityKg");
            double pricePerKg = doc.getDouble("pricePerKg");
            String buyer = doc.getString("buyer");
            String notes = doc.getString("notes");

            RevenueEntity revenue = new RevenueEntity(
                    farmId,
                    blockId,
                    doc.getLong("harvestDate"),
                    quantityKg,
                    pricePerKg,
                    buyer,
                    RevenueEntity.QualityGrade.valueOf(doc.getString("quality")),
                    notes
            );
            revenue.setId(id);
            return revenue;
        } catch (Exception e) {
            Log.e(TAG, "Error converting revenue document", e);
            return null;
        }
    }

    private TaskEntity documentToTask(DocumentSnapshot doc) {
        try {
            long id = doc.getLong("id");
            long farmId = doc.getLong("farmId");
            Long blockId = doc.contains("blockId") ? doc.getLong("blockId") : null;
            String title = doc.getString("title");
            String description = doc.getString("description");
            long dueDate = doc.getLong("dueDate");

            TaskEntity task = new TaskEntity(
                    farmId,
                    blockId,
                    title,
                    description,
                    TaskEntity.TaskType.valueOf(doc.getString("type")),
                    TaskEntity.TaskStatus.valueOf(doc.getString("status")),
                    dueDate
            );
            task.setId(id);
            task.setCompletedDate(doc.getLong("completedDate"));
            return task;
        } catch (Exception e) {
            Log.e(TAG, "Error converting task document", e);
            return null;
        }
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