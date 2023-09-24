package com.mastik.vk_test_mod.db.attachments;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface StickerDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addSticker(StickerEntity entity);

    @Query("DELETE FROM StickerEntity WHERE id = :id")
    void removeSticker(int id);

    @Query("SELECT * FROM StickerEntity WHERE id = :id")
    StickerEntity getStickerById(int id);

    @Query("DELETE FROM StickerEntity")
    void clearStickers();
}
