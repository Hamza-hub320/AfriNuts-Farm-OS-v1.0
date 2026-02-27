package com.afrinuts.farmos.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.afrinuts.farmos.data.local.entity.ExpenseEntity;

import java.util.List;

@Dao
public interface ExpenseDao {

    @Insert
    long insert(ExpenseEntity expense);

    @Update
    void update(ExpenseEntity expense);

    @Delete
    void delete(ExpenseEntity expense);

    @Query("SELECT * FROM expenses WHERE id = :id")
    ExpenseEntity getExpenseById(long id);

    @Query("SELECT * FROM expenses WHERE farmId = :farmId ORDER BY date DESC")
    List<ExpenseEntity> getExpensesByFarmId(long farmId);

    @Query("SELECT * FROM expenses WHERE blockId = :blockId ORDER BY date DESC")
    List<ExpenseEntity> getExpensesByBlockId(long blockId);

    @Query("SELECT * FROM expenses WHERE farmId = :farmId AND blockId IS NULL ORDER BY date DESC")
    List<ExpenseEntity> getFarmWideExpenses(long farmId);

    @Query("SELECT * FROM expenses WHERE category = :category ORDER BY date DESC")
    List<ExpenseEntity> getExpensesByCategory(ExpenseEntity.ExpenseCategory category);

    @Query("SELECT SUM(amount) FROM expenses WHERE farmId = :farmId")
    double getTotalExpenses(long farmId);

    @Query("SELECT SUM(amount) FROM expenses WHERE blockId = :blockId")
    double getTotalExpensesByBlock(long blockId);

    @Query("SELECT SUM(amount) FROM expenses WHERE farmId = :farmId AND blockId IS NULL")
    double getTotalFarmWideExpenses(long farmId);

    @Query("SELECT SUM(amount) FROM expenses WHERE farmId = :farmId AND date BETWEEN :startDate AND :endDate")
    double getExpensesInDateRange(long farmId, long startDate, long endDate);

    @Query("SELECT category, SUM(amount) as total FROM expenses WHERE farmId = :farmId GROUP BY category")
    List<CategoryTotal> getExpenseSummaryByCategory(long farmId);

    // Inner class for category totals
    class CategoryTotal {
        public String category;
        public double total;
    }
}