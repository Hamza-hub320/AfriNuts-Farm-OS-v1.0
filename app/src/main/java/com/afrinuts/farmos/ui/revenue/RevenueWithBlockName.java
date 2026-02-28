package com.afrinuts.farmos.ui.revenue;

import com.afrinuts.farmos.data.local.entity.RevenueEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RevenueWithBlockName {
    private RevenueEntity revenue;
    private String blockName; // Null if processing center

    public RevenueWithBlockName(RevenueEntity revenue, String blockName) {
        this.revenue = revenue;
        this.blockName = blockName;
    }

    public RevenueEntity getRevenue() { return revenue; }
    public String getBlockName() { return blockName; }

    public boolean isFromBlock() {
        return blockName != null;
    }

    public boolean isFromProcessingCenter() {
        return blockName == null;
    }

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(revenue.getHarvestDate()));
    }

    public String getFormattedAmount() {
        return String.format(Locale.getDefault(), "%,.0f XAF", revenue.getTotalAmount());
    }

    public String getFormattedQuantity() {
        return String.format(Locale.getDefault(), "%.1f kg", revenue.getQuantityKg());
    }

    public String getSourceDisplay() {
        if (isFromBlock()) {
            return "Block " + blockName;
        } else {
            return "Processing Center";
        }
    }
}