package com.afrinuts.farmos.ui.tasks;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.TaskEntity;

public class TaskIconHelper {

    public static int getStatusIconResId(TaskEntity.TaskStatus status) {
        switch (status) {
            case PENDING:
                return R.drawable.ic_hourglass_empty;
            case IN_PROGRESS:
                return R.drawable.ic_sync;
            case COMPLETED:
                return R.drawable.ic_check_circle;
            case CANCELLED:
                return R.drawable.ic_cancel;
            default:
                return R.drawable.ic_note;
        }
    }

    public static int getTypeIconResId(TaskEntity.TaskType type) {
        switch (type) {
            case CLEARING:
                return R.drawable.ic_tractor;
            case PLOWING:
                return R.drawable.ic_grain;
            case PLANTING:
                return R.drawable.ic_grass;
            case REPLACEMENT:
                return R.drawable.ic_sync;
            case FERTILIZING:
                return R.drawable.ic_fertilizer;
            case PRUNING:
                return R.drawable.ic_cut;
            case WEEDING:
                return R.drawable.ic_grass;
            case HARVEST:
                return R.drawable.ic_download;
            case IRRIGATION:
                return R.drawable.ic_water_drop;
            case MAINTENANCE:
                return R.drawable.ic_construction;
            case OTHER:
            default:
                return R.drawable.ic_note;
        }
    }
}