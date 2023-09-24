package com.mastik.vk_test_mod.dataTypes;

import android.content.Context;

import com.mastik.vk_test_mod.MainActivity;
import com.mastik.vk_test_mod.dataTypes.attachments.AttachmentsUtilities;
import com.mastik.vk_test_mod.dataTypes.attachments.MessageAttachment;
import com.mastik.vk_test_mod.dataTypes.attachments.Photo;
import com.mastik.vk_test_mod.dataTypes.attachments.Sticker;
import com.mastik.vk_test_mod.dataTypes.attachments.Video;
import com.mastik.vk_test_mod.db.AppDatabase;
import com.mastik.vk_test_mod.db.MessageContentEntity;
import com.mastik.vk_test_mod.db.MessageEntity;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import timber.log.Timber;

public class VKMessage implements Comparable<VKMessage> {
    private static final HashMap<Class<? extends MessageAttachment>, String> attachmentsToText = new HashMap<>();
    private static final HashMap<Integer, VKMessage> loadedMessages = new HashMap<>();
    private final HashMap<Instant, MessageContent> editHistory;
    private MessageContent currentContent;
    private final VKDialog peer;
    private final int userId, localId, globalId;
    private Instant timestamp;
    private final MessageAction action;
    private final int actionMemberId;

    static {
        attachmentsToText.put(Photo.class, "[ фотография ]");
        attachmentsToText.put(Sticker.class, "[ стикер ]");
        attachmentsToText.put(Video.class, "[ видео ]");
    }

//    public VKMessage(String text, List<MessageAttachment> attachments, int sender_id, VKDialog peer, int conversation_id, int global_id, Instant timestamp) {
//        this(text, attachments, sender_id, peer, conversation_id, global_id, timestamp, -1, null, null, 0, null);
//    }
//
//    public VKMessage(String text, List<MessageAttachment> attachments, int sender_id, VKDialog peer, int conversation_id, int global_id, Instant timestamp, int reply_to_id) {
//        this(text, attachments, sender_id, peer, conversation_id, global_id, timestamp, reply_to_id, null, null, 0, null);
//    }

    public VKMessage(int sender_id, VKDialog peer, int conversation_id, int global_id, Instant timestamp, HashMap<Instant, MessageContent> editHistory, MessageAction action, int actionMemberId) {
        this(null, null, sender_id, peer, conversation_id, global_id, timestamp, null, editHistory, action, actionMemberId, null);
    }

    public VKMessage(String text, List<MessageAttachment> attachments, int sender_id, VKDialog peer, int conversation_id, int global_id, Instant timestamp, VKMessage replyMessage, HashMap<Instant, MessageContent> editHistory, MessageAction action, int actionMemberId, List<VKMessage> forwardedMessages) {
        currentContent = editHistory == null || editHistory.size() == 0 ? new MessageContent(text, attachments, forwardedMessages, replyMessage) : editHistory.get(editHistory.keySet().stream().max(Instant::compareTo).get());
        this.userId = sender_id;
        this.peer = peer;
        this.localId = conversation_id;
        this.globalId = global_id;
        this.timestamp = timestamp;
        this.editHistory = editHistory == null ? new HashMap<>() : editHistory;
        this.action = action;
        this.actionMemberId = actionMemberId;
        this.editHistory.put(timestamp, currentContent);
    }

    public void editMessage(String text, Instant timestamp) {
        editMessage(text, currentContent.attachments(), currentContent.replyMessage, currentContent.forwardedMessages, timestamp);
    }

    public void editMessage(String text, List<MessageAttachment> attachments, Instant timestamp) {
        editMessage(text, attachments, currentContent.replyMessage, currentContent.forwardedMessages, timestamp);
    }

    public void editMessage(String text, List<MessageAttachment> attachments, VKMessage replyMessage, Instant timestamp) {
        editMessage(text, attachments, replyMessage, currentContent.forwardedMessages, timestamp);
    }

