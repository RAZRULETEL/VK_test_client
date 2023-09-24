package com.mastik.vk_test_mod.dataTypes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.widget.ImageView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.mastik.vk_test_mod.MainActivity;
import com.mastik.vk_test_mod.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import timber.log.Timber;

public class VKImage {
    private static final String TAG = VKImage.class.getSimpleName();
    public static final String PREFIX = "img_";
    private static final ConcurrentHashMap<String, Map<Integer, VKImage>> caches = new ConcurrentHashMap<>();
    private static final HashSet<Integer> loadingImages = new HashSet<>();
    private static VKImage defaultImage;
    private final int id;
    private String url;
    private Bitmap img;
    private static final int cornerRadius = 20;
    private final File cacheDir;
    private String prefix = PREFIX;
    private final ArrayList<initListener> listeners = new ArrayList<>();

    private VKImage(Context context) {
        id = -1;
        url = null;
        cacheDir = context.getCacheDir();
        img = BitmapFactory.decodeResource(context.getResources(), R.drawable.camera_50);
    }

    private VKImage(String url_str, int id, String prefix, Context context) {
        this.url = url_str;
        this.id = Math.abs(id);
        cacheDir = context.getCacheDir();
        if (prefix != null)
            this.prefix = prefix;
    }

    public VKImage init(Context context) {
        if (img != null)
            return this;
        MainActivity.BACKGROUND_THREADS.execute(() -> {
            try {
                img = load();
                for (initListener listener : listeners) listener.onInitListener(this);
            } catch (IOException e) {
                if (url == null)
                    throw new IllegalStateException("url cannot be null, id: " + prefix + id);
                if (url.equals("default")) {
                    img = getDefault(context).getImg();
                    for (initListener listener : listeners) listener.onInitListener(VKImage.this);
                    return;
                }
                synchronized (loadingImages) {
                    if (loadingImages.contains(id))
                        return;
                    else
                        loadingImages.add(id);
                    Timber.tag(TAG).d("Trying get from url %s %s, url: %s", prefix, id, url);
                }
                ImageRequest req = new ImageRequest(this.url, new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap response) {
                        img = response;
                        save();
                        for (initListener listener : listeners) listener.onInitListener(VKImage.this);
                        loadingImages.remove(id);
                    }
                }, 0, 0, ImageView.ScaleType.CENTER_INSIDE, Bitmap.Config.ARGB_8888, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        loadingImages.remove(id);
                        img = getDefault(context).getImg();
                        for (initListener listener : listeners) listener.onInitListener(VKImage.this);
                    }
                });
                MainActivity.getVolleyQueue(context).add(req);
            }
        });
        return this;
    }

    /**
     * Allows to overwrite bitmap if it already exists
     *
     * @param context
     * @return
     */
    public VKImage forceInit(Context context) {
        MainActivity.BACKGROUND_THREADS.execute(() -> {
            Timber.tag(TAG).d("Trying get from url %s %s, url: %s", prefix, id, url);
            if (url == null)
                throw new IllegalStateException("url cannot be null");
            ImageRequest req = new ImageRequest(this.url, new Response.Listener<Bitmap>() {
                @Override
                public void onResponse(Bitmap response) {
                    img = response;
                    save();
                    for (initListener listener : listeners) {
                        listener.onInitListener(VKImage.this);
                    }
                }
            }, 0, 0, Bitmap.Config.ARGB_8888, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    img = getDefault(context).getImg();
                    for (initListener listener : listeners) {
                        listener.onInitListener(VKImage.this);
                    }
                }
            });
            MainActivity.getVolleyQueue(context).add(req);
        });
        return this;
    }

    public Bitmap getImg() {
        return img;
    }

    public Bitmap load() throws IOException {
        Bitmap bitmap;
        File f = new File(cacheDir, prefix + id + ".png");
        FileInputStream fin = new FileInputStream(f);
        bitmap = BitmapFactory.decodeStream(fin);
        fin.close();
        return bitmap;
    }

    public void save() {
        if (img == null) {
            Timber.tag(TAG).e("Cannot save %s%s, bitmap is null", prefix, id);
            return;
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        img.compress(Bitmap.CompressFormat.PNG, 100, bytes);
        File f = new File(cacheDir, prefix + id + ".png");
        try (FileOutputStream fo = new FileOutputStream(f)) {
            fo.write(bytes.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Bitmap getRoundedCorner() {
        return getRoundedCorner(cornerRadius);
    }

    public Bitmap getRoundedCorner(int pixels) {
        if (img == null)
            return getDefault().getRoundedCorner(pixels);
        Bitmap output = Bitmap.createBitmap(img.getWidth(), img
                .getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, img.getWidth(), img.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, (float) pixels, (float) pixels, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(img, rect, rect, paint);

        return output;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public String getSavePath() {
        return prefix + id + ".png";
    }

    public enum ImageOwner {
        USER("users.get?fields=photo&user_ids=", "response/[0]/smth"),
        GROUP("groups.getById?group_id=", "response/[0]/smth");
        public final String signature, path;

        ImageOwner(String signature, String path) {
            this.signature = signature;
            this.path = path;
        }
    }

    public VKImage clearInitListeners() {
        listeners.clear();
        return this;
    }

    public void addOnInitListener(initListener listener) {
        if (img != null)
            listener.onInitListener(this);
        this.listeners.add(listener);
    }

    public interface initListener {
        void onInitListener(VKImage image);
    }

    public static VKImage get(String url, int id, Context context) {
        return get(url, id, PREFIX, context);
    }

    public static VKImage get(String url, int id, String prefix, Context context) {
        if (caches.get(prefix) == null)
            caches.put(prefix, new ConcurrentHashMap<>());
        if (id > 0) {
            if (caches.get(prefix).get(id) == null)
                caches.get(prefix).put(id, new VKImage(url, id, prefix, context));
        } else {
            for (VKImage img : caches.get(prefix).values())
                if (Objects.equals(img.getUrl(), url))
                    return img;
            return new VKImage(url, id, prefix, context);
        }
        caches.get(prefix).get(id).setUrl(url);
        return caches.get(prefix).get(id);
    }

    public static VKImage getDefault(Context context) {
        if (defaultImage == null)
            defaultImage = new VKImage(context);
        return defaultImage;
    }

    public static VKImage getDefault() {
        return defaultImage;
    }

    public static int getCornerRadius() {
        return cornerRadius;
    }


}
