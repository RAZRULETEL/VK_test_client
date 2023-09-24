package com.mastik.vk_test_mod;

import android.content.Context;

import com.mastik.vk_test_mod.dataTypes.VKDialog;
import com.mastik.vk_test_mod.dataTypes.VKImage;
import com.mastik.vk_test_mod.dataTypes.VKMessage;
import com.mastik.vk_test_mod.dataTypes.VKUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class RandomTools {
    private static final String TAG = RandomTools.class.getSimpleName();

    private RandomTools() {}

    public static List<String> splitNonRegex(String input, String delim)
    {
        List<String> l = new ArrayList<>();
        int offset = 0;

        while (true)
        {
            int index = input.indexOf(delim, offset);
            if (index == -1)
            {
                l.add(input.substring(offset));
                return l;
            } else
            {
                l.add(input.substring(offset, index));
                offset = (index + delim.length());
            }
        }
    }

    public static List<VKDialog> JSONToVKDialogs(JSONArray dialogsArray, Context context) {
        ArrayList<VKDialog> res = new ArrayList<>();
        for (int i = 0; i < dialogsArray.length(); i++) {
            try {
                JSONObject dialog = dialogsArray.getJSONObject(i).getJSONObject("conversation");
                JSONObject lastMessage = dialogsArray.getJSONObject(i).getJSONObject("last_message");
                VKDialog vkDialog;
                switch (dialog.getJSONObject("peer").getString("type")) {
                    case "user", "group" -> {
                        int userId = dialog.getJSONObject("peer").getInt("id");
                        VKUser user = VKUser.getById(userId, context);
                        vkDialog = new VKDialog(user.getFullName(), VKImage.getDefault(context).getImg(), userId);
                        if(user.getAvatarUrl() != null)
                            VKImage.get(user.getAvatarUrl(), userId, VKUser.PREFIX, context).init(context).addOnInitListener(img -> vkDialog.setLogo(img.getRoundedCorner()));
                        else
                            vkDialog.setLogo(VKImage.getDefault(context).getImg());
                    }
                    case "chat" -> {
                        vkDialog = new VKDialog(dialog.getJSONObject("chat_settings").getString("title"), VKImage.getDefault(context).getImg(), dialog.getJSONObject("peer").getInt("id"));
                        VKDialog finalVkDialog1 = vkDialog;
                        VKImage.get(dialog.getJSONObject("chat_settings").has("photo") ? dialog.getJSONObject("chat_settings").getJSONObject("photo").getString("photo_50") : "default", dialog.getJSONObject("peer").getInt("id"), VKUser.PREFIX, context)
                                .init(context).addOnInitListener(img -> finalVkDialog1.setLogo(img.getRoundedCorner()));
                    }
                    default ->
                            vkDialog = new VKDialog(dialog.getJSONObject("peer").getString("id"), VKImage.getDefault(context).getImg(), dialog.getJSONObject("peer").getInt("id"));
                }
                vkDialog.addMessage(VKMessage.getFromJSON(lastMessage, vkDialog).save(context));
                vkDialog.setUnreadCount(dialog.getInt("out_read_cmid") - dialog.getInt("in_read_cmid") );
                res.add(vkDialog);
            } catch (JSONException e) {
                Timber.tag(TAG).e(e, dialogsArray.toString());
            }
        }
        return res;
    }
}
