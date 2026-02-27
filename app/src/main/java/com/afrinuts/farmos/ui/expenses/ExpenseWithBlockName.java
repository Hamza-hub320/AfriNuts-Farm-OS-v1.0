package com.afrinuts.farmos.ui.expenses;

import com.afrinuts.farmos.data.local.entity.ExpenseEntity;

public class ExpenseWithBlockName {
    private ExpenseEntity expense;
    private String blockName; // Null if farm-wide

    public ExpenseWithBlockName(ExpenseEntity expense, String blockName) {
        this.expense = expense;
        this.blockName = blockName;
    }

    public ExpenseEntity getExpense() { return expense; }
    public String getBlockName() { return blockName; }
    public boolean isFarmWide() { return blockName == null; }
}