package com.mastik.vk_test_mod.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;

@Entity(primaryKeys = {"message_id", "timestamp"})
public class MessageContentEntity {

    @ColumnInfo(name = "message_id")
    private int messageId;
    private String text;
    @ColumnInfo(name = "attachments_ids")
    private String attachmentsIds;
    @ColumnInfo(name = "reply_id")
    private int replyId;
    @ColumnInfo(name = "forwarded_messages")
    private String forwardedMessages;
    private long timestamp;


    public MessageContentEntity(int messageId, String text, String attachmentsIds, int replyId, String forwardedMessages, long timestamp) {
        this.messageId = messageId;
        this.text = text;
        this.attachmentsIds = attachmentsIds;
        this.replyId = replyId;
        this.forwardedMessages = forwardedMessages;
        this.timestamp = timestamp;
    }

    public int getMessageId() {
        return messageId;
    }

    public String getText() {
        return text;
    }

    public String getAttachmentsIds() {
        return attachmentsIds;
    }

    public int getReplyId() {
        return replyId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getForwardedMessages() {
        return forwardedMessages;
    }
}
