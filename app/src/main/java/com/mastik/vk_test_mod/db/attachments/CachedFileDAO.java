package com.mastik.vk_test_mod.db.attachments;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CachedFileDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addFile(CachedFileEntity entity);

    @Query("SELECT * FROM CachedFileEntity WHERE file_type = :type and id = :id")
    List<CachedFileEntity> getByTypeAndId(FileType type, int id);

    @Query("DELETE FROM CachedFileEntity")
    void clearCachedFiles();

    @Query("DELETE FROM CachedFileEntity WHERE file_type = :type and id = :id")
    void clearByTypeAndId(FileType type, int id);
}
