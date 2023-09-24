package com.mastik.vk_test_mod.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public
interface MessageDAO {
    @Query("SELECT * FROM MessageEntity WHERE id = :id")
    MessageEntity getByID(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessage(MessageEntity... users);

    @Query("DELETE FROM MessageEntity WHERE id = :id")
    void deleteMessageById(int id);

    @Query("DELETE FROM MessageEntity")
    void clearMessages();
}
