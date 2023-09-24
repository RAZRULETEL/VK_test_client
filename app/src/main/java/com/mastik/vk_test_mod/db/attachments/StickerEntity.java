package com.mastik.vk_test_mod.db.attachments;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class StickerEntity {

    @PrimaryKey
    private int id;
    @ColumnInfo(name = "sticker_pack_id")
    private int stickerPackId;
    @ColumnInfo(name = "url_sizes")
    private String urlSizes;

    public StickerEntity(int id, int stickerPackId, String urlSizes) {
        this.id = id;
        this.stickerPackId = stickerPackId;
        this.urlSizes = urlSizes;
    }

    public int getId() {
        return id;
    }

    public int getStickerPackId() {
        return stickerPackId;
    }

    public String getUrlSizes() {
        return urlSizes;
    }
}
