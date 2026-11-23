package com.afrinuts.farmos.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.afrinuts.farmos.data.local.entity.PhotoEntity;

import java.util.List;

@Dao
public interface PhotoDao {

    @Insert
    long insert(PhotoEntity photo);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(PhotoEntity photo);

    @Update
    void update(PhotoEntity photo);

    @Delete
    void delete(PhotoEntity photo);

    @Query("DELETE FROM photos WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM photos WHERE id = :id")
    PhotoEntity getPhotoById(long id);

    @Query("SELECT * FROM photos WHERE id = :id")
    LiveData<PhotoEntity> getPhotoByIdLiveData(long id);

    @Query("SELECT * FROM photos WHERE farmId = :farmId ORDER BY takenAt DESC")
    List<PhotoEntity> getPhotosByFarmId(long farmId);

    @Query("SELECT * FROM photos WHERE farmId = :farmId ORDER BY takenAt DESC")
    LiveData<List<PhotoEntity>> getPhotosByFarmIdLiveData(long farmId);

    @Query("SELECT * FROM photos WHERE blockId = :blockId ORDER BY takenAt DESC")
    List<PhotoEntity> getPhotosByBlockId(long blockId);

    @Query("SELECT * FROM photos WHERE blockId = :blockId ORDER BY takenAt DESC")
    LiveData<List<PhotoEntity>> getPhotosByBlockIdLiveData(long blockId);

    @Query("SELECT * FROM photos WHERE taskId = :taskId ORDER BY takenAt DESC")
    List<PhotoEntity> getPhotosByTaskId(long taskId);

    @Query("SELECT * FROM photos WHERE taskId = :taskId ORDER BY takenAt DESC")
    LiveData<List<PhotoEntity>> getPhotosByTaskIdLiveData(long taskId);

    @Query("SELECT * FROM photos WHERE expenseId = :expenseId ORDER BY takenAt DESC")
    List<PhotoEntity> getPhotosByExpenseId(long expenseId);

    @Query("SELECT * FROM photos WHERE expenseId = :expenseId ORDER BY takenAt DESC")
    LiveData<List<PhotoEntity>> getPhotosByExpenseIdLiveData(long expenseId);

    @Query("SELECT * FROM photos WHERE inspectionId = :inspectionId ORDER BY takenAt DESC")
    LiveData<List<PhotoEntity>> getPhotosForInspection(String inspectionId);

    @Query("SELECT COUNT(*) FROM photos WHERE blockId = :blockId")
    int getPhotoCountForBlock(long blockId);

    @Query("SELECT COUNT(*) FROM photos WHERE taskId = :taskId")
    int getPhotoCountForTask(long taskId);

    @Query("SELECT COUNT(*) FROM photos WHERE expenseId = :expenseId")
    int getPhotoCountForExpense(long expenseId);

    @Query("SELECT * FROM photos ORDER BY takenAt DESC")
    LiveData<List<PhotoEntity>> getAllPhotosLiveData();
}
