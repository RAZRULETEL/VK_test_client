package com.mastik.vk_test_mod.dataTypes.attachments;

import android.content.Context;

import com.mastik.vk_test_mod.RandomTools;
import com.mastik.vk_test_mod.dataTypes.MultiSizeImage;
import com.mastik.vk_test_mod.dataTypes.RepostCounters;
import com.mastik.vk_test_mod.db.AppDatabase;
import com.mastik.vk_test_mod.db.attachments.VideoEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

public class Video implements MessageAttachment {
    public static final Comparator<String> QUALITY_COMPARATOR = new Comparator<String>() {
        @Override
        public int compare(String q1, String q2) {
            if (q1.charAt(0) == q2.charAt(0)) {
                Pattern pattern = Pattern.compile("[0-9]+");
                Matcher matcher1 = pattern.matcher(q1);
                Matcher matcher2 = pattern.matcher(q2);
                int res = Integer.parseInt(matcher1.find() ? matcher1.group() : "0") - Integer.parseInt(matcher2.find() ? matcher2.group() : "0");
                while (res == 0 && matcher1.find() && matcher2.find()) {
                    res = Integer.parseInt(matcher1.group()) - Integer.parseInt(matcher2.group());
                }
                return res;
            }
            return q1.charAt(0) < q2.charAt(0) ? 1 : -1;
        }
    };
    private static final String TAG = Video.class.getSimpleName();
    public static final String DB_PREFIX = "video";
    private final int id, ownerId, width, height, duration;
    private int views, likes;
    private RepostCounters reposts;
    private final String title, description, accessKey;
    private final boolean canLike, canEdit, canRepost, isPrivate;
    private final Instant timestamp;
    private final MultiSizeImage mPreviewImage;
    private final HashMap<String, String> mLinks;


