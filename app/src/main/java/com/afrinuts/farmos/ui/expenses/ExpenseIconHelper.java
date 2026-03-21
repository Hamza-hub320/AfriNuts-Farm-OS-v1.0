package com.afrinuts.farmos.ui.expenses;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;

public class ExpenseIconHelper {

    public static int getCategoryIconResId(ExpenseEntity.ExpenseCategory category) {
        switch (category) {
            case LAND_CLEARING:
                return R.drawable.ic_tractor;
            case PLOWING:
                return R.drawable.ic_grain;
            case SEEDLINGS:
                return R.drawable.ic_grass;
            case LABOR:
                return R.drawable.ic_people;
            case SECURITY:
                return R.drawable.ic_security;
            case FENCING:
                return R.drawable.ic_fence;
            case FERTILIZER:
                return R.drawable.ic_fertilizer;
            case IRRIGATION:
                return R.drawable.ic_water_drop;
            case EQUIPMENT:
                return R.drawable.ic_construction;
            case MAINTENANCE:
                return R.drawable.ic_maintenance;
            case PROCESSING_CENTER:
                return R.drawable.ic_factory;
            case OTHER:
                return R.drawable.ic_more_horiz;
            default:
                return R.drawable.ic_attach_money;
        }
    }
}