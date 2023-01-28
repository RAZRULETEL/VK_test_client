package com.mastik.vk_test_mod;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.widget.LinearLayout.HORIZONTAL;

import static com.mastik.vk_test_mod.LongPollService.getLongPollServer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.res.ResourcesCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    //String tag = "test";
    public static String token ;//= getResources().getString(R.string.token);
    static int unreadMessageColor = (200 & 0xff) << 24 | (0) << 16 | (0xff) << 8 | (0xff);
    LinearLayout scroll, menu;
    GradientDrawable corners;
    int cornerRadius = 20;
    static boolean runned = false;
    static NotificationManager main_notife;
    static String CHANNEL_ID = Integer.toString(View.generateViewId());
    static String server, key;
    static HashMap<String, String> actions = new HashMap<>(), file_type = new HashMap<>();
    static File CacheDir;
    static int owner_id = 216663221;
    private int time_offset = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
// eg.
        prefs.edit().putString("foo","bar").apply();
        prefs.getString("foo", null);

        //NotificationManager mNotificationManager = (NotificationManager) getSystemService( NOTIFICATION_SERVICE ) ;
        //for(int i = 0; i < mNotificationManager.getNotificationChannels().toArray().length; i++)
        //    mNotificationManager.deleteNotificationChannel(mNotificationManager.getNotificationChannels().get(i).getId());

        CacheDir = getCacheDir();
        MovingRgbGradient.init(getApplicationContext());

        //createNotificationChannel("Новое сообщение");

        actions.put("chat_kick_user","выпилился из беседы.");
        actions.put("chat_pin_message", "прикрепил сообщение.");
        actions.put("chat_invite_user", "присоединился к gay party.");
        actions.put("chat_unpin_message", "открепил сообщение.");
        actions.put("chat_photo_update", "обновил картинку.");
        actions.put("chat_photo_remove", "удолил картинку.");
        actions.put("chat_title_update", "изменил название gay бара на ");
        System.out.println(actions.toString());

        file_type.put("file", "Файл");
        file_type.put("photo", "Фотография");
        file_type.put("video", "Видео");
        file_type.put("audio", "Аудио");
        file_type.put("doc", "Документ");
        file_type.put("call", "Звонок");
        file_type.put("link", "Ссылка");
        file_type.put("sticker", "Стикер");


        createNotificationChannel("Статус", "status");
        createNotificationChannel("Сообщения", "message");

        main_notife = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        //main_back = findViewById(R.id.main_layout);

        corners = new GradientDrawable();
        corners.setCornerRadius(cornerRadius);
        corners.setColor(unreadMessageColor);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        token = getResources().getString(R.string.token);
        scroll = findViewById(R.id.scroll);

        menu = findViewById(R.id.main_menu);
        DisplayMetrics displayMetrics = getApplicationContext().getResources().getDisplayMetrics();
        menu.setTranslationX(-displayMetrics.widthPixels);

       /* String uri = "https://oauth.vk.com/authorize?client_id=7681504&display=mobile&redirect_uri=https://oauth.vk.com/blank.html&scope=friends&response_type=token&v=5.131&state=123456";
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(uri));
        startActivity(i);*/

        RequestQueue queue = Volley.newRequestQueue(this);
       /* RequestQueue queue = Volley.newRequestQueue(this);
        String url ="https://api.vk.com/method/users.get?user_ids=&access_token="+token+"&v=5.131";


// Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        api_response = response;
                        Toast.makeText(getApplicationContext(),api_response,Toast.LENGTH_LONG).show();
                        return api_response;

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(),"Error",Toast.LENGTH_SHORT).show();
            }
        });

// Add the request to the RequestQueue.
        queue.add(stringRequest);*/
// ...


// Instantiate the RequestQueue.
        //RequestQueue queue = Volley.newRequestQueue(this);
        String url ="https://api.vk.com/method/messages.getConversations?access_token="+token+"&count=15&extended=1&v=5.131";//

// Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        Log.i("Инфoрмaция: ", "Пoтoк нaчaл рaбoту.");

                        buildDialogs(response);

                        Log.i("Инфoрмaция: ", "Пoтoк зaкoнчил рaбoту.");

                        writeData(response, "last");
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                TextView textView = new TextView(getApplicationContext());
                textView.setText("That didn't work!");
                textView.setBackgroundColor(Color.RED);
                textView.setTextSize(20);
                textView.setGravity(Gravity.CENTER);
                textView.setTextColor(Color.BLACK);
                scroll.addView(textView);
                buildDialogs(readSourceData("last"));
            }
        });

        queue.add(stringRequest);

        FrameLayout menu_button = findViewById(R.id.menu);
        menu_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("click", "menu");
                menu.animate().translationX(0).setDuration(200).start();
            }
        });
        Button hide = findViewById(R.id.hide_menu);
        hide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menu.animate().translationX(-displayMetrics.widthPixels).setDuration(200).start();
            }
        });
        Button settings = findViewById(R.id.setts);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Settings.class);
                startActivity(intent);
            }
        });
        Button news = findViewById(R.id.news);
        news.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Settings.class);
                startActivity(intent);
            }
        });
    }
    private void buildDialogs(String response){
        try {
            JSONObject jsonObject = new JSONObject(response).getJSONObject("response");
            JSONArray answerArray = jsonObject.getJSONArray("items");
            TextView unread_count = findViewById(R.id.unread_count);
            unread_count.setText(jsonObject.get("unread_count").toString());
            LongPollService.unread_msg_count = Integer.parseInt(jsonObject.get("unread_count").toString());
            unread_count.setVisibility(View.VISIBLE);
            JSONArray profiles = null;
            if(jsonObject.has("profiles")){
                profiles = jsonObject.getJSONArray("profiles");
                for(int i = 0; i < profiles.length(); i++)
                    writeData(profiles.getJSONObject(i).toString(), "user_"+profiles.getJSONObject(i).get("id"));
            }
            JSONArray groups = null;
            if(jsonObject.has("groups"))
                groups = jsonObject.getJSONArray("groups");
            //textView.setText(null);
            for(int l = 0; l < answerArray.length();l++){
                long now = System.currentTimeMillis();
                Log.i("Инфoрмaция: ", "Пoтoк "+l+" зaкoнчил рaбoту.");
                JSONObject conversation = answerArray.getJSONObject(l);
                JSONObject last_message = conversation.getJSONObject("last_message");

                String title = "";
                String last_msg;
                Long time = Long.parseLong(last_message.get("date").toString());
                ImageView logo_last = new ImageView(getApplicationContext());
                int unread = unreadMsgCount(conversation.toString());
                LinearLayout main = new LinearLayout(getApplicationContext());

                last_msg = last_message.get("text").toString();
                if(last_message.get("text").toString().equals(""))
                    if(last_message.getJSONArray("attachments").length() > 0)
                        last_msg = file_type.get(last_message.getJSONArray("attachments").getJSONObject(0).get("type").toString());
                    else
                    if(last_message.has("action")){
                        int i;
                        for(i = 0; i < profiles.length(); i++)
                            if(profiles.getJSONObject(i).get("id").toString().equals(last_message.getJSONObject("action").get("member_id").toString()))
                                break;
                        last_msg = profiles.getJSONObject(i).get("first_name")+" "+profiles.getJSONObject(i).get("last_name")+" ";
                        last_msg += actions.get(last_message.getJSONObject("action").get("type").toString());
                    }

                String id = conversation.getJSONObject("conversation").getJSONObject("peer").get("id").toString();
                ImageView logo = new ImageView(getApplicationContext());
                Bitmap logo_bit = readBitmap(id, getApplicationContext());
                if(Integer.parseInt(last_message.get("random_id").toString()) != 0)
                    if(!conversation.getJSONObject("conversation").get("in_read").toString().equals(conversation.getJSONObject("conversation").get("out_read").toString()))
                        unread = Integer.parseInt(conversation.getJSONObject("conversation").get("out_read").toString()) - Integer.parseInt(conversation.getJSONObject("conversation").get("in_read").toString());
                switch(conversation.getJSONObject("conversation").getJSONObject("peer").get("type").toString()){
                    case "chat":
                        title = conversation.getJSONObject("conversation").getJSONObject("chat_settings").get("title").toString();
                        if(logo_bit == null){
                            if(conversation.getJSONObject("conversation").getJSONObject("chat_settings").has("photo")){
                                AsyncSetBitmapFromUrl(conversation.getJSONObject("conversation").getJSONObject("chat_settings").getJSONObject("photo").get("photo_50").toString(), logo, id);
                            }else
                                AsyncSetBitmapFromUrl("https://vk.com/images/camera_50.png", logo, id);
                        }else
                            logo.setImageBitmap(getRoundedCornerBitmap(logo_bit, cornerRadius));
                    break;
                    case "user":
                        int i;
                        for(i = 0; i < profiles.length(); i++){
                            if(profiles.getJSONObject(i).get("id").toString().equals(id))
                                break;
                        }
                        title = profiles.getJSONObject(i).get("first_name") +" "+ profiles.getJSONObject(i).get("last_name");
                        if(logo_bit == null)
                            AsyncSetBitmapFromUrl(profiles.getJSONObject(i).get("photo_50").toString(), logo, id);
                        else
                            logo.setImageBitmap(getRoundedCornerBitmap(logo_bit, cornerRadius));
                    break;
                    case "group":
                        //int i;
                        for(i = 0; i < groups.length(); i++){
                            if(groups.getJSONObject(i).get("id").toString().equals(id.substring(1)))
                                break;
                        }
                        title = groups.getJSONObject(i).get("name").toString();
                        if(logo_bit == null)
                            AsyncSetBitmapFromUrl(groups.getJSONObject(i).get("photo_50").toString(), logo, id);
                        else
                            logo.setImageBitmap(getRoundedCornerBitmap(logo_bit, cornerRadius));
                    break;
                }//dynamicTextView.append(" "+conversation.getJSONObject("conversation").getJSONObject("peer").get("type").toString());


                if(!last_message.has("action")){
                    Bitmap logo_last_bit = readBitmap(last_message.get("from_id").toString(), getApplicationContext());
                    if(logo_last_bit == null){
                        if(!last_message.get("from_id").toString().contains("-")) {
                            int i;
                            for (i = 0; i < profiles.length(); i++)
                                if (profiles.getJSONObject(i).get("id").equals(last_message.get("from_id")))
                                    break;
                            AsyncSetBitmapFromUrl(profiles.getJSONObject(i).get("photo_50").toString(), logo_last, last_message.get("from_id").toString());
                        }else {
                            int i;
                            for (i = 0; i < groups.length(); i++)
                                if (groups.getJSONObject(i).get("id").toString().equals(last_message.get("from_id").toString().replace("-", "")))
                                    break;
                            AsyncSetBitmapFromUrl(groups.getJSONObject(i).get("photo_50").toString(), logo_last, last_message.get("from_id").toString());
                        }

                    }else
                        logo_last.setImageBitmap(getRoundedCornerBitmap(logo_last_bit, cornerRadius));
                }


                main.setId(Integer.parseInt(id));
                main.setTag(unread);
                switch(conversation.getJSONObject("conversation").getJSONObject("peer").get("type").toString()){
                    case "chat":
                        main.setTag(unread+"split"+ conversation.getJSONObject("conversation").getJSONObject("chat_settings"));
                        break;
                    case "user":
                        for(int p = 0; p < profiles.length();p++){
                            if(profiles.getJSONObject(p).get("id").toString().equals(id)){
                                main.setTag(unread+"split"+profiles.getJSONObject(p).toString());
                                break;
                            }
                        }
                        break;

                }
                addDialog(main, title, last_msg, time, logo, logo_last, unread);
                LinearLayout hr_cont = new LinearLayout(getApplicationContext());
                TextView hr = new TextView(getApplicationContext());
                LinearLayout.LayoutParams line = new LinearLayout.LayoutParams((int) (scroll.getWidth()*0.6),5);
                line.gravity = Gravity.CENTER;
                hr.setLayoutParams(line);
                hr.setBackgroundColor(Color.parseColor("#E6E6E6"));
                hr_cont.setOrientation(LinearLayout.VERTICAL);
                hr_cont.setBackgroundColor(getResources().getColor(R.color.transparent, null));
                hr_cont.addView(hr);
                scroll.addView(hr_cont);


                Log.i("Time", " "+(System.currentTimeMillis() - now));

            }
            //textView.append(String.valueOf(answerArray.length()));
            //textView.setText(jsonObject.getJSONObject("response").getJSONArray("items").getJSONObject(0).getJSONObject("conversation").getJSONObject("peer").get("id").toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if(!LongPollService.isActive())
            getLongPollServer(getApplicationContext(), token, scroll);
    }
    public void AsyncSetValue(String method_and_params, TextView text, String path, ImageView imgView){
        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="https://api.vk.com/method/"+method_and_params+"&access_token="+token+"&v=5.131";
        //String url = "https://api.vk.com/method/users.get?user_ids=615382099&access_token=a6f8602fb18031c31d0edc642be06b8e9342b25802cf4a99820f318b9f010d9846d1057953fa531c0c151&v=5.131bfygh";


// Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        String rsp = response;
                        String[] path_smpl = path.split("/");
                        try {
                            for(int i = 0;i<path_smpl.length-1;i++){
                                //Toast.makeText(getApplicationContext(),rsp+"   "+path_smpl[i].charAt(0),Toast.LENGTH_SHORT).show();

                                if(path_smpl[i+1].charAt(0) != '['){
                                    if(path_smpl[i].charAt(0) == '['){
                                        //Toast.makeText(getApplicationContext(),"0 "+path_smpl[i].replaceAll("[^0-9]",""),Toast.LENGTH_SHORT).show();
                                        rsp = new JSONArray(rsp).getJSONObject(Integer.parseInt(path_smpl[i].replaceAll("[^0-9]",""))).toString();

                                    }else{
                                        //Toast.makeText(getApplicationContext(),"obj",Toast.LENGTH_SHORT).show();
                                        rsp = new JSONObject(rsp).getJSONObject(path_smpl[i]).toString();

                                    }
                                }else{
                                    //Toast.makeText(getApplicationContext(),"arr",Toast.LENGTH_SHORT).show();
                                    rsp = new JSONObject(rsp).getJSONArray(path_smpl[i]).toString();
                                }

                            }
                            Bitmap logo_img = null;
                            try {
                            if(new JSONObject(rsp).has("photo_50"))
                                logo_img = getBitmapFromURL(new JSONObject(rsp).get("photo_50").toString());
                            else
                                logo_img =getBitmapFromURL(new JSONObject(rsp).get("photo").toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            if(logo_img != null)
                            if(method_and_params.contains("groups"))
                                writeData(logo_img, "-"+ new JSONObject(rsp).get("id"));
                            else
                                writeData(logo_img, new JSONObject(rsp).get("id").toString());
                            imgView.setImageBitmap(logo_img);
                            if(!path_smpl[path_smpl.length-1].contains("+"))
                                rsp = new JSONObject(rsp).get(path_smpl[path_smpl.length-1]).toString();
                            else{
                                String buff="";
                                for(int i = 0; i < path_smpl[path_smpl.length-1].split("\\+").length;i++){
                                    //Toast.makeText(getApplicationContext(),path_smpl[path_smpl.length-1].split("\\+")[i],Toast.LENGTH_SHORT).show();
                                    buff += new JSONObject(rsp).get(path_smpl[path_smpl.length-1].split("\\+")[i]) +" ";
                                }
                                rsp = buff;
                            }
                            //api_response = response;
                            //Toast.makeText(getApplicationContext(), api_response, Toast.LENGTH_LONG).show();
                            //return api_response;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        text.setText(rsp);

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), "Error AsyncSetValue+img", Toast.LENGTH_SHORT).show();
            }
        });

