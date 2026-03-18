package com.afrinuts.farmos.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.afrinuts.farmos.data.local.entity.UserEntity;

import java.util.List;

@Dao
public interface UserDao {

    @Insert
    long insert(UserEntity user);

    @Update
    void update(UserEntity user);

    @Delete
    void delete(UserEntity user);

    @Query("DELETE FROM users WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM users WHERE id = :id")
    UserEntity getUserById(long id);

    @Query("SELECT * FROM users WHERE id = :id")
    LiveData<UserEntity> getUserByIdLiveData(long id);

    @Query("SELECT * FROM users WHERE email = :email")
    UserEntity getUserByEmail(String email);

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
}