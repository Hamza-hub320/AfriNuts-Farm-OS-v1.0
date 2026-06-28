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
        // ✅ Check if this is a procurement expense
        if (expense.isProcurementRelated()) {
            StringBuilder display = new StringBuilder();

            // Add supplier name if available
            if (expense.getSupplierName() != null && !expense.getSupplierName().isEmpty()) {
                display.append("").append(expense.getSupplierName());
            }

            // Add batch number if available
            if (expense.getBatchNumber() != null && !expense.getBatchNumber().isEmpty()) {
                if (display.length() > 0) {
                    display.append(" | ");
                }
                display.append("").append(expense.getBatchNumber());
            }

            // If no procurement info, show category
            if (display.length() == 0) {
                display.append(expense.getCategory().getDisplayName());
            }

            return display.toString();
        }

        // ✅ Farm expenses - show block info
        if (isFarmWide()) {
            return "Farm-Wide";
        } else {
            return "Block " + blockName;
        }
    }

    // ✅ Helper to get short display for small spaces
    public String getShortDisplayName() {
        if (expense.isProcurementRelated()) {
            if (expense.getSupplierName() != null) {
                return "" + expense.getSupplierName();
            } else if (expense.getBatchNumber() != null) {
                return "" + expense.getBatchNumber();
            }
            return expense.getCategory().getDisplayName();
        }
        return getDisplayName();
    }
}