// Add the request to the RequestQueue.
        queue.add(stringRequest);


    }
    public void AsyncSetBitmapFromUrl(String url, ImageView imageView, String id){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(time_offset++ * 10);
                    Bitmap logo_img = readBitmap(id, getApplicationContext());
                    while (logo_img == null) {
                        logo_img = getBitmapFromURL(url);
                        Thread.sleep(10);
                    }
                    writeData(logo_img, id);
                    Bitmap finalLogo_img = logo_img;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            imageView.setImageBitmap(getRoundedCornerBitmap(finalLogo_img, cornerRadius));
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    public void AsyncSetBitmapFromUrl(String url, ImageView imageView){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    imageView.setImageBitmap(getRoundedCornerBitmap(getBitmapFromURL(url), cornerRadius));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    public static Bitmap getBitmapFromURL(String src) {
        try {
            Log.e("src",src);
            java.net.URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            Log.e("Bitmap","returned");
            return myBitmap;
        } catch (IOException e) {
            e.printStackTrace();
            //Log.e("Exception",e.getMessage());
            return null;
        }
    }
    public int convertDpToPixel(float dp){
        return Math.round(dp * ((float) getApplicationContext().getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }
    public static int convertDpToPixel(float dp, Context context){
        return Math.round(dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }
    private void writeData(Bitmap img, String name){
        try
        {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            img.compress(Bitmap.CompressFormat.PNG, 100, bytes);
            File f = new File(getCacheDir(),name +".png");
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());
            fo.close();
        }
        catch (IOException e)
        {
            Toast.makeText(getApplicationContext(),"Error "+name,Toast.LENGTH_SHORT).show();
            e.printStackTrace();

        }
    }
    private void writeData(String info, String name){
        System.out.println(name);
        try
        {
            File f = new File(getCacheDir(),name +".txt");
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(info.getBytes());
            fo.close();
        }
        catch (IOException e)
        {
            Toast.makeText(getApplicationContext(),"Error txt: "+name,Toast.LENGTH_SHORT).show();
            e.printStackTrace();

        }
    }
    public static Bitmap readBitmap(String name, Context context) {
        Bitmap bitmap;
        try
        {
            File f = new File(context.getCacheDir(),name + ".png");
            FileInputStream fin = new FileInputStream(f);
            bitmap = BitmapFactory.decodeStream(fin);
            fin.close();
            return bitmap;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }
    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int pixels) {
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
    private int unreadMsgCount(String rsp) throws JSONException {
        return  Integer.parseInt(new JSONObject(rsp).getJSONObject("conversation").get("out_read_cmid").toString()) - Integer.parseInt(new JSONObject(rsp).getJSONObject("conversation").get("in_read_cmid").toString());
    }
    private static void longPollServer(int ts, Context context, String activity_name, View scroll){
        System.out.println("Threads: "+Thread.activeCount());

        int TIMEOUT_MS=30000;

        RequestQueue queue = Volley.newRequestQueue(context);

        String poll = "https://"+server+"?act=a_check&key="+key+"&ts="+ts+"&wait=25&mode=2&version=2";

        StringRequest longPoll = new StringRequest(Request.Method.GET, poll,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        try {
                            if(scroll != null)
                                System.out.println(new JSONObject(response).getJSONArray("updates"));
                            else
                                System.out.println("Background: "+ new JSONObject(response).getJSONArray("updates"));
                            //Toast.makeText(context, new JSONObject(response).getJSONArray("updates").toString(),Toast.LENGTH_SHORT).show();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        try {
                            JSONArray updates = new JSONObject(response).getJSONArray("updates");

                            for(int i = 0; i < updates.length(); i++) {
                                if(updates.getJSONArray(i).get(0).toString().equals("4")) {
                                    int peer_id = Integer.parseInt(updates.getJSONArray(i).get(3).toString());
                                    String from_id = "";
                                    if(updates.getJSONArray(i).length() >= 7)
                                        from_id = updates.getJSONArray(i).getJSONObject(6).get("from").toString();
                                    if((Integer.parseInt(updates.getJSONArray(i).get(2).toString()) & 2) == 0){
                                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                                        int id_notif = new Random().nextInt(1000000);//Integer.parseInt(null)
                                        notificationManager.cancel(String.valueOf(peer_id), id_notif);
                                    }
                                    if(activity_name.split("\\.")[activity_name.split("\\.").length - 1].equals("MainActivity")) {
                                        System.out.println(peer_id);
                                        for (int index = 0; index < ((ViewGroup) scroll).getChildCount(); index++) {
                                            View nextChild = ((ViewGroup) scroll).getChildAt(index);
                                            if (nextChild.getId() == peer_id) {
                                                View last_msg_cont = ((ViewGroup) ((ViewGroup) nextChild).getChildAt(1)).getChildAt(1);
                                                ImageView last_msg_logo = (ImageView) ((ViewGroup) last_msg_cont).getChildAt(0);
                                                TextView last_msg = (TextView) ((ViewGroup) last_msg_cont).getChildAt(1);
                                                last_msg.setText(updates.getJSONArray(i).get(5).toString());
                                                if (peer_id < 2000000000){
                                                    if ((Integer.parseInt(updates.getJSONArray(i).get(2).toString()) & 2) == 0)
                                                        last_msg_logo.setImageBitmap(readBitmap(String.valueOf(peer_id), context));
                                                }else
                                                    last_msg_logo.setImageBitmap(readBitmap(from_id, context));
                                                if ((Integer.parseInt(updates.getJSONArray(i).get(2).toString()) & 2) != 0)
                                                    last_msg_logo.setImageBitmap(readBitmap("216663221", context));
                                            }
                                        }
                                    }else
                                        if(activity_name.split("\\.")[activity_name.split("\\.").length - 1].equals("msg")){
                                            LinearLayout scroll_l = (LinearLayout) scroll;
                                            if(scroll.getId() == peer_id){
                                                LinearLayout message = new LinearLayout(context);
                                                message.setOrientation(LinearLayout.HORIZONTAL);

                                                message.setBackgroundColor(unreadMessageColor);

                                                ConstraintLayout msg_layout = new ConstraintLayout(context);
                                                ConstraintLayout.LayoutParams title_params = new ConstraintLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
                                                msg_layout.setLayoutParams(title_params);
                                                msg_layout.setId(View.generateViewId());

                                                ImageView logo = new ImageView(context);
                                                Bitmap logo_bit = null;
                                                int scroll_id = scroll.getId();
                                                if((Integer.parseInt(updates.getJSONArray(i).get(2).toString()) & 2) != 0)
                                                    logo_bit = readBitmap("216663221", context);
                                                else
                                                    if(scroll_id > 0 && !Integer.toString(scroll.getId()).contains("2000000")){
                                                        logo_bit = readBitmap(String.valueOf(peer_id), context);
                                                    }

                                                /*if(Integer.parseInt(last_msgs.getJSONObject(i).get("from_id").toString()) < 0)
                                                    if(logo_bit == null){
                                                        AsyncSetImage("groups.getById?group_id="+last_msgs.getJSONObject(i).get("from_id").toString().substring(1),"response/[0]/smth",logo);
                                                    }else
                                                        logo.setImageBitmap(getRoundedCornerBitmap(logo_bit, 20));
                                                else
                                                if(logo_bit == null){
                                                    AsyncSetImage("users.get?user_ids="+last_msgs.getJSONObject(i).get("from_id").toString()+"&fields=photo","response/[0]/smth",logo);
                                                }else
                                                    logo.setImageBitmap(getRoundedCornerBitmap(logo_bit, 20));*/

                                                logo.setImageBitmap(logo_bit);

                                                TextView newMsg = new TextView(context);
                                                int newMsg_id = View.generateViewId();
                                                newMsg.setId(newMsg_id);
                                                newMsg.setTextSize(15);
                                                newMsg.setText(updates.getJSONArray(i).get(5).toString());
                                                ViewGroup.LayoutParams txt = new ViewGroup.LayoutParams(0, WRAP_CONTENT);
                                                newMsg.setLayoutParams(txt);

                                                TextView time = new TextView(context);
                                                int time_id = View.generateViewId();
                                                time.setId(time_id);
                                                SimpleDateFormat formatter = new SimpleDateFormat("HH:mm", Locale.ROOT);
                                                String dateString = formatter.format(new Date(Long.parseLong(updates.getJSONArray(i).get(4).toString())*1000));
                                                time.setTextSize(15);
                                                time.setText(dateString);

                                                msg_layout.addView(newMsg);
                                                msg_layout.addView(time);

                                                message.addView(logo);
                                                message.addView(msg_layout);
                                                message.setPadding(convertDpToPixel(5, context),convertDpToPixel(6, context),convertDpToPixel(13, context),convertDpToPixel(6, context));
                                                LinearLayout.LayoutParams msg_params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
                                                msg_params.setMargins(0, convertDpToPixel(5, context), 0, convertDpToPixel(5, context));
                                                message.setLayoutParams(msg_params);



                                                GradientDrawable msg_style = new GradientDrawable();
                                                msg_style.setCornerRadius(10);
                                                msg_style.setColor(Color.parseColor("#ffffff"));//#FFD6D6D6
                                                newMsg.setBackground(msg_style);
                                                newMsg.setPadding(convertDpToPixel(3, context),convertDpToPixel(3, context),convertDpToPixel(3, context),convertDpToPixel(3, context));

                                                scroll_l.addView(message, 0);

                                                message.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
                                                logo.getLayoutParams().width = convertDpToPixel(35, context);
                                                logo.getLayoutParams().height = convertDpToPixel(35, context);
                                                LinearLayout.LayoutParams logo_params = (LinearLayout.LayoutParams) logo.getLayoutParams();
                                                logo_params.gravity = Gravity.BOTTOM;
                                                logo.setLayoutParams(logo_params);



                                                ConstraintSet c = new ConstraintSet();
                                                c.clone(msg_layout);
                                                c.connect(newMsg_id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT);
                                                //c.connect(newMsg_id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
                                                c.connect(newMsg_id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
                                                c.connect(newMsg_id, ConstraintSet.RIGHT, time_id, ConstraintSet.LEFT);
                                                //c.connect(time_id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
                                                c.connect(time_id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
                                                c.connect(time_id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT);
                                                c.setHorizontalBias(newMsg_id, 0);
                                                c.setMargin(newMsg_id, ConstraintSet.LEFT, convertDpToPixel(10, context));
                                                c.setMargin(newMsg_id, ConstraintSet.RIGHT, convertDpToPixel(10, context));
                                                msg_layout.setConstraintSet(c);
                                            }
                                        }else
                                            if(scroll == null){// && activity_name.split("\\.")[activity_name.split("\\.").length - 1].equals("TestWorker")
                                                if((Integer.parseInt(updates.getJSONArray(i).get(2).toString()) & 2) == 0)
                                                    AsyncNotificate(context, updates.getJSONArray(i).get(1).toString(), updates.getJSONArray(i).get(3).toString());
                                            }
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        new Thread(() -> {
                            try {
                                JSONObject rsp = new JSONObject(response);
                                System.out.println(rsp);

                                longPollServer(Integer.parseInt(rsp.get("ts").toString()), context, activity_name, scroll);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(context, "Error longPollServer "+error, Toast.LENGTH_SHORT).show();
                System.out.println(error.getNetworkTimeMs()+" "+error);
                if(error.toString().contains("com.android.volley.NoConnectionError")){
                    try{
                        Thread.sleep(1000);
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        });

// Add the request to the RequestQueue.
 longPoll.setRetryPolicy(new DefaultRetryPolicy(
                TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(longPoll);

    }
    private void createNotificationChannel(String name, String id){
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if(notificationManager.getNotificationChannel(name) == null){
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(id, name, importance);
            //String description = getString(R.string.channel_description);
            //channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(channel);
        }
    }
    @Override
    protected void onStop () {
        super.onStop();
        System.out.println("Shalom");
        if(!runned)
            startForegroundService( new Intent( this, NotificationService.class )) ;
        runned = true;
    }
    private static void AsyncNotificate(Context context, String msg_id, String peer_id){
        RequestQueue queue = Volley.newRequestQueue(context);

        String url ="https://api.vk.com/method/messages.getById?access_token="+token+"&extended=1&message_ids="+msg_id+"&v=5.131";//

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try{
                            JSONObject rsp = new JSONObject(response).getJSONObject("response");
                            
                            String title = null;
                            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID);

                            builder .setSmallIcon(R.drawable.ricardo)
                                    .setContentText(rsp.getJSONArray("items").getJSONObject(0).get("text").toString())
                                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                                    .setLargeIcon(readBitmap(rsp.getJSONArray("items").getJSONObject(0).get("from_id").toString(), context))
                                    .setCategory(Notification.CATEGORY_MESSAGE)
                                    .setGroup(peer_id)
                                    .setStyle(new NotificationCompat.BigTextStyle()
                                            .bigText(rsp.getJSONArray("items").getJSONObject(0).get("text").toString()));

                            if(Integer.parseInt(peer_id) > 0){
                                if(Integer.parseInt(peer_id) > 2000000000){
                                    syncGroup(context, peer_id, builder);
                                    return;
                                }else{
                                    for(int i = 0; i < rsp.getJSONArray("profiles").length(); i++){
                                        if(rsp.getJSONArray("profiles").getJSONObject(i).get("id").toString().equals(rsp.getJSONArray("items").getJSONObject(0).get("from_id").toString())){
                                            title = rsp.getJSONArray("profiles").getJSONObject(i).get("first_name") +" "+ rsp.getJSONArray("profiles").getJSONObject(0).get("last_name");
                                            builder.setContentTitle(title);
                                            break;
                                        }
                                    }
                                }
                            }else{
                                for(int i = 0; i < rsp.getJSONArray("groups").length(); i++){
                                    if(rsp.getJSONArray("groups").getJSONObject(i).get("id").toString().equals(peer_id.substring(1))){
                                        title = rsp.getJSONArray("groups").getJSONObject(i).get("name").toString();
                                        builder.setContentTitle(title);
                                        break;
                                    }
                                }
                            }

                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

                            // notificationId is a unique int for each notification that you must define

                                builder.setContentTitle(title);
                                int notificationId = View.generateViewId();
                                notificationManager.notify(notificationId, builder.build());

                        } catch (JSONException | InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        });

// Add the request to the RequestQueue.
        //Toast.makeText(getApplicationContext(),"Started... queue", Toast.LENGTH_SHORT).show();
        queue.add(stringRequest);


    }
    private static void syncGroup(Context context, String chat_id, NotificationCompat.Builder builder) throws InterruptedException {
        final String[] answer = {null};
        RequestQueue queue = Volley.newRequestQueue(context);

        String url ="https://api.vk.com/method/messages.getConversationsById?access_token="+token+"&extended=1&peer_ids="+chat_id+"&v=5.131";//
        System.out.println("response");

            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            System.out.println(response);
                            try {
                                answer[0] = new JSONObject(response).getJSONObject("response").getJSONArray("items").getJSONObject(0).getJSONObject("chat_settings").get("title").toString();

                            } catch (JSONException e) {
                                answer[0] = "Error 404";
                                e.printStackTrace();
                            }
                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                            builder.setContentTitle(answer[0]);
                            int notificationId = View.generateViewId();
                            notificationManager.notify(notificationId, builder.build());


                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                }
            });

            queue.add(stringRequest);
    }
    public boolean isInternetAvailable() {
        try {
            InetAddress address = InetAddress.getByName("www.google.com");
            return !address.equals("");
        } catch (UnknownHostException e) {
            // Log error
        }
        return false;
    }
    public String getTextFileData(String fileName) {

        StringBuilder text = new StringBuilder();


        try {


            FileInputStream fIS = getApplicationContext().openFileInput(fileName);
            InputStreamReader isr = new InputStreamReader(fIS, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);

            String line;

            while ((line = br.readLine()) != null) {
                text.append(line + '\n');
            }
            br.close();
        } catch (IOException e) {
            Log.e("Error!", "Error occured while reading text file from Internal Storage!");

        }

        return text.toString();

    }
    public static String readSourceData(String name){
        StringBuilder temp = new StringBuilder();
        try
        {
            File f = new File(CacheDir,name +".txt");
            FileInputStream fin = new FileInputStream(f);
            BufferedReader br = new BufferedReader(new InputStreamReader(fin, StandardCharsets.UTF_8));
            int a;
            while ((a = br.read()) != -1)
            {
                temp.append((char)a);
            }
            fin.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    return temp.toString();
    }
    private void addDialog(LinearLayout main, String title, String last_msg, Long timestamp, ImageView logo, ImageView logo_last, int unreadMsgCount){
        main.setOrientation(HORIZONTAL);
        main.setBackground(ResourcesCompat.getDrawable(getResources(),R.drawable.ripple,null));
        ViewGroup.LayoutParams main_params = new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        main.setLayoutParams(main_params);
        main.setPadding(0, 20, 0, 20);


        LinearLayout text_layout = new LinearLayout(getApplicationContext());
        text_layout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams text_params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        text_layout.setLayoutParams(text_params);



        ConstraintLayout title_layout = new ConstraintLayout(getApplicationContext());
        title_layout.setId(View.generateViewId());

        ConstraintLayout.LayoutParams title_params = new ConstraintLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        title_layout.setLayoutParams(title_params);



        LinearLayout last_msg_layout = new LinearLayout(getApplicationContext());
        last_msg_layout.setOrientation(HORIZONTAL);
        last_msg_layout.setLayoutParams(text_params);
        LinearLayout.LayoutParams last_msg_params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        last_msg_params.setMargins(0, convertDpToPixel(7), 0, 0);
        last_msg_layout.setLayoutParams(last_msg_params);



        TextView titleV= new TextView(getApplicationContext());
        titleV.setId(View.generateViewId());
        titleV.setTextSize(16);
        ViewGroup.LayoutParams text = new ViewGroup.LayoutParams(0, WRAP_CONTENT);
        titleV.setLayoutParams(text);
        titleV.setText(title);
        titleV.setMaxLines(1);
        titleV.setEllipsize(TextUtils.TruncateAt.END);


        TextView last_msgV = new TextView(getApplicationContext());
        last_msgV.setTextSize(15);
        last_msgV.setText(last_msg);
        last_msgV.setMaxLines(1);
        last_msgV.setEllipsize(TextUtils.TruncateAt.END);



        TextView last_time = new TextView(getApplicationContext());
        last_time.setId(View.generateViewId());
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm", Locale.ROOT);
        String dateString = formatter.format(new Date(timestamp*1000));
        last_time.setText(dateString);
        last_time.setPadding(0, 0, convertDpToPixel(20), 0);



        //if(logo_last.getDrawable() != null)
            logo_last.setLayoutParams(new ViewGroup.LayoutParams(convertDpToPixel(28), convertDpToPixel(28)));

        logo.setAdjustViewBounds(true);
        logo.setScaleType(ImageView.ScaleType.FIT_XY);
        logo.setLayoutParams(new ViewGroup.LayoutParams(WRAP_CONTENT, convertDpToPixel(60)));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(convertDpToPixel(60), convertDpToPixel(60));
        params.setMargins(convertDpToPixel(10), 0, convertDpToPixel(10), 0);
        logo.setLayoutParams(params);



        main.addView(logo);
        main.addView(text_layout);

        title_layout.addView(titleV);
        title_layout.addView(last_time);

        text_layout.addView(title_layout);
        text_layout.addView(last_msg_layout);

        //if(logo_last.getDrawable() != null)
            last_msg_layout.addView(logo_last);
        last_msg_layout.addView(last_msgV);

        Log.i("msg", String.valueOf(unreadMsgCount));
        if(unreadMsgCount > 0){
            PorterDuffColorFilter greyFilter = new PorterDuffColorFilter(unreadMessageColor, PorterDuff.Mode.MULTIPLY);
            main.setBackground(ResourcesCompat.getDrawable(getResources(),R.drawable.unread_ripple,null));
            main.getBackground().setColorFilter(greyFilter);

            TextView unreadMsgs = new TextView(getApplicationContext());
            unreadMsgs.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            unreadMsgs.setText(String.valueOf(unreadMsgCount));
            unreadMsgs.setTextColor(Color.WHITE);

            GradientDrawable round_back = new GradientDrawable();
            round_back.setColor(Color.BLACK);
            round_back.setCornerRadius(50);
            unreadMsgs.setBackground(round_back);

            unreadMsgs.setMaxLines(1);
            LinearLayout.LayoutParams last_params = (LinearLayout.LayoutParams) last_msgV.getLayoutParams();
            last_params.weight = 1;
            last_msgV.setLayoutParams(last_params);
            LinearLayout.LayoutParams last_params1 = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            last_params1.setMargins(0, 0, convertDpToPixel(23), 0);
            unreadMsgs.setLayoutParams(last_params1);
            unreadMsgs.setPadding(convertDpToPixel(11), 3, convertDpToPixel(11), 3);


            last_msg_layout.addView(unreadMsgs);
        }else
            if(unreadMsgCount < 0)
                last_msg_layout.setBackground(corners);
        LinearLayout.LayoutParams last_params = (LinearLayout.LayoutParams) last_msgV.getLayoutParams();//main
        last_params.setMargins(convertDpToPixel(5), 0, 0, 0);
        last_msgV.setLayoutParams(last_params);



        ConstraintSet c = new ConstraintSet();
        c.clone(title_layout);
        c.connect(titleV.getId(), ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT);
        c.connect(titleV.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
        c.connect(titleV.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
        c.connect(titleV.getId(), ConstraintSet.RIGHT, last_time.getId(), ConstraintSet.LEFT);
        c.connect(last_time.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
        c.connect(last_time.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
        c.connect(last_time.getId(), ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT);
        c.setHorizontalBias(titleV.getId(), 0);
        title_layout.setConstraintSet(c);
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run(){
                scroll.addView(main);


                main.setClickable(true);
                main.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //Toast.makeText(getApplicationContext(),"negr  "+view.getId(),Toast.LENGTH_SHORT).show();
                        TextView chat_name = (TextView) ((ViewGroup)(((ViewGroup)((ViewGroup) view).getChildAt(1)).getChildAt(0))).getChildAt(0);
                        Intent intent = new Intent(MainActivity.this, msg.class);
                        intent.putExtra("id", Integer.toString(view.getId()));
                        if(view.getTag().toString().contains("split")){
                            intent.putExtra("unreadMsgCount", view.getTag().toString().split("split")[0]);
                            intent.putExtra("chat_info", view.getTag().toString().split("split")[1]);
                        }else{
                            intent.putExtra("unreadMsgCount", view.getTag().toString());
                            intent.putExtra("chat_info", "");
                        }
                        intent.putExtra("chat_name", chat_name.getText());

                        startActivity(intent);
                    }
                });
//            }
//        });
    }
}