package com.mastik.vk_test_mod.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.mastik.vk_test_mod.db.attachments.CachedFileDAO;
import com.mastik.vk_test_mod.db.attachments.CachedFileEntity;
import com.mastik.vk_test_mod.db.attachments.PhotoDAO;
import com.mastik.vk_test_mod.db.attachments.PhotoEntity;
import com.mastik.vk_test_mod.db.attachments.StickerDAO;
import com.mastik.vk_test_mod.db.attachments.StickerEntity;
import com.mastik.vk_test_mod.db.attachments.VideoDAO;
import com.mastik.vk_test_mod.db.attachments.VideoEntity;

@Database(version = 5, entities = {MessageEntity.class, MessageContentEntity.class, UserEntity.class, StickerEntity.class, PhotoEntity.class, CachedFileEntity.class, VideoEntity.class})
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    public static final String ELEMENT_JOINER = "☺", ARRAY_JOINER = "$";
    private static AppDatabase instance;

    public static AppDatabase getInstance(Context context) {
        if(instance == null)
            instance = Room.databaseBuilder(context, AppDatabase.class, "AppDB").fallbackToDestructiveMigration().build();
        return instance;
    }

    public static AppDatabase getInstance() {
        return instance;
    }

    abstract public MessageDAO getMessageDAO();

    abstract public MessageContentDAO getMessageContentDAO();

    abstract public UserDAO getUserDAO();

    abstract public PhotoDAO getPhotoDAO();

    abstract public StickerDAO getStickerDAO();

    abstract public CachedFileDAO getCachedFileDAO();

    abstract public VideoDAO getVideoDAO();
}
