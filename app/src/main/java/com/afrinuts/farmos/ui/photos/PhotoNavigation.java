package com.afrinuts.farmos.ui.photos;

import android.content.Context;
import android.content.Intent;

public final class PhotoNavigation {
    private PhotoNavigation() { }

    public static void openPhoto(Context context, long photoId) {
        openPhoto(context, photoId, -1, -1, -1);
    }

    public static void openPhoto(Context context, long photoId, long blockId, long taskId, long expenseId) {
        boolean tablet = context.getResources().getConfiguration().screenWidthDp >= 600;
        if (tablet) {
            Intent intent = new Intent(context, PhotoGalleryActivity.class);
            intent.putExtra(PhotoGalleryActivity.EXTRA_SELECTED_PHOTO_ID, photoId);
            if (blockId >= 0) intent.putExtra("block_id", blockId);
            if (taskId >= 0) intent.putExtra("task_id", taskId);
            if (expenseId >= 0) intent.putExtra("expense_id", expenseId);
            context.startActivity(intent);
        } else {
            Intent intent = new Intent(context, PhotoDetailActivity.class);
            intent.putExtra(PhotoDetailActivity.EXTRA_PHOTO_ID, photoId);
            context.startActivity(intent);
        }
    }
}
