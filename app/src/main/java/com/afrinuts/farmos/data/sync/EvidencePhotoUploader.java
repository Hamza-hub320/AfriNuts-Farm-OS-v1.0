package com.afrinuts.farmos.data.sync;

import android.util.Log;

import com.afrinuts.farmos.data.local.entity.SupplierDisputeEntity;
import com.afrinuts.farmos.data.local.entity.SupplierInspectionEntity;
import com.afrinuts.farmos.data.remote.FirebaseManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Uploads local evidence files referenced by a supplier-trust record, rewrites
 * the record's evidenceUrls field from file:// to Storage URLs, then returns.
 *
 * The caller's rule (enforced by AutoSyncWorker):
 *   1. call this uploader
 *   2. if it returns a record whose evidenceUrls no longer contains file://,
 *      then and only then push the record to Firestore
 *
 * If any upload fails, the returned record still has file:// entries; the
 * caller must NOT push it. It re-queues instead. The consequence is a delay,
 * never a broken evidence link in Firestore.
 */
public class EvidencePhotoUploader {

    private static final String TAG = "EvidencePhotoUploader";
    private static final long UPLOAD_TIMEOUT_SECONDS = 90;

    private final FirebaseManager firebase;
    private final String farmId;

    public EvidencePhotoUploader(FirebaseManager firebase, String farmId) {
        this.firebase = firebase;
        this.farmId = farmId;
    }

    /** Returns true iff every entry in evidenceUrls is now a real URL. */
    public boolean allEvidenceIsRemote(List<String> urls) {
        if (urls == null || urls.isEmpty()) return true;
        for (String u : urls) {
            if (u == null || u.startsWith("file://") || u.startsWith("/")) return false;
        }
        return true;
    }

    public SupplierInspectionEntity prepareInspection(SupplierInspectionEntity i) {
        List<String> current = i.getEvidenceUrlList();
        if (allEvidenceIsRemote(current)) return i;

        List<String> uploaded = uploadAll(i.getId(), "supplier_inspections", current);
        i.setEvidenceUrls(new com.google.gson.Gson().toJson(uploaded));
        i.setUpdatedAt(System.currentTimeMillis());
        return i;
    }

    public SupplierDisputeEntity prepareDispute(SupplierDisputeEntity d) {
        List<String> current = d.getEvidenceUrlList();
        if (allEvidenceIsRemote(current)) return d;

        List<String> uploaded = uploadAll(d.getId(), "supplier_disputes", current);
        d.setEvidenceUrls(new com.google.gson.Gson().toJson(uploaded));
        d.setUpdatedAt(System.currentTimeMillis());
        return d;
    }

    /**
     * Uploads each file:// entry. On success its URL replaces the local path.
     * On failure the local path is kept, so the caller can see the record
     * still isn't ready to push.
     */
    private List<String> uploadAll(long entityId, String entityType, List<String> urls) {
        List<String> result = new ArrayList<>();
        for (String url : urls) {
            if (url == null) continue;
            if (!url.startsWith("file://") && !url.startsWith("/")) {
                result.add(url);      // already remote
                continue;
            }

            File file = new File(url.replace("file://", ""));
            if (!file.exists()) {
                Log.w(TAG, "Evidence file missing, keeping local path: " + url);
                result.add(url);
                continue;
            }

            String filename = UUID.randomUUID().toString().substring(0, 8)
                    + "_" + file.getName();

            final CountDownLatch latch = new CountDownLatch(1);
            final String[] out = {null};
            firebase.uploadEvidenceFile(file, farmId, entityType, entityId, filename,
                    new FirebaseManager.EvidenceUploadCallback() {
                        @Override public void onSuccess(String downloadUrl) {
                            out[0] = downloadUrl;
                            latch.countDown();
                        }
                        @Override public void onError(String error) {
                            Log.e(TAG, "Upload failed for " + entityId + ": " + error);
                            latch.countDown();
                        }
                    });

            try {
                if (!latch.await(UPLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    Log.e(TAG, "Upload timed out for " + entityId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            result.add(out[0] != null ? out[0] : url);
        }
        return result;
    }
}