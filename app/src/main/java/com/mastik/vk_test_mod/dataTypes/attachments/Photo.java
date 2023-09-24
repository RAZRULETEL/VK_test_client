package com.mastik.vk_test_mod.dataTypes.attachments;

import android.content.Context;
import android.util.Log;

import com.mastik.vk_test_mod.RandomTools;
import com.mastik.vk_test_mod.dataTypes.MultiSizeImage;
import com.mastik.vk_test_mod.db.AppDatabase;
import com.mastik.vk_test_mod.db.attachments.CachedFileEntity;
import com.mastik.vk_test_mod.db.attachments.FileType;
import com.mastik.vk_test_mod.db.attachments.PhotoEntity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import timber.log.Timber;

public class Photo extends MultiSizeImage implements MessageAttachment {
    public static final String DB_PREFIX = "photo";
    private final int id, userId;
    private final String accessKey;

    public Photo(PhotoSize[] keys, int[][] sizes, String[] urls, int id, int userId, String accessKey, HashMap<PhotoSize, String> cachedPaths) {
        super(keys, sizes, urls, cachedPaths);
        this.id = id;
        this.userId = userId;
        this.accessKey = accessKey;
    }

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    @Override
    public void setCachedSize(PhotoSize size, String path) {
        AppDatabase.getInstance().getCachedFileDAO().clearByTypeAndId(FileType.PHOTO, id);
        AppDatabase.getInstance().getCachedFileDAO().addFile(new CachedFileEntity(FileType.PHOTO, id, size.ordinal(), path));
        super.setCachedSize(size, path);
    }

    public PhotoEntity toEntity(){
        StringBuilder sizesString = new StringBuilder();
        StringBuilder urlsString = new StringBuilder();
        for(PhotoSize size : PhotoSize.sortedValues()){
            if(mSizes.get(size) == null)
                continue;
            sizesString.append(getWidth(size)).append(AppDatabase.ELEMENT_JOINER).append(getHeight(size)).append(AppDatabase.ARRAY_JOINER);
            urlsString.append(size.toString()).append(AppDatabase.ELEMENT_JOINER).append(mUrls.get(size)).append(AppDatabase.ARRAY_JOINER);
        }
        sizesString.deleteCharAt(sizesString.length()-1);
        urlsString.deleteCharAt(urlsString.length()-1);
        return new PhotoEntity(id, userId, accessKey, sizesString.toString(), urlsString.toString());
    }

    @Override
    public void save(Context context){
        AppDatabase.getInstance(context).getPhotoDAO().addPhoto(toEntity());
    }

    public static Photo getFromJSON(JSONObject obj) {
        try {
            if(obj.has("photo"))
                obj = obj.getJSONObject("photo");
            PhotoSize[] vkSizes;
            int[][] sizes;
            String[] urls;
            if (obj.has("sizes")) {
                vkSizes = new PhotoSize[obj.getJSONArray("sizes").length()];
                sizes = new int[obj.getJSONArray("sizes").length()][2];
                urls = new String[obj.getJSONArray("sizes").length()];
                for (int i = 0; i < obj.getJSONArray("sizes").length(); i++) {
                    PhotoSize vkSize = PhotoSize.valueOf(obj.getJSONArray("sizes").getJSONObject(i).getString("type"));
                    int[] size = new int[]{obj.getJSONArray("sizes").getJSONObject(i).getInt("width"), obj.getJSONArray("sizes").getJSONObject(i).getInt("height")};
                    String url = obj.getJSONArray("sizes").getJSONObject(i).getString("url");
                    vkSizes[i] = vkSize;
                    sizes[i] = size;
                    urls[i] = url;
                }
            } else {
                Iterator<String> values = obj.keys();
                int i = 0;
                while (values.hasNext()) {
                    String key = values.next();
                    if (!key.startsWith("photo_"))
                        continue;
                    i++;
                }
                vkSizes = new PhotoSize[i];
                sizes = new int[i][2];
                urls = new String[i];

                values = obj.keys();
                i = 0;
                while (values.hasNext()) {
                    String key = values.next();
                    if (!key.startsWith("photo_"))
                        continue;
                    int maxSideSize = Integer.parseInt(key.split("_")[1]);
                    vkSizes[i] = PhotoSize.getClosest(maxSideSize);
                    sizes[i] = new int[]{maxSideSize, maxSideSize};
                    urls[i] = obj.getString(key);
                    i++;
                }
            }
            int id = obj.getInt("id");
            int ownerId = obj.has("owner_id") ? obj.getInt("owner_id") : 0;
            String accessKey = obj.has("access_key") ? obj.getString("access_key") : null;
            return new Photo(vkSizes, sizes, urls, id, ownerId, accessKey, new HashMap<>());
        }catch (JSONException e){
            Timber.tag(Photo.class.getSimpleName()).e(e, obj.toString());
            return null;
        }
    }

    public static Photo getFromEntity(PhotoEntity entity){
        List<String> sizesUrls = RandomTools.splitNonRegex(entity.getUrls(), AppDatabase.ARRAY_JOINER);
        PhotoSize[] keys = sizesUrls.stream().map(sizeUrl -> PhotoSize.valueOf(sizeUrl.split(AppDatabase.ELEMENT_JOINER)[0])).toArray(PhotoSize[]::new);
        String[] urls = sizesUrls.stream().map(sizeUrl -> sizeUrl.split(AppDatabase.ELEMENT_JOINER)[1]).toArray(String[]::new);
        int[][] sizes = RandomTools.splitNonRegex(entity.getSizes(), AppDatabase.ARRAY_JOINER).stream()
                .map(size -> new int[]{Integer.parseInt(size.split(AppDatabase.ELEMENT_JOINER)[0]), Integer.parseInt(size.split(AppDatabase.ELEMENT_JOINER)[1])})
                .toArray(int[][]::new);
        List<CachedFileEntity> entities = AppDatabase.getInstance().getCachedFileDAO().getByTypeAndId(FileType.PHOTO, entity.getId());
        HashMap<PhotoSize, String> paths = new HashMap<>();
        if(entities != null) {
            PhotoSize[] values = PhotoSize.values();
            for (int i = 0; i < entities.size(); i++) {
                paths.put(values[entities.get(i).getAdditionalInfo()], entities.get(i).getPath());
            }
        }
        return new Photo(keys, sizes, urls, entity.getId(), entity.getOwnerId(), entity.getAccessKey(), paths);
    }

    @Override
    public String getDBPrefix() {
        return DB_PREFIX;
    }
}
