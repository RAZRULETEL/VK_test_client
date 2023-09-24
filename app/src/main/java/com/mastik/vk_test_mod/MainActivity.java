package com.mastik.vk_test_mod;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.mastik.vk_test_mod.dataTypes.MessageAction;
import com.mastik.vk_test_mod.db.AppDatabase;
import com.mastik.vk_test_mod.dialogs.DialogsFragment;
import com.mastik.vk_test_mod.settings.SettingsFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    public static final ThreadPoolExecutor BACKGROUND_THREADS;
    private static RequestQueue REQUEST_QUEUE;
    public static final int UNREAD_MESSAGE_COLOR = (200 & 0xff) << 24 | (0) << 16 | (0xff) << 8 | (0xff);
    private static String token;
    private static int userId;
    private LinearLayout menu;
    private static boolean runned = false;
    private final static HashMap<String, String> actions = new HashMap<>(), file_type = new HashMap<>();
    private Fragment currentFragment, lastFragment;
    private int displayWidth;

    static {
        BACKGROUND_THREADS = new ThreadPoolExecutor(1, 4,
                20L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
        BACKGROUND_THREADS.allowCoreThreadTimeOut(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Timber.plant(new Timber.DebugTree());
        displayWidth = getApplicationContext().getResources().getDisplayMetrics().widthPixels;

        File cacheDir = getCacheDir();
        MovingRgbGradient.init(getApplicationContext());
        actions.put(MessageAction.KICK.toString(), "выпилился из беседы.");
        actions.put(MessageAction.PIN.toString(), "прикрепил сообщение.");
        actions.put(MessageAction.INVITE.toString(), "присоединился к gay party.");
        actions.put(MessageAction.UNPIN.toString(), "открепил сообщение.");
        actions.put(MessageAction.UPDATE_PHOTO.toString(), "обновил картинку.");
        actions.put(MessageAction.DELETE_PHOTO.toString(), "удалил картинку.");
        actions.put(MessageAction.RENAME.toString(), "изменил название gay бара на ");


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

        BACKGROUND_THREADS.execute(() -> {// Development purposes only
            AppDatabase.getInstance(getApplicationContext()).getMessageContentDAO().clearMessageHistory();
            AppDatabase.getInstance(getApplicationContext()).getMessageDAO().clearMessages();
            AppDatabase.getInstance(getApplicationContext()).getUserDAO().clearUsers();
            AppDatabase.getInstance(getApplicationContext()).getPhotoDAO().clearPhotos();
            AppDatabase.getInstance(getApplicationContext()).getStickerDAO().clearStickers();
            AppDatabase.getInstance(getApplicationContext()).getCachedFileDAO().clearCachedFiles();
            AppDatabase.getInstance(getApplicationContext()).getVideoDAO().clearVideos();
            Timber.tag(MainActivity.class.getSimpleName()).d("CLEAR DB");
        });
        try {// Wait DB clearing
            Thread.sleep(100);
        } catch (InterruptedException ignored) {}

        REQUEST_QUEUE = Volley.newRequestQueue(getApplicationContext());

        SharedPreferences prefs = getApplicationContext().getSharedPreferences(AuthActivity.API_AUTHENTICATION, Context.MODE_PRIVATE);
        token = prefs.getString(AuthActivity.ACCESS_TOKEN, null);
        userId = prefs.getInt(AuthActivity.USER_ID, 0);

        menu = findViewById(R.id.main_menu);
        menu.setTranslationX(-displayWidth);

        LinearLayout tabs = findViewById(R.id.tabs);
        for(Tabs tab : Tabs.values()){
            Button b = new Button(getApplicationContext());
            b.setText(tab.name);
            b.setTextColor(Color.BLACK);
            b.setBackgroundColor(Color.WHITE);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    changeTabTo(tabToFragment(tab));
                }
            });
            tabs.addView(b);
        }

        Fragment addedFragment = new DialogsFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        currentFragment = addedFragment;

        transaction.replace(R.id.main_tab, addedFragment);
        transaction.addToBackStack(null);
        transaction.commit();

        FrameLayout menu_button = findViewById(R.id.menu);
        menu_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("click", "menu");
                toggleMenu();
            }
        });
        Button hide = findViewById(R.id.hide_menu);
        hide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleMenu();
            }
        });
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
        return convertDpToPixel(dp, getApplicationContext());
    }
    public static int convertDpToPixel(float dp, Context context){
        return Math.round(dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT));
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
//        if(!runned)
//            startForegroundService(new Intent(this, NotificationService.class));
//        runned = true;
    }
    private static void syncGroup(Context context, String chat_id, NotificationCompat.Builder builder) {
        RequestQueue queue = Volley.newRequestQueue(context);

        String url ="https://api.vk.com/method/messages.getConversationsById?access_token="+token+"&extended=1&peer_ids="+chat_id+"&v=5.131";//
        System.out.println("response");

            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<>() {
                        @Override
                        public void onResponse(String response) {
                            System.out.println(response);
                            String answer;
                            try {
                                answer = new JSONObject(response).getJSONObject("response").getJSONArray("items").getJSONObject(0).getJSONObject("chat_settings").get("title").toString();
                            } catch (JSONException e) {
                                answer = "Error 404";
                                e.printStackTrace();
                            }
                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                            builder.setContentTitle(answer);
                            int notificationId = View.generateViewId();
                            try{
                            notificationManager.notify(notificationId, builder.build());
                            }catch (SecurityException ignored){}
                        }
                    }, error -> Timber.tag(TAG).e(error));

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
    public static String getFileType(String name){
        return file_type.get(name);
    }
    public static String getAction(String name){
        return actions.get(name);
    }
    private void changeTabTo(Fragment tab){
        if(currentFragment.equals(tab))
            return;
        toggleMenu();
        //((ConstraintLayout)findViewById(R.id.main_constraint_layout)).removeView(((ConstraintLayout) findViewById(R.id.main_constraint_layout)).getChildAt(1));
        lastFragment = currentFragment;
        currentFragment = tab;
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        transaction.replace(R.id.main_tab, tab);
        transaction.addToBackStack(null);
        transaction.commit();
    }
    public void backTab(){
        changeTabTo(lastFragment);
    }
    @Override
    public void onBackPressed(){
        if(menu.getTranslationX() == 0) {
            toggleMenu();
            return;
        }
        if(currentFragment instanceof SettingsFragment)
            if(((SettingsFragment)currentFragment).isSettingOpened()){
                ((SettingsFragment) currentFragment).closeSettings();
            }else
                backTab();
    }
    public Fragment tabToFragment(Tabs tab){
        return switch (tab) {
            case NEWS -> new NewsFragment();
            case SETTINGS -> new SettingsFragment();
            default -> new DialogsFragment();
        };
    }
    enum Tabs{
        DIALOGS("Сообщения"), NEWS("Новости"), SETTINGS("Настройки");
        public final String name;
        Tabs(String name) {
            this.name = name;
        }
    }
    private void toggleMenu(){
        if(menu.getTranslationX() == 0) {
            menu.animate().translationX(-displayWidth).setDuration(200).start();
        }else {
            menu.animate().translationX(0).setDuration(200).start();
        }
    }

    public static RequestQueue getVolleyQueue(Context context){
        if(REQUEST_QUEUE == null)
            REQUEST_QUEUE = Volley.newRequestQueue(context);
        return REQUEST_QUEUE;
    }

    public static String getToken(Context context){
        if(token == null)
            token = context.getSharedPreferences(AuthActivity.API_AUTHENTICATION, Context.MODE_PRIVATE).getString(AuthActivity.ACCESS_TOKEN, null);
        return token;
    }

    public static String getToken(){
        return token;
    }

    public static int getCurrentUserId(){
        return userId;
    }
}