package com.afrinuts.farmos.data.integrity;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.afrinuts.farmos.data.local.entity.IntegrityScanRunEntity;

public class IntegrityScanWorker extends Worker {
    public static final String PERIODIC_WORK_NAME = "integrity_scan_daily";
    public static final String KEY_FARM_ID = "farmId";
    public static final String KEY_TRIGGER = "triggeredBy";
    private static final String TAG = "IntegrityScanWorker";

    public IntegrityScanWorker(@NonNull Context context, @NonNull WorkerParameters params) { super(context, params); }

    @NonNull @Override public Result doWork() {
        long farmId = getInputData().getLong(KEY_FARM_ID, -1);
        if (farmId <= 0) return Result.failure();
        String trigger = getInputData().getString(KEY_TRIGGER);
        if (trigger == null) trigger = "scheduled";
        try {
            IntegrityScanRunEntity run = new IntegrityScanner(getApplicationContext()).scan(farmId, trigger);
            if ("COMPLETED".equals(run.getStatus())) return Result.success(new Data.Builder().putLong("scanId", run.getId()).build());
            return Result.retry();
        } catch (Exception e) {
            Log.e(TAG, "Unable to run integrity scan", e);
            return Result.retry();
        }
    }
}
