package com.mastik.vk_test_mod.db.attachments;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;

@Entity(primaryKeys = {"file_type", "id", "additional_info"})
public class CachedFileEntity {

    @ColumnInfo(name = "file_type")
    private final int fileType;
    private final int id;
    @ColumnInfo(name = "additional_info")
    private final int additionalInfo;
    private final String path;

    public CachedFileEntity(int fileType, int id, int additionalInfo, String path) {
        this.fileType = fileType;
        this.id = id;
        this.additionalInfo = additionalInfo;
        this.path = path;
    }

    @Ignore
    public CachedFileEntity(FileType fileType, int id, int additionalInfo, String path) {
        this.fileType = fileType.ordinal();
        this.id = id;
        this.additionalInfo = additionalInfo;
        this.path = path;
    }

    public int getFileType() {
        return fileType;
    }

    public int getId() {
        return id;
    }

    public String getPath() {
        return path;
    }

    public int getAdditionalInfo() {
        return additionalInfo;
    }
}