    public void editMessage(String text, List<MessageAttachment> attachments, VKMessage replyMessage, List<VKMessage> forwardedMessages, Instant timestamp) {
        MessageContent newContent = new MessageContent(text, attachments, forwardedMessages, replyMessage);
        editHistory.put(timestamp, newContent);
        currentContent = newContent;
        this.timestamp = timestamp;
    }

    public String getText() {
        return currentContent.text();
    }

    public List<MessageAttachment> getAttachments() {
        return currentContent.attachments();
    }

    public int getUserId() {
        return userId;
    }

    public VKDialog getDialog() {
        return peer;
    }

    public int getPeerId() {
        return peer.getId();
    }

    public int getLocalId() {
        return localId;
    }

    public int getGlobalId() {
        return globalId;
    }

    public List<VKMessage> getForwardedMessages() {
        return currentContent.forwardedMessages();
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public boolean isReply() {
        return currentContent.replyMessage != null;
    }

    public int getReplyToId() {
        return currentContent.replyMessage != null ? currentContent.replyMessage.getGlobalId() : -1;
    }

    public VKMessage getReplyMessage(){
        return currentContent.replyMessage;
    }

    public HashMap<Instant, MessageContent> getHistory() {
        return new HashMap<>(editHistory);
    }

    /**
     * @return true if message sent by current logged user, false otherwise
     */
    public boolean isOutgoing() {
        return userId == MainActivity.getCurrentUserId();
    }

    public boolean hasAction() {
        return action != null;
    }

    public MessageAction getAction() {
        return action;
    }

    public int getMemberId() {
        return actionMemberId;
    }

    public VKMessage save(Context context) {
        AppDatabase.getInstance(context).getMessageDAO().insertMessage(VKMessage.toEntity(this));
        AppDatabase.getInstance(context).getMessageContentDAO().addMessageHistoryRecords(MessageContent.historyToEntities(this.editHistory, this.globalId));
        if (currentContent.forwardedMessages() != null && currentContent.forwardedMessages().size() > 0)
            for (VKMessage forwardMessage : currentContent.forwardedMessages())
                forwardMessage.save(context);
        if(currentContent.replyMessage != null)
            currentContent.replyMessage.save(context);
        if(currentContent.attachments() != null && currentContent.attachments().size() > 0)
            for (MessageAttachment attachment : currentContent.attachments())
                attachment.save(context);
        return this;
    }

    public String getExtendedText() {
        StringBuilder resText = new StringBuilder(currentContent.text);
        if (getForwardedMessages() != null && getForwardedMessages().size() > 0)
            resText.append(" [ ").append(getForwardedMessages().size()).append(" пересланных сообщения ]");
        if (getAttachments() != null && getAttachments().size() > 0) {
            for (MessageAttachment attachment : getAttachments())
                resText.append(attachmentsToText.get(attachment.getClass())).append(" ");
        }
        if(action != null)
            resText.append(VKUser.getById(actionMemberId, null).getFullName()).append(" ").append(MainActivity.getAction(action.toString()));
        return resText.toString();
    }

    @Override
    public int compareTo(VKMessage vkMessage) {
        return globalId - vkMessage.globalId;
    }

    /**
     * Converts VKMessage to MessageEntity that can be saved in db <br> Warning: need to manually save edit history
     *
     * @param message that will be converted
     * @return valid entity to save in db
     */
    public static MessageEntity toEntity(VKMessage message) {
        return new MessageEntity(message.getGlobalId(), message.getUserId(), message.getPeerId(), message.getLocalId(), message.getAction() == null ? null : message.getAction().toString(), message.getMemberId(), message.getTimestamp().toEpochMilli());
    }

    public static VKMessage getById(int id, VKDialog peer) {
        if (loadedMessages.get(id) != null)
            return loadedMessages.get(id);
        MessageEntity entity = AppDatabase.getInstance().getMessageDAO().getByID(id);
        return entity == null ? null : getFromEntity(entity, peer);
    }

    public static VKMessage getFromEntity(MessageEntity entity, VKDialog peer) {
        List<MessageContentEntity> rawEditHistory = AppDatabase.getInstance().getMessageContentDAO().getMessageHistory(entity.getId());
        HashMap<Instant, MessageContent> editHistory = MessageContent.entitiesToHistory(rawEditHistory, peer);
        return new VKMessage(entity.getUserId(), peer, entity.getLocalId(), entity.getId(), Instant.ofEpochMilli(entity.getTimestamp()), editHistory, MessageAction.fromText(entity.getActionType()), entity.getActionUserId());
    }

    public static VKMessage getFromJSON(JSONObject messageObject, VKDialog peer) throws JSONException {
        VKMessage msg = VKMessage.getById(messageObject.getInt("id"), peer);
        if (msg != null) {
            if (msg.getTimestamp().toEpochMilli() < messageObject.getLong("date") * 1000) {
                VKMessage replyMessage = null;
                if(msg.getReplyToId() != (messageObject.has("reply_message") ? messageObject.getJSONObject("reply_message").getInt("id") : -1))
                    replyMessage = VKMessage.getFromJSON(messageObject.getJSONObject("reply_message"), peer);
                msg.editMessage(messageObject.getString("text"),
                        AttachmentsUtilities.JSONtoAttachmentList(messageObject.getJSONArray("attachments")),
                        replyMessage,
                        Instant.ofEpochMilli(messageObject.getLong("date") * 1000));
            }
            if(msg.getTimestamp().toEpochMilli() == messageObject.getLong("date") * 1000 && messageObject.has("reply_message") && msg.getReplyToId() != messageObject.getJSONObject("reply_message").getInt("id")){
                VKMessage replyMessage = null;
                if(msg.getReplyToId() != (messageObject.has("reply_message") ? messageObject.getJSONObject("reply_message").getInt("id") : -1))
                    replyMessage = VKMessage.getFromJSON(messageObject.getJSONObject("reply_message"), peer);
                msg.editMessage(messageObject.getString("text"),
                        AttachmentsUtilities.JSONtoAttachmentList(messageObject.getJSONArray("attachments")),
                        replyMessage,
                        Instant.ofEpochMilli(messageObject.getLong("date") * 1000));
            }
        } else {
            List<VKMessage> forwardedMessages = null;
            if (messageObject.has("fwd_messages") && messageObject.getJSONArray("fwd_messages").length() > 0) {
                forwardedMessages = new ArrayList<>();
                for (int i = 0; i < messageObject.getJSONArray("fwd_messages").length(); i++) {
                    JSONObject forwardedMessage = messageObject.getJSONArray("fwd_messages").getJSONObject(i);
                    if(!forwardedMessage.has("id"))//Messages forwarded from chats that you don't have don't have global id
                        forwardedMessage.put("id", -messageObject.getInt("id"));//Add id to make message correct and valid for save in db
                    VKMessage vkMessage = VKMessage.getFromJSON(forwardedMessage, peer);
                    forwardedMessages.add(vkMessage);
                }
            }
            VKMessage replyMessage = null;
            if(messageObject.has("reply_message"))
                replyMessage = VKMessage.getFromJSON(messageObject.getJSONObject("reply_message"), peer);
            msg = new VKMessage(
                    messageObject.getString("text"),
                    AttachmentsUtilities.JSONtoAttachmentList(messageObject.getJSONArray("attachments")),
                    messageObject.getInt("from_id"),
                    peer,
                    messageObject.getInt("conversation_message_id"),
                    messageObject.getInt("id"),
                    Instant.ofEpochMilli(messageObject.getLong("date") * 1000),
                    replyMessage,
                    null,
                    messageObject.has("action") ? MessageAction.fromText(messageObject.getJSONObject("action").getString("type")) : null,
                    messageObject.has("action") ? messageObject.getJSONObject("action").getInt("member_id") : -1,
                    forwardedMessages);
        }
        return msg;
    }

    private static final class MessageContent {
        private final String text;
        private final List<MessageAttachment> attachments;
        private final List<VKMessage> forwardedMessages;
        private final VKMessage replyMessage;

        private MessageContent(String text, List<MessageAttachment> attachments,
                               List<VKMessage> forwardedMessages, VKMessage replyMessage) {
            this.text = text;
            this.attachments = attachments;
            this.forwardedMessages = forwardedMessages;
            this.replyMessage = replyMessage;
        }

        public static HashMap<Instant, MessageContent> entitiesToHistory(List<MessageContentEntity> entities, VKDialog peer) {
                HashMap<Instant, MessageContent> editHistory = new HashMap<>();
                for (MessageContentEntity entity : entities) {
                    List<VKMessage> forwardedMessages = null;
                    if (entity.getForwardedMessages() != null && !entity.getForwardedMessages().equals("null") && entity.getForwardedMessages().split(",").length > 0) {
                        forwardedMessages = new ArrayList<>();
                        for (String messageId : entity.getForwardedMessages().split(","))
                            forwardedMessages.add(getById(Integer.parseInt(messageId), peer));
                    }
                    editHistory.put(Instant.ofEpochMilli(entity.getTimestamp()), new MessageContent(entity.getText(), AttachmentsUtilities.attachmentsFromIds(entity.getAttachmentsIds()), forwardedMessages, VKMessage.getById(entity.getReplyId(), peer)));
                }
                return editHistory;
            }

            public static List<MessageContentEntity> historyToEntities(HashMap<Instant, MessageContent> history, int messageId) {
                ArrayList<MessageContentEntity> entities = new ArrayList<>();
                for (Instant date : history.keySet()) {
                    MessageContent content = history.get(date);
                    String attachmentsIds = AttachmentsUtilities.attachmentsToIds(content.attachments);
                    StringBuilder forwardedIds = new StringBuilder();
                    if (content.forwardedMessages() != null && content.forwardedMessages().size() > 0) {
                        for (VKMessage forwarded : content.forwardedMessages())
                            forwardedIds.append(forwarded.getGlobalId()).append(",");
                        forwardedIds.delete(forwardedIds.length() - 1, forwardedIds.length());
                    } else
                        forwardedIds = null;
                    entities.add(new MessageContentEntity(messageId, content.text(), attachmentsIds, content.replyMessage == null ? -1 : content.replyMessage.globalId, String.valueOf(forwardedIds), date.toEpochMilli()));
                }
                return entities;
            }

        public String text() {
            return text;
        }

        public List<MessageAttachment> attachments() {
            return attachments;
        }

        public List<VKMessage> forwardedMessages() {
            return forwardedMessages;
        }

        public VKMessage replyMessage() {
            return replyMessage;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (MessageContent) obj;
            return Objects.equals(this.text, that.text) &&
                    Objects.equals(this.attachments, that.attachments) &&
                    Objects.equals(this.forwardedMessages, that.forwardedMessages) &&
                    Objects.equals(this.replyMessage, that.replyMessage);
        }

        @Override
        public int hashCode() {
            return Objects.hash(text, attachments, forwardedMessages, replyMessage);
        }

        @Override
        public String toString() {
            return "MessageContent[" +
                    "text=" + text + ", " +
                    "attachments=" + attachments + ", " +
                    "forwardedMessages=" + forwardedMessages + ", " +
                    "replyMessage=" + replyMessage + ']';
        }

        }

    @Override
    public String toString() {
        return "VKMessage{" +
                "editHistory=" + editHistory +
                ", currentContent=" + currentContent +
                ", peer=" + peer.getId() +
                ", userId=" + userId +
                ", localId=" + localId +
                ", globalId=" + globalId +
                ", timestamp=" + timestamp +
                ", action=" + action +
                ", actionMemberId=" + actionMemberId +
                '}';
    }
}
