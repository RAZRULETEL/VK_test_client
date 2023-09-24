package com.mastik.vk_test_mod.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface UserDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addUser(UserEntity entity);

    @Query("DELETE FROM UserEntity WHERE id = :id")
    void deleteUserById(int id);

    @Query("SELECT * FROM UserEntity WHERE id = :id")
    UserEntity getUserById(int id);

    @Query("DELETE FROM UserEntity")
    void clearUsers();
}
