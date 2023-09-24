package com.mastik.vk_test_mod.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class MessageEntity {
    @PrimaryKey
    private int id;
    @ColumnInfo(name = "user_id")
    private int userId;
    @ColumnInfo(name = "peer_id")
    private int peerId;
    @ColumnInfo(name = "local_id")
    private int localId;
//    @ColumnInfo(name = "current_content_id")
//    private int currentContentId;
    @ColumnInfo(name = "action_type")
    private String actionType;
    @ColumnInfo(name = "action_user_id")
    private int actionUserId;
    private long timestamp;

    public MessageEntity(int id, int userId, int peerId, int localId, String actionType, int actionUserId, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.peerId = peerId;
        this.localId = localId;
        this.actionType = actionType;
        this.actionUserId = actionUserId;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public int getPeerId() {
        return peerId;
    }

    public int getLocalId() {
        return localId;
    }

//    public int getCurrentContentId() {
//        return currentContentId;
//    }

    public String getActionType() {
        return actionType;
    }

    public int getActionUserId() {
        return actionUserId;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
