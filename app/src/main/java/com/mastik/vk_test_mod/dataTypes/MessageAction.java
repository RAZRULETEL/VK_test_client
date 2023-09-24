package com.mastik.vk_test_mod.dataTypes;

public enum MessageAction {
    KICK("chat_kick_user"),
        PIN("chat_pin_message"),
        INVITE("chat_invite_user"),
        UNPIN("chat_unpin_message"),
        UPDATE_PHOTO("chat_photo_update"),
        DELETE_PHOTO("chat_photo_remove"),
        RENAME("chat_title_update");


    private final String actionText;

    MessageAction(String text) {
        actionText = text;
    }

    @Override
    public String toString() {
        return actionText;
    }

    public static MessageAction fromText(String text) {
        for(MessageAction action : MessageAction.values())
            if(action.actionText.equals(text))
                return action;
        return null;
    }
}
