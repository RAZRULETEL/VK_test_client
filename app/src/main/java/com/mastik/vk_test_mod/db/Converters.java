package com.mastik.vk_test_mod.db;

import androidx.room.TypeConverter;

import com.mastik.vk_test_mod.db.attachments.FileType;

public class Converters {

    @TypeConverter
    public static long fileTypeToInt(FileType type) {
        return type.ordinal();
    }

}
