package com.afrinuts.farmos.ui.expenses;

import com.afrinuts.farmos.data.local.entity.ExpenseEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(expense.getDate()));
    }

    public String getFormattedAmount() {
        return String.format(Locale.getDefault(), "%,.0f XAF", expense.getAmount());
    }

    public String getDisplayName() {
        if (isFarmWide()) {
            return "Farm-Wide";
        } else {
            return "Block " + blockName;
        }
    }
}