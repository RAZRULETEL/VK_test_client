package com.mastik.vk_test_mod.dataTypes.attachments;

import android.content.Context;

public interface MessageAttachment {
    String getDBPrefix();
    int getId();
    void save(Context context);
}