    private Video(int id, int ownerId, String accessKey, int width, int height, int duration, String title, String description, boolean canLike, boolean canEdit, boolean canRepost, boolean isPrivate, Instant timestamp, MultiSizeImage preview, HashMap<String, String> links) {
        this.id = id;
        this.ownerId = ownerId;
        this.accessKey = accessKey;
        this.width = width;
        this.height = height;
        this.duration = duration;
        this.title = title;
        this.description = description;
        this.canLike = canLike;
        this.canEdit = canEdit;
        this.canRepost = canRepost;
        this.isPrivate = isPrivate;
        this.timestamp = timestamp;
        this.mPreviewImage = preview;
        if (links == null)
            throw new NullPointerException("links can't be null");
        this.mLinks = links;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getDuration() {
        return duration;
    }

    public int getViews() {
        return views;
    }

    public int getLikes() {
        return likes;
    }

    public RepostCounters getReposts() {
        return reposts;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCanLike() {
        return canLike;
    }

    public boolean isCanEdit() {
        return canEdit;
    }

    public boolean isCanRepost() {
        return canRepost;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public MultiSizeImage getPreviewImage() {
        return mPreviewImage;
    }

    public HashMap<String, String> getLinks() {
        return new HashMap<>(mLinks);
    }

    public VideoEntity toEntity() {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            new ObjectOutputStream(os).writeObject(mPreviewImage);

            return new VideoEntity(id,
                    title,
                    description,
                    ownerId,
                    accessKey,
                    mLinks.entrySet().stream().map(entry -> entry.getKey() + AppDatabase.ELEMENT_JOINER + entry.getValue()).collect(java.util.stream.Collectors.joining(AppDatabase.ARRAY_JOINER)),
                    (canLike ? VideoEntity.CAN_LIKE_FLAG : 0) + (canEdit ? VideoEntity.CAN_EDIT_FLAG : 0) + (canRepost ? VideoEntity.CAN_REPOST_FLAG : 0) + (isPrivate ? VideoEntity.IS_PRIVATE_FLAG : 0),
                    width,
                    height,
                    duration,
                    timestamp.toEpochMilli(),
                    os.toByteArray());
        } catch (IOException e) {
            Timber.tag(TAG).e(e);
            throw new RuntimeException(e);
        }
    }

    public static Video getFromJSON(JSONObject obj) {
        try {
            obj = obj.has("video") ? obj.getJSONObject("video") : obj;

            JSONArray preview = obj.has("image") ? obj.getJSONArray("image") : obj.getJSONArray("first_frame");

            PhotoSize[] VKsizes = new PhotoSize[preview.length()];
            int[][] sizes = new int[preview.length()][];
            String[] urls = new String[preview.length()];
            for (int i = 0; i < preview.length(); i++) {
                JSONObject image = preview.getJSONObject(i);
                VKsizes[i] = PhotoSize.getClosest(image.getInt("width"), false, image.has("with_padding") && image.getInt("with_padding") == 1);
                if (i > 0 && VKsizes[i] == VKsizes[i - 1]) {
                    VKsizes[i] = VKsizes[i].getNext(false);
                }
                sizes[i] = new int[]{image.getInt("width"), image.getInt("height")};
                urls[i] = image.getString("url");
            }

            HashMap<String, String> links = new HashMap<>();

            for (Iterator<String> keys = obj.getJSONObject("files").keys(); keys.hasNext(); ) {
                String key = keys.next();
                links.put(key, obj.getJSONObject("files").getString(key));
            }

            return new Video(
                    obj.getInt("id"),
                    obj.getInt("owner_id"),
                    obj.getString("access_key"),
                    obj.getInt("width"),
                    obj.getInt("height"),
                    obj.getInt("duration"),
                    obj.getString("title"),
                    obj.getString("description"),
                    obj.getInt("can_like") == 1,
                    obj.has("can_edit") && obj.getInt("can_edit") == 1,
                    obj.getInt("can_repost") == 1,
                    obj.has("is_private") && obj.getInt("is_private") == 1,
                    Instant.ofEpochSecond(obj.getLong("date")),
                    new MultiSizeImage(VKsizes, sizes, urls, null),
                    links
            );
        } catch (JSONException e) {
            Timber.tag(TAG).e(e, obj.toString());
        }
        return null;
    }

    public static Video getFromEntity(VideoEntity entity) {
        HashMap<String, String> links = new HashMap<>();
        RandomTools.splitNonRegex(entity.getLinks(), AppDatabase.ARRAY_JOINER).forEach(s -> links.put(s.split(AppDatabase.ELEMENT_JOINER)[0], s.split(AppDatabase.ELEMENT_JOINER)[1]));

        MultiSizeImage preview = null;
        try {
            ObjectInputStream is = new ObjectInputStream(new ByteArrayInputStream(entity.getPreview()));
            preview = (MultiSizeImage) is.readObject();
        } catch (ClassNotFoundException | IOException e) {
            Timber.tag(TAG).e(e);
        }


        return new Video(
                entity.getId(),
                entity.getOwnerId(),
                entity.getAccessKey(),
                entity.getWidth(),
                entity.getHeight(),
                entity.getDuration(),
                entity.getTitle(),
                entity.getDescription(),
                (entity.getFlags() & VideoEntity.CAN_LIKE_FLAG) != 0,
                (entity.getFlags() & VideoEntity.CAN_EDIT_FLAG) != 0,
                (entity.getFlags() & VideoEntity.CAN_REPOST_FLAG) != 0,
                (entity.getFlags() & VideoEntity.IS_PRIVATE_FLAG) != 0,
                Instant.ofEpochMilli(entity.getTimestamp()),
                preview,
                links
        );
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
        AppDatabase.getInstance(context).getVideoDAO().addVideo(toEntity());
    }

    @Override
    public String toString() {
        return "Video{" +
                "id=" + id +
                ", ownerId=" + ownerId +
                ", width=" + width +
                ", height=" + height +
                ", duration=" + duration +
                ", views=" + views +
                ", likes=" + likes +
                ", reposts=" + reposts +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", canLike=" + canLike +
                ", canEdit=" + canEdit +
                ", canRepost=" + canRepost +
                ", isPrivate=" + isPrivate +
                ", timestamp=" + timestamp +
                ", mPreviewImage=" + mPreviewImage +
                ", mLinks=" + mLinks +
                '}';
    }
}
