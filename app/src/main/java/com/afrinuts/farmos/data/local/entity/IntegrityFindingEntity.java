package com.afrinuts.farmos.data.local.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "integrity_findings", indices = {@Index(value = {"farmId", "ruleCode", "entityId", "status"})})
public class IntegrityFindingEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long scanId;
    private long farmId;
    private String entityType;
    private String entityId;
    private String ruleCode;
    private String severity;
    private String description;
    private String expectedValue;
    private String actualValue;
    private Double expectedNumeric;
    private Double actualNumeric;
    private String status;
    private long detectedAt;
    private Long resolvedAt;
    private String resolvedBy;
    private String resolutionNotes;
    private boolean isSynced;
    private long updatedAt;
    private String historyJson;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getScanId() { return scanId; }
    public void setScanId(long scanId) { this.scanId = scanId; }
    public long getFarmId() { return farmId; }
    public void setFarmId(long farmId) { this.farmId = farmId; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getExpectedValue() { return expectedValue; }
    public void setExpectedValue(String expectedValue) { this.expectedValue = expectedValue; }
    public String getActualValue() { return actualValue; }
    public void setActualValue(String actualValue) { this.actualValue = actualValue; }
    public Double getExpectedNumeric() { return expectedNumeric; }
    public void setExpectedNumeric(Double expectedNumeric) { this.expectedNumeric = expectedNumeric; }
    public Double getActualNumeric() { return actualNumeric; }
    public void setActualNumeric(Double actualNumeric) { this.actualNumeric = actualNumeric; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getDetectedAt() { return detectedAt; }
    public void setDetectedAt(long detectedAt) { this.detectedAt = detectedAt; }
    public Long getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Long resolvedAt) { this.resolvedAt = resolvedAt; }
    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }
    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public String getHistoryJson() { return historyJson; }
    public void setHistoryJson(String historyJson) { this.historyJson = historyJson; }
}
