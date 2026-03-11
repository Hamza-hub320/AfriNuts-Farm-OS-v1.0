package com.afrinuts.farmos.ui.sync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.afrinuts.farmos.data.sync.SyncService;
import com.google.android.material.snackbar.Snackbar;

public class SyncStatusReceiver extends BroadcastReceiver {

    private SyncStatusListener listener;

    public interface SyncStatusListener {
        void onSyncStatusUpdate(String status);
        void onSyncComplete();
        void onSyncError(String error);
    }

    public SyncStatusReceiver(SyncStatusListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(SyncService.ACTION_SYNC_COMPLETE)) {
            String status = intent.getStringExtra(SyncService.EXTRA_SYNC_STATUS);
            boolean completed = intent.getBooleanExtra("completed", false);

            if (listener != null) {
                if (completed) {
                    listener.onSyncComplete();
                } else {
                    listener.onSyncStatusUpdate(status);
                }
            }
        } else if (intent.getAction().equals(SyncService.ACTION_SYNC_ERROR)) {
            String error = intent.getStringExtra(SyncService.EXTRA_ERROR_MESSAGE);
            if (listener != null) {
                listener.onSyncError(error);
            }
        }
    }
}