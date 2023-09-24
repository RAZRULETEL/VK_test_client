package com.mastik.vk_test_mod.dataTypes.listeners;

import com.mastik.vk_test_mod.dataTypes.VKMessage;

public interface MessageListener {
    void onNewMessage(VKMessage msg);
}
