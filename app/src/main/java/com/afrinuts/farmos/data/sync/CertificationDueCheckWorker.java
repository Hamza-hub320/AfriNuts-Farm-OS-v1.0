package com.afrinuts.farmos.data.sync;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.AuditEntity;
import com.afrinuts.farmos.data.local.entity.CertificationEntity;
import com.afrinuts.farmos.data.local.entity.DocumentEntity;
import com.afrinuts.farmos.data.local.entity.InspectionEntity;
import com.afrinuts.farmos.ui.compliance.ComplianceHubActivity;
import com.afrinuts.farmos.utils.DateFormatter;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class CertificationDueCheckWorker extends Worker {
    private static final String TAG = "CertDueCheckWorker";
    public static final String PERIODIC_WORK_NAME = "certification_due_check_work";
    private static final String CHECK_NOW_WORK_NAME = "certification_due_check_now";
    private static final String CHANNEL_ID = "farm_notifications";
    private static final String CHANNEL_NAME = "Farm Notifications";
    private static final String PREFS_NAME = "certification_due_notifications";
    private static final long DAY_MS = TimeUnit.DAYS.toMillis(1);

    private final AppDatabase database;
    private final long farmId;

    public CertificationDueCheckWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        database = AppDatabase.getInstance(context);
        farmId = getInputData().getLong("farmId", -1);
    }

    public static void enqueueNow(Context context, long farmId) {
        Data input = new Data.Builder().putLong("farmId", farmId).build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(CertificationDueCheckWorker.class)
                .setInputData(input)
                .build();
        WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(
                CHECK_NOW_WORK_NAME + "_" + farmId,
                ExistingWorkPolicy.KEEP,
                request);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (farmId <= 0) {
            Log.w(TAG, "Missing farm ID; skipping certification due check");
            return Result.failure();
        }

        try {
            long now = System.currentTimeMillis();
            List<CertificationEntity> certifications = database.certificationDao().getCertificationsByFarmId(farmId);
            List<DocumentEntity> documents = database.documentDao().getDocumentsByFarmId(farmId);
            List<AuditEntity> audits = database.auditDao().getAuditsByFarmId(farmId);
            List<InspectionEntity> inspections = database.inspectionDao().getInspectionsByFarmId(farmId);

            for (CertificationEntity certification : certifications) {
                long targetDate = certification.getTargetDate();
                if (targetDate > 0
                        && targetDate < now
                        && certification.getEffectiveStatus() == CertificationEntity.CertificationStatus.IN_PROGRESS) {
                    String key = "target_" + certification.getCertificationId() + "_" + targetDate;
                    notifyOnce(key, certification.getCertificationName(),
                            "Programme target date passed · " + DateFormatter.formatDateOrDash(targetDate));
                }

                if (certification.getStatus() == CertificationEntity.CertificationStatus.CERTIFIED
                        && certification.getExpiryDate() > 0) {
                    notifyExpiry(keyPrefix("cert", certification.getCertificationId(), certification.getExpiryDate()),
                            certification.getCertificationName(), "Certificate", certification.getExpiryDate(), now);
                }
            }

            for (DocumentEntity document : documents) {
                if (document.getExpiryDate() <= 0) continue;
                String itemName = hasText(document.getDocumentName()) ? document.getDocumentName() : "Compliance document";
                String what = document.getDocumentType() == DocumentEntity.DocumentType.CERTIFICATE
                        ? "Certificate" : "Document";
                notifyExpiry(keyPrefix("doc", document.getDocumentId(), document.getExpiryDate()),
                        itemName, what, document.getExpiryDate(), now);
            }

            return Result.success();
        } catch (Exception error) {
            Log.e(TAG, "Certification due check failed", error);
            return Result.retry();
        }
    }

    private void notifyExpiry(String keyPrefix, String itemName, String kind, long expiryDate, long now) {
        long daysUntilExpiry = (long) Math.ceil((expiryDate - now) / (double) DAY_MS);
        if (daysUntilExpiry <= 0) return;

        int band = daysUntilExpiry <= 7 ? 7 : daysUntilExpiry <= 30 ? 30 : daysUntilExpiry <= 90 ? 90 : 0;
        if (band == 0) return;
        String key = keyPrefix + "_band_" + band;
        String message = kind + " expires in " + daysUntilExpiry + " days · "
                + DateFormatter.formatDateOrDash(expiryDate);
        notifyOnce(key, itemName, message);
    }

    private String keyPrefix(String type, String id, long date) {
        return type + "_" + id + "_expiry_" + date;
    }

    private void notifyOnce(String dedupeKey, String title, String message) {
        SharedPreferences preferences = getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (preferences.getBoolean(dedupeKey, false)) return;
        if (!hasNotificationPermission()) {
            Log.i(TAG, "Notifications permission unavailable; leaving due check eligible for a later run");
            return;
        }

        createNotificationChannel();
        Intent intent = new Intent(getApplicationContext(), ComplianceHubActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(), dedupeKey.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(hasText(title) ? title : "AfriNuts Farm OS")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManager manager = (NotificationManager) getApplicationContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        manager.notify(dedupeKey.hashCode(), notification.build());
        preferences.edit().putBoolean(dedupeKey, true).commit();
    }

    private boolean hasNotificationPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Farm operation notifications");
        NotificationManager manager = getApplicationContext().getSystemService(NotificationManager.class);
        if (manager != null) manager.createNotificationChannel(channel);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
