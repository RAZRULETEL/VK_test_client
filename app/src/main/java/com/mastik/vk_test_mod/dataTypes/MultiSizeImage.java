package com.mastik.vk_test_mod.dataTypes;

import com.mastik.vk_test_mod.dataTypes.attachments.PhotoSize;
import com.mastik.vk_test_mod.db.AppDatabase;
import com.mastik.vk_test_mod.db.attachments.CachedFileEntity;
import com.mastik.vk_test_mod.db.attachments.FileType;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;

public class MultiSizeImage implements Serializable {
    private static final long serialVersionUID = -8679740758098169020L;
    protected final HashMap<PhotoSize, int[]> mSizes = new HashMap<>();
    protected final HashMap<PhotoSize, String> mUrls = new HashMap<>();
    protected final HashMap<PhotoSize, String> mCachedPaths;

    public MultiSizeImage(PhotoSize[] keys, int[][] sizes, String[] urls, HashMap<PhotoSize, String> cachedPaths) {
        for (int i = 0; i < keys.length; i++){
            this.mSizes.put(keys[i], sizes[i]);
            this.mUrls.put(keys[i], urls[i]);
        }
        this.mCachedPaths = cachedPaths;
    }

    public int[] getSize(PhotoSize size) {
        return mSizes.get(size);
    }

    public int getWidth(PhotoSize size) {
        return mSizes.get(size)[0];
    }

    public int getHeight(PhotoSize size) {
        return mSizes.get(size)[1];
    }

    public PhotoSize[] getVKSizes(){
        return mUrls.keySet().stream().toArray(PhotoSize[]::new);
    }

    public void setCachedSize(PhotoSize size, String path){
        mCachedPaths.clear();
        mCachedPaths.put(size, path);
    }

    public HashMap<PhotoSize, String> getCachedPaths(){
        return mCachedPaths;
    }

    public PhotoSize getMaxAvailableSize(){
        return mUrls.keySet().stream().max(Comparator.comparingInt(PhotoSize::getMaxSideSize)).orElseThrow(()->new IllegalStateException("No sizes"));
    }

    public String getUrl(PhotoSize size){
        return mUrls.get(size);
    }
}
