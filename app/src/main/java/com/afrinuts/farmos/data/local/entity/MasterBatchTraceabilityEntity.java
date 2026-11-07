package com.afrinuts.farmos.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Entity(tableName = "master_batch_traceability")
public class MasterBatchTraceabilityEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    // Master batch identifiers
    private String masterBatchNumber;    // e.g., "MB-2026-001"
    private String shipmentName;          // e.g., "Shipment to Olam - June 2026"
    private String shipmentId;            // External shipment ID

    // Aggregated data
    private double totalWeightKg;
    private int totalBatches;
    private String grades;                // "Premium, Standard"
    private String certifications;        // "Organic, GLOBALG.A.P."
    private String batchNumbersJson;      // JSON array of batch numbers
    private String batchTraceabilityIdsJson; // JSON array of traceability IDs

    private long farmId;

    // Buyer information
    private String buyer;
    private String destination;
    private long exportDate;

    private String verificationCode;

    // Tracking
    private String traceabilityUrl;
    private long createdAt;
    private long updatedAt;
    private boolean isSynced;

    // Constructor
    public MasterBatchTraceabilityEntity(String masterBatchNumber, String shipmentName) {
        this.masterBatchNumber = masterBatchNumber;
        this.shipmentName = shipmentName;
        this.batchNumbersJson = "[]";
        this.batchTraceabilityIdsJson = "[]";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.isSynced = false;
        this.traceabilityUrl = "https://www.afrinutsexport.com/trace?batchId=" + masterBatchNumber;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getMasterBatchNumber() { return masterBatchNumber; }
    public void setMasterBatchNumber(String masterBatchNumber) { this.masterBatchNumber = masterBatchNumber; }

    public String getShipmentName() { return shipmentName; }
    public void setShipmentName(String shipmentName) { this.shipmentName = shipmentName; }

    public String getShipmentId() { return shipmentId; }
    public void setShipmentId(String shipmentId) { this.shipmentId = shipmentId; }

    public double getTotalWeightKg() { return totalWeightKg; }
    public void setTotalWeightKg(double totalWeightKg) { this.totalWeightKg = totalWeightKg; }

    public int getTotalBatches() { return totalBatches; }
    public void setTotalBatches(int totalBatches) { this.totalBatches = totalBatches; }

    public String getGrades() { return grades; }
    public void setGrades(String grades) { this.grades = grades; }

    public String getCertifications() { return certifications; }
    public void setCertifications(String certifications) { this.certifications = certifications; }

    public String getBatchNumbersJson() { return batchNumbersJson; }
    public void setBatchNumbersJson(String batchNumbersJson) { this.batchNumbersJson = batchNumbersJson; }

    public String getBatchTraceabilityIdsJson() { return batchTraceabilityIdsJson; }
    public void setBatchTraceabilityIdsJson(String batchTraceabilityIdsJson) { this.batchTraceabilityIdsJson = batchTraceabilityIdsJson; }

    public String getBuyer() { return buyer; }
    public void setBuyer(String buyer) { this.buyer = buyer; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public long getExportDate() { return exportDate; }
    public void setExportDate(long exportDate) { this.exportDate = exportDate; }

    public String getTraceabilityUrl() { return traceabilityUrl; }
    public void setTraceabilityUrl(String traceabilityUrl) { this.traceabilityUrl = traceabilityUrl; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }

    public long getFarmId() { return farmId; }
    public void setFarmId(long farmId) { this.farmId = farmId; }

    // Helper methods for batch numbers
    public List<String> getBatchNumbers() {
        try {
            Type listType = new TypeToken<List<String>>(){}.getType();
            return new Gson().fromJson(batchNumbersJson, listType);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void setBatchNumbers(List<String> batchNumbers) {
        this.batchNumbersJson = new Gson().toJson(batchNumbers);
    }

    public List<Long> getBatchTraceabilityIds() {
        try {
            Type listType = new TypeToken<List<Long>>(){}.getType();
            return new Gson().fromJson(batchTraceabilityIdsJson, listType);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void setBatchTraceabilityIds(List<Long> ids) {
        this.batchTraceabilityIdsJson = new Gson().toJson(ids);
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public String getFormattedExportDate() {
        if (exportDate <= 0) return "Not exported";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new java.util.Date(exportDate));
    }

    public String getFormattedWeight() {
        return String.format(Locale.getDefault(), "%,.0f kg", totalWeightKg);
    }

    public String getBatchNumbersAsString() {
        List<String> batchNumbers = getBatchNumbers();
        return String.join(", ", batchNumbers);
    }

    public String getQrCodeUrl() {
        if (traceabilityUrl != null && !traceabilityUrl.isEmpty()) {
            return traceabilityUrl;
        }
        return "https://www.afrinutsexport.com/trace?batchId=" + masterBatchNumber;
    }
}