package com.mastik.vk_test_mod.db.attachments;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class PhotoEntity {
    @PrimaryKey
    private int id;
    @ColumnInfo(name = "owner_id")
    private int ownerId;
    @ColumnInfo(name = "access_key")
    private String accessKey;
    private String sizes;
    private String urls;

    public PhotoEntity(int id, int ownerId, String accessKey, String sizes, String urls) {
        this.id = id;
        this.ownerId = ownerId;
        this.accessKey = accessKey;
        this.sizes = sizes;
        this.urls = urls;
    }

    public int getId() {
        return id;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSizes() {
        return sizes;
    }

    public String getUrls() {
        return urls;
    }
}
