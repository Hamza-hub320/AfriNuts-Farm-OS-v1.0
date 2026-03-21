package com.afrinuts.farmos.data.repository;

import android.content.Context;

import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.remote.FirestoreHelper;
import com.afrinuts.farmos.data.sync.SyncManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class BaseRepository {
    protected final AppDatabase database;
    protected final FirestoreHelper firestoreHelper;
    protected final SyncManager syncManager;
    protected final ExecutorService executor = Executors.newSingleThreadExecutor();
    protected final long farmId;

    // Flag to prevent sync loops
    protected boolean isRemoteUpdate = false;

    public BaseRepository(Context context, long farmId) {
        this.database = AppDatabase.getInstance(context);
        this.firestoreHelper = new FirestoreHelper(farmId);
        this.syncManager = SyncManager.getInstance(context);
        this.farmId = farmId;
    }

    public interface RepositoryCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public interface InsertCallback {
        void onSuccess(long id);
        void onError(String error);
    }

    public void shutdown() {
        executor.shutdown();
    }
}