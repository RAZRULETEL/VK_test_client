package com.mastik.vk_test_mod.db.attachments;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity
public class VideoEntity {
    @Ignore
    public static final int CAN_LIKE_FLAG = 1, CAN_REPOST_FLAG = 2, CAN_EDIT_FLAG = 4, IS_PRIVATE_FLAG = 8;

    @PrimaryKey
    private int id;
    private String title;
    private String description;
    @ColumnInfo(name = "owner_id")
    private int ownerId;
    @ColumnInfo(name = "access_key")
    private String accessKey;
    private String links;
    private int flags;
    private int width;
    private int height;
    private int duration;
    private long timestamp;
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    private byte[] preview;

    public VideoEntity(int id, String title, String description, int ownerId, String accessKey, String links, int flags, int width, int height, int duration, long timestamp, byte[] preview) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.ownerId = ownerId;
        this.accessKey = accessKey;
        this.links = links;
        this.flags = flags;
        this.width = width;
        this.height = height;
        this.duration = duration;
        this.timestamp = timestamp;
        this.preview = preview;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getLinks() {
        return links;
    }

    public int getFlags() {
        return flags;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getDuration() {
        return duration;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public byte[] getPreview() {
        return preview;
    }
}
