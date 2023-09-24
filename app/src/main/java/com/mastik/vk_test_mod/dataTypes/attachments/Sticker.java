package com.mastik.vk_test_mod.dataTypes.attachments;

import android.content.Context;

import com.mastik.vk_test_mod.RandomTools;
import com.mastik.vk_test_mod.db.AppDatabase;
import com.mastik.vk_test_mod.db.attachments.StickerEntity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import timber.log.Timber;

public class Sticker implements MessageAttachment {
    private static final String TAG = Sticker.class.getSimpleName();
    public static final String DB_PREFIX = "sticker";
    private final int id;
    private final int stickerPackId;
    private final HashMap<Integer, String> urls = new HashMap<>();

    public Sticker(int[] sizes, String[] urls, int stickerId, int stickerPackId) {
        for (int i = 0; i < sizes.length; i++) {
            this.urls.put(sizes[i], urls[i]);
        }
        this.id = stickerId;
        this.stickerPackId = stickerPackId;
    }

    public int getStickerPackId() {
        return stickerPackId;
    }

    public int[] getSizes() {
        return urls.keySet().stream().mapToInt(Integer::intValue).toArray();
    }

    public String getUrl(int size) {
        return urls.get(size);
    }

    public StickerEntity toEntity() {
        StringBuilder urlsSizes = new StringBuilder();
        for(int size : urls.keySet()) {
            urlsSizes.append(size).append(AppDatabase.ELEMENT_JOINER).append(urls.get(size)).append(AppDatabase.ARRAY_JOINER);
        }
        urlsSizes.deleteCharAt(urlsSizes.length() - 1);
        return new StickerEntity(id, stickerPackId, urlsSizes.toString());
    }

    @Override
    public String getDBPrefix() {
        return DB_PREFIX;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void save(Context context) {
        AppDatabase.getInstance(context).getStickerDAO().addSticker(toEntity());
    }

    public static Sticker getFromJSON(JSONObject obj) {
        try {
            if(obj.has("sticker"))
                obj = obj.getJSONObject("sticker");
            int[] sizes = new int[obj.getJSONArray("images").length()];
            String[] urls = new String[obj.getJSONArray("images").length()];
            for (int i = 0; i < obj.getJSONArray("images").length(); i++) {
                String url = obj.getJSONArray("images").getJSONObject(i).getString("url");
                sizes[i] = obj.getJSONArray("images").getJSONObject(i).getInt("width");
                urls[i] = url;
            }
            int id = obj.getInt("sticker_id");
            return new Sticker(sizes, urls, id, obj.getInt("product_id"));
        } catch (JSONException e) {
            Timber.tag(TAG).e(e, obj.toString());
            return null;
        }
    }

    public static Sticker getFromEntity(StickerEntity entity) {
        int[] sizes = RandomTools.splitNonRegex(entity.getUrlSizes(), AppDatabase.ARRAY_JOINER).stream().map(str -> str.split(AppDatabase.ELEMENT_JOINER)[0]).mapToInt(Integer::parseInt).toArray();
        String[] urls = RandomTools.splitNonRegex(entity.getUrlSizes(), AppDatabase.ARRAY_JOINER).stream().map(str -> str.split(AppDatabase.ELEMENT_JOINER)[1]).toArray(String[]::new);
        return new Sticker(sizes, urls, entity.getId(), entity.getStickerPackId());
    }
}
