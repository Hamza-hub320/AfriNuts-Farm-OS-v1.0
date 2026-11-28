package com.afrinuts.farmos.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "integrity_scan_runs")
public class IntegrityScanRunEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long farmId;
    private long startedAt;
    private long completedAt;
    private int recordsScanned;
    private int rulesEvaluated;
    private int issuesDetected;
    private String status;
    private String triggeredBy;
    private int pendingSyncCount;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getFarmId() { return farmId; }
    public void setFarmId(long farmId) { this.farmId = farmId; }
    public long getStartedAt() { return startedAt; }
    public void setStartedAt(long startedAt) { this.startedAt = startedAt; }
    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }
    public int getRecordsScanned() { return recordsScanned; }
    public void setRecordsScanned(int recordsScanned) { this.recordsScanned = recordsScanned; }
    public int getRulesEvaluated() { return rulesEvaluated; }
    public void setRulesEvaluated(int rulesEvaluated) { this.rulesEvaluated = rulesEvaluated; }
    public int getIssuesDetected() { return issuesDetected; }
    public void setIssuesDetected(int issuesDetected) { this.issuesDetected = issuesDetected; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(String triggeredBy) { this.triggeredBy = triggeredBy; }
    public int getPendingSyncCount() { return pendingSyncCount; }
    public void setPendingSyncCount(int pendingSyncCount) { this.pendingSyncCount = pendingSyncCount; }
}
