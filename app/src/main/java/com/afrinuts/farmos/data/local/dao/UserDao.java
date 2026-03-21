package com.afrinuts.farmos.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.afrinuts.farmos.data.local.entity.UserEntity;

import java.util.List;

@Dao
public interface UserDao {

    @Insert
    long insert(UserEntity user);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(UserEntity user);

    @Update
    void update(UserEntity user);

    @Delete
    void delete(UserEntity user);

    // FIXED: Changed 'id' to 'localId'
    @Query("DELETE FROM users WHERE localId = :localId")
    void deleteById(long localId);

    // FIXED: Changed 'id' to 'localId'
    @Query("SELECT * FROM users WHERE localId = :localId")
    UserEntity getUserById(long localId);

    // FIXED: Changed 'id' to 'localId'
    @Query("SELECT * FROM users WHERE localId = :localId")
    LiveData<UserEntity> getUserByIdLiveData(long localId);

    @Query("SELECT * FROM users WHERE email = :email")
    UserEntity getUserByEmail(String email);

    @Query("SELECT * FROM users WHERE uid = :uid")
    UserEntity getUserByUid(String uid);

    @Query("SELECT * FROM users WHERE uid = :uid")
    LiveData<UserEntity> getUserByUidLiveData(String uid);

    @Query("SELECT * FROM users WHERE role = :role")
    List<UserEntity> getUsersByRole(UserEntity.UserRole role);

    @Query("SELECT * FROM users WHERE role = :role")
    LiveData<List<UserEntity>> getUsersByRoleLiveData(UserEntity.UserRole role);

    @Query("SELECT * FROM users ORDER BY name ASC")
    List<UserEntity> getAllUsers();

    @Query("SELECT * FROM users ORDER BY name ASC")
    LiveData<List<UserEntity>> getAllUsersLiveData();

    @Query("SELECT * FROM users WHERE isActive = 1")
    LiveData<List<UserEntity>> getActiveUsersLiveData();

    @Query("SELECT COUNT(*) FROM users WHERE role = 'OWNER'")
    int getOwnerCount();

    @Query("SELECT COUNT(*) FROM users WHERE isActive = 1")
    int getActiveUserCount();

    @Query("SELECT * FROM users WHERE localId = :localId LIMIT 1")
    UserEntity getUserByLocalId(long localId);
}