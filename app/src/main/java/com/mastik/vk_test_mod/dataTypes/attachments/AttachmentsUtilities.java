package com.mastik.vk_test_mod.dataTypes.attachments;

import com.mastik.vk_test_mod.RandomTools;
import com.mastik.vk_test_mod.db.AppDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

import timber.log.Timber;

public class AttachmentsUtilities {
    private static final String TAG = AttachmentsUtilities.class.getSimpleName();
    private static final HashMap<String, Function<JSONObject, MessageAttachment>> attachmentsJSONParsersList = new HashMap<>();
    private static final HashMap<String, Function<Object, MessageAttachment>> attachmentsEntitiesParsersList = new HashMap<>();

    static {
        attachmentsJSONParsersList.put(Photo.DB_PREFIX, Photo::getFromJSON);
        attachmentsJSONParsersList.put(Sticker.DB_PREFIX, Sticker::getFromJSON);
        attachmentsJSONParsersList.put(Video.DB_PREFIX, Video::getFromJSON);

        attachmentsEntitiesParsersList.put(Photo.DB_PREFIX, obj -> Photo.getFromEntity(AppDatabase.getInstance().getPhotoDAO().getPhotoById((Integer) obj)));
        attachmentsEntitiesParsersList.put(Sticker.DB_PREFIX, obj -> Sticker.getFromEntity(AppDatabase.getInstance().getStickerDAO().getStickerById((Integer) obj)));
        attachmentsEntitiesParsersList.put(Video.DB_PREFIX, obj -> Video.getFromEntity(AppDatabase.getInstance().getVideoDAO().getVideoById((Integer) obj)));
    }

    private AttachmentsUtilities(){}

    public static List<MessageAttachment> JSONtoAttachmentList(JSONArray array){
        if(array == null || array.length() == 0)
            return null;
        ArrayList<MessageAttachment> attachments = new ArrayList<>();
        for (int i = 0; i < array.length(); i++){
            try {
                JSONObject object = array.getJSONObject(i);
                Function<JSONObject, MessageAttachment> builderMethod =  attachmentsJSONParsersList.get(object.getString("type"));
                if(builderMethod == null)
                    continue;
                MessageAttachment attachment = builderMethod.apply(object);
                if(attachment != null)
                    attachments.add(attachment);
            } catch (JSONException e) {
                Timber.tag(TAG).e(e, array.toString());
            }
        }
        return attachments;
    }

    public static String attachmentsToIds(List<MessageAttachment> attachments){
        if(attachments == null || attachments.size() == 0)
            return null;
        StringBuilder res = new StringBuilder();
        for(MessageAttachment attachment : attachments){
            res.append(attachment.getDBPrefix()).append(AppDatabase.ELEMENT_JOINER).append(attachment.getId()).append(AppDatabase.ARRAY_JOINER);
        }
        return res.deleteCharAt(res.length()-1).toString();
    }

    public static List<MessageAttachment> attachmentsFromIds(String ids){
        if(ids == null || ids.isEmpty() || ids.equals("null"))
            return null;
        List<MessageAttachment> attachments = new ArrayList<>();
        for(String dbId : RandomTools.splitNonRegex(ids, AppDatabase.ARRAY_JOINER)){
            String prefix = dbId.split(AppDatabase.ELEMENT_JOINER)[0];
            int id = Integer.parseInt(dbId.split(AppDatabase.ELEMENT_JOINER)[1]);
            if(attachmentsEntitiesParsersList.get(prefix) == null)
                Timber.tag(TAG).w("Unknown attachment type: %s", prefix);
            else
                attachments.add(attachmentsEntitiesParsersList.get(prefix).apply(id));
        }
        return attachments;
    }
}
