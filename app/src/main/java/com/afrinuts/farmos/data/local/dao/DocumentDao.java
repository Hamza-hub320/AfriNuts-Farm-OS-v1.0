package com.afrinuts.farmos.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.afrinuts.farmos.data.local.entity.DocumentEntity;

import java.util.List;

@Dao
public interface DocumentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(DocumentEntity document);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertAbort(DocumentEntity document);

    @Update
    void update(DocumentEntity document);

    @Delete
    void delete(DocumentEntity document);

    @Query("DELETE FROM compliance_documents WHERE documentId = :documentId")
    void deleteById(String documentId);

    // Get all documents for a farm
    @Query("SELECT * FROM compliance_documents WHERE farmId = :farmId ORDER BY issueDate DESC")
    List<DocumentEntity> getDocumentsByFarmId(long farmId);

    @Query("SELECT * FROM compliance_documents WHERE farmId = :farmId ORDER BY issueDate DESC")
    LiveData<List<DocumentEntity>> getDocumentsByFarmIdLiveData(long farmId);

    // Get documents for a specific certification
    @Query("SELECT * FROM compliance_documents WHERE certificationId = :certificationId ORDER BY issueDate DESC")
    List<DocumentEntity> getDocumentsByCertificationId(String certificationId);

    @Query("SELECT * FROM compliance_documents WHERE certificationId = :certificationId ORDER BY issueDate DESC")
    LiveData<List<DocumentEntity>> getDocumentsByCertificationIdLiveData(String certificationId);

    // Get single document
    @Query("SELECT * FROM compliance_documents WHERE documentId = :documentId")
    DocumentEntity getDocumentById(String documentId);

    @Query("SELECT * FROM compliance_documents WHERE documentId = :documentId")
    LiveData<DocumentEntity> getDocumentByIdLiveData(String documentId);

    // Filter by type
    @Query("SELECT * FROM compliance_documents WHERE farmId = :farmId AND documentType = :type ORDER BY issueDate DESC")
    List<DocumentEntity> getDocumentsByType(long farmId, String type);

    // Filter by status
    @Query("SELECT * FROM compliance_documents WHERE farmId = :farmId AND status = :status ORDER BY issueDate DESC")
    List<DocumentEntity> getDocumentsByStatus(long farmId, String status);

    // Get active documents (approved and not expired)
    @Query("SELECT * FROM compliance_documents WHERE farmId = :farmId AND status = 'APPROVED' AND expiryDate > :currentTime ORDER BY issueDate DESC")
    List<DocumentEntity> getActiveDocuments(long farmId, long currentTime);

    // Get expiring documents (within 90 days)
    @Query("SELECT * FROM compliance_documents WHERE farmId = :farmId AND status = 'APPROVED' AND expiryDate > :currentTime AND expiryDate <= :expiryThreshold ORDER BY expiryDate ASC")
    List<DocumentEntity> getExpiringDocuments(long farmId, long currentTime, long expiryThreshold);

    // Count by status
    @Query("SELECT COUNT(*) FROM compliance_documents WHERE farmId = :farmId AND status = :status")
    int countByStatus(long farmId, String status);

    // Sync related
    @Query("SELECT * FROM compliance_documents WHERE isSynced = 0 ORDER BY createdAt ASC")
    List<DocumentEntity> getUnsyncedDocuments();

    @Query("UPDATE compliance_documents SET isSynced = 1 WHERE documentId = :documentId")
    void markAsSynced(String documentId);

    @Query("SELECT documentId FROM compliance_documents " +
            "WHERE documentId LIKE :prefix || '%' ORDER BY documentId DESC LIMIT 1")
    String getMaxDocumentIdForPrefix(String prefix);
}