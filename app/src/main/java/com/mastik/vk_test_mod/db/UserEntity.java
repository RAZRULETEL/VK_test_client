package com.mastik.vk_test_mod.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class UserEntity {
    @PrimaryKey
    private int id;
    @ColumnInfo(name = "first_name")
    private String firstName;
    @ColumnInfo(name = "last_name")
    private String lastName;
    @ColumnInfo(name = "screen_name")
    private String screenName;
    @ColumnInfo(name = "is_friend")
    private boolean isFriend;
    @ColumnInfo(name = "is_male")
    private boolean isMale;
    @ColumnInfo(name = "avatar_url")
    private String avatarUrl;
    @ColumnInfo(name = "last_seen")
    private long lastSeen;

    public UserEntity(int id, String firstName, String lastName, String screenName, boolean isFriend, boolean isMale, String avatarUrl, long lastSeen) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.screenName = screenName;
        this.isFriend = isFriend;
        this.isMale = isMale;
        this.avatarUrl = avatarUrl;
        this.lastSeen = lastSeen;
    }

    public int getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getScreenName() {
        return screenName;
    }

    public boolean isFriend() {
        return isFriend;
    }

    public boolean isMale() {
        return isMale;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public long getLastSeen() {
        return lastSeen;
    }
}
