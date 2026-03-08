package com.afrinuts.farmos.data.local.dao;

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

    @Query("SELECT * FROM users WHERE id = :id")
    UserEntity getUserById(long id);

    @Query("SELECT * FROM users WHERE email = :email")
    UserEntity getUserByEmail(String email);

    @Query("SELECT * FROM users WHERE role = :role")
    List<UserEntity> getUsersByRole(UserEntity.UserRole role);

    @Query("SELECT * FROM users ORDER BY name ASC")
    List<UserEntity> getAllUsers();

    @Query("SELECT COUNT(*) FROM users WHERE role = 'OWNER'")
    int getOwnerCount();

    @Query("SELECT COUNT(*) FROM users WHERE isActive = 1")
    int getActiveUserCount();
}