package com.mastik.vk_test_mod.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MessageContentDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addMessageHistoryRecords(MessageContentEntity... entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addMessageHistoryRecords(List<MessageContentEntity> entity);

    @Query("SELECT * from MessageContentEntity WHERE message_id = :messageId")
    List<MessageContentEntity> getMessageHistory(int messageId);

    @Query("DELETE from MessageContentEntity WHERE message_id = :messageId")
    void deleteMessageHistory(int messageId);

    @Query("DELETE FROM MessageContentEntity")
    void clearMessageHistory();
}
