package com.mastik.vk_test_mod.db.attachments;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface PhotoDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addPhoto(PhotoEntity entity);

    @Query("DELETE FROM PhotoEntity WHERE id = :id")
    void removePhoto(int id);

    @Query("SELECT * FROM PhotoEntity WHERE id = :id")
    PhotoEntity getPhotoById(int id);

    @Query("DELETE FROM PhotoEntity")
    void clearPhotos();
}
