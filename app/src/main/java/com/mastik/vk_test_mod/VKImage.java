package com.mastik.vk_test_mod;

import static com.mastik.vk_test_mod.MainActivity.token;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.widget.ImageView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class VKImage {
    private final String id;
    private final String url;
    private Bitmap img;
    private static int cornerRadius = 20;
    private Context context;
    private String prefix = "img_";

    public VKImage(Context context){
        id = null;
        url = null;
        this.context = context;
        img = BitmapFactory.decodeResource(context.getResources(), R.drawable.camera_50);
    }
    public VKImage(String url_str, String id, Context context){
        this(url_str, id, null, context);
    }
    public VKImage(String url_str, String id, String prefix, Context context){
        this.url = url_str;
        this.id = id != null ? id.replace("-",""):null;
        this.context = context;
        if(prefix != null)
            this.prefix = prefix;
    }
    public VKImage init() throws IOException {
        try {
            img = load();
        }catch (IOException e) {
            //Log.i("src",url_str);
            Log.e("VKImage", "Trying get from url");
            java.net.URL url = new URL(this.url);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            Log.i("Bitmap","returned");
            img = myBitmap;
            save();
        }
        return this;
    }
    public Bitmap getImg() {
        return img;
    }

    public Bitmap load() throws IOException{
            Bitmap bitmap;
            File f = new File(context.getCacheDir(),prefix + id + ".png");
            //Log.e("VKImage", "Trying to load "+f.getPath());
            FileInputStream fin = new FileInputStream(f);
            bitmap = BitmapFactory.decodeStream(fin);
            fin.close();
            //Log.e("VKImage", "Loaded "+f.getPath());
            return bitmap;
    }
    public void save(){
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        img.compress(Bitmap.CompressFormat.PNG, 100, bytes);
        File f = new File(context.getCacheDir(),prefix + id +".png");
        Log.e("Saving",f.getPath());
        try(FileOutputStream fo = new FileOutputStream(f);)
        {
            fo.write(bytes.toByteArray());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    };
    public static int getCornerRadius() {
        return cornerRadius;
    }
    public Bitmap getRoundedCorner(){
        return getRoundedCorner(cornerRadius);
    }
    public Bitmap getRoundedCorner(int pixels){
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
    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int pixels){
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap
                .getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, (float) pixels, (float) pixels, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }
    public static void asyncSetImage(ImageOwner imgInfo, String id, ImageView imgView, Context context){

        id = id.replace("-","");

        String url ="https://api.vk.com/method/"+imgInfo.signature+id+"&access_token="+token+"&v=5.131";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
//                        if(method_and_params.startsWith("users")) {
//                            try {
//                                writeData(new JSONObject(response).getJSONArray("response").getJSONObject(0).toString(), "user_"+new JSONObject(response).getJSONArray("response").getJSONObject(0).get("id").toString());
//                            } catch (JSONException e) {
//                                e.printStackTrace();
//                            }
//                        }
                        String rsp = response;
                        String[] path_smpl = imgInfo.path.split("/");
                        try {
                            for(int i = 0;i<path_smpl.length-1;i++){
                                if(path_smpl[i+1].charAt(0) != '['){
                                    if(path_smpl[i].charAt(0) == '['){
                                        //Get element from array
                                        rsp = new JSONArray(rsp).getJSONObject(Integer.parseInt(path_smpl[i].replaceAll("[^0-9]",""))).toString();

                                    }else{
                                        //Get object from object
                                        rsp = new JSONObject(rsp).getJSONObject(path_smpl[i]).toString();

                                    }
                                }else{
                                    //Get array from object
                                    rsp = new JSONObject(rsp).getJSONArray(path_smpl[i]).toString();

                                }

                            }

                            Bitmap logo_img = null;
                            String url = "";
                            if(new JSONObject(rsp).has("photo_50"))
                                url = new JSONObject(rsp).get("photo_50").toString();
                            else
                                url = new JSONObject(rsp).get("photo").toString();
                            try {
                                logo_img = new VKImage(url, new JSONObject(rsp).get("id").toString(), context).init().getImg();
                                imgView.setImageBitmap(logo_img);
                            }catch (IOException e){
                                e.printStackTrace();
                            }


                        } catch (JSONException e) {
                            e.printStackTrace();
                            System.out.println(rsp);
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("Async", "load img error");
            }
        });

// Add the request to the RequestQueue.
        msg.getQueue().add(stringRequest);

    }
    enum ImageOwner{
        USER("users.get?fields=photo&user_ids=", "response/[0]/smth"),
        GROUP("groups.getById?group_id=", "response/[0]/smth");
        private final String signature, path;
        ImageOwner(String signature, String path) {
            this.signature = signature;
            this.path = path;
        }
    }
    public static void asyncInitImage(VKImage img, ImageView view){

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    view.setImageBitmap(img.init().getImg());
                } catch (IOException e) {
                    view.setImageBitmap(new VKImage(view.getContext()).getImg());
                }
            }
        }).start();
    }
}
