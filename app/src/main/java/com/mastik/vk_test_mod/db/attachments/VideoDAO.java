package com.mastik.vk_test_mod.db.attachments;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface VideoDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addVideo(VideoEntity entity);

    @Query("DELETE FROM VideoEntity WHERE id = :id")
    void removeVideo(int id);

    @Query("SELECT * FROM VideoEntity WHERE id = :id")
    VideoEntity getVideoById(int id);

    @Query("DELETE FROM VideoEntity")
    void clearVideos();
}

