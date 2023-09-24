package com.mastik.vk_test_mod.polling;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.mastik.vk_test_mod.MainActivity;
import com.mastik.vk_test_mod.MessagesActivity;
import com.mastik.vk_test_mod.R;
import com.mastik.vk_test_mod.dataTypes.VKImage;
import com.mastik.vk_test_mod.dataTypes.VKUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import timber.log.Timber;

public class NotificationService extends Service {
    private static String token, this_id;
    public final static String message_channel = "message", status_channel = "status";
    private int notification_id;
    String TAG = "Timers";
    @Override
    public IBinder onBind (Intent arg0) {return null;}
    int tag = View.generateViewId();
    @Override
    public int onStartCommand (Intent intent , int flags , int startId) {
        super.onStartCommand(intent , flags , startId) ;
        Log. e ( TAG , "onStartCommand" );
        return START_STICKY ;
    }
    @Override
    public void onCreate () {
        Timber.tag(TAG).e("Run notification service");

        notification_id = (int)System.currentTimeMillis();
        NotificationCompat.Builder notif_g = new NotificationCompat.Builder(getApplicationContext(), status_channel)
                .setContentTitle("Онлайн")
                .setSmallIcon(R.drawable.ricardo)
                .setAutoCancel(true)
                .setSilent(true);
        this.startForeground(notification_id, notif_g.build());
        LongPollService.setErrorListener(new LongPollService.longPollServiceError() {
            @Override
            public void onError(String error) {
                notif_g.setContentTitle("Ошибка");
                notif_g.setContentText(error)
                        .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(error)
                        .setBigContentTitle("Ошибка"));
                startForeground(notification_id, notif_g.build());
            }
        });
        token = MainActivity.getToken();
        this_id = MainActivity.getCurrentUserId()+"";
        if(!LongPollService.isActive())
            getLongPollServer(token);


        LongPollService.setLongPollListener(new LongPollService.longPollService() {
            @Override
            public void onNewEvent(JSONArray update) {
                notif_g.setContentTitle("Онлайн");
                notif_g.setContentText("");
                startForeground(notification_id, notif_g.build());
                try {
                    /*update.put(0, new JSONArray());
                    update.getJSONArray(0).put(0, "4");
                    update.getJSONArray(0).put(1, msg_id);
                    update.getJSONArray(0).put(2, "532497");
                    update.getJSONArray(0).put(3, "2000000052");
                    update.getJSONArray(0).put(4, "1648931192");
                    update.getJSONArray(0).put(5, "+");
                    update.getJSONArray(0).put(6, new JSONObject().put("from", 349987770));*///[4,1096812,532497,2000000052,1648931192,"+",{"from":"183502247"}]
                    for (int i = 0; i < update.length(); i++) {
                        if (update.getJSONArray(i).get(0).toString().equals("4")) {// && (Integer.parseInt(update.getJSONArray(i).get(2).toString()) & 2) == 0
                            int peer_id = Integer.parseInt(update.getJSONArray(i).get(3).toString());
                            String from_id = "";
                            if (update.getJSONArray(i).length() >= 7 && update.getJSONArray(i).getJSONObject(6).has("from"))
                                from_id = update.getJSONArray(i).getJSONObject(6).get("from").toString();
                            else
                                from_id = update.getJSONArray(i).get(3).toString();
                            if((Integer.parseInt(update.getJSONArray(i).get(2).toString()) & 2) != 0)
                                from_id = String.valueOf(MainActivity.getCurrentUserId());
                            //                        JSONObject message = new JSONObject();
                            //                        if((Integer.parseInt(update.getJSONArray(i).get(2).toString()) & 2) == 0)
                            //                            message.put("out", "1");
                            //                        else
                            //                            message.put("out", "0");
                            //                        message.put("from_id", from_id);
                            //                        message.put("date", update.getJSONArray(i).get(4).toString());
                            //                        message.put("text", update.getJSONArray(i).get(5).toString());
                            //                        message.put("attachments", new JSONArray());
                            //                        addMessage(scroll, message, true);
                            if(peer_id > 2000000000) {
                                //Log.i("strt", "1");
                                /*JSONObject profile = new JSONObject(readSourceData("user_"+from_id));
                                String title = profile.get("first_name")+" "+profile.get("last_name");
                                Bitmap logo = MainActivity.readBitmap(from_id,getApplicationContext());
                                if(logo == null)
                                    logo = MainActivity.getBitmapFromURL("https://vk.com/images/camera_50.png");*/
                                //createNotification(title, update.getJSONArray(i).get(5).toString(), logo);
                                chatNotification(String.valueOf(peer_id), update.getJSONArray(i).get(5).toString(), from_id, update.getJSONArray(i).get(1).toString());
                            }else {
                                JSONObject profile = new JSONObject(readSourceData("user_"+from_id));
                                JSONObject prvt_chat_name = new JSONObject(readSourceData("user_"+peer_id));
                                String title = profile.get("first_name")+" "+profile.get("last_name");
                                Bitmap logo = MainActivity.readBitmap(from_id,getApplicationContext());
                                if(logo == null)
                                    logo = MainActivity.getBitmapFromURL("https://vk.com/images/camera_50.png");

                                NotificationManager mNotificationManager = (NotificationManager) getSystemService( NOTIFICATION_SERVICE );
                                boolean is_first_in_group = true;
                                for(StatusBarNotification not : mNotificationManager.getActiveNotifications())
                                    if(not.getNotification().getGroup().equals(String.valueOf(peer_id)))
                                        is_first_in_group = false;
                                if(is_first_in_group && from_id.equals(this_id));
                                else createNotification(title, prvt_chat_name.get("first_name")+" "+prvt_chat_name.get("last_name"), update.getJSONArray(i).get(5).toString(), logo, String.valueOf(peer_id), profile.toString());
                            }
                        }

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        //onTaskRemoved(intent);


    }
    @Override
    public void onDestroy () {
        Timber.tag(TAG).e("Destroy notification service");

        super.onDestroy() ;
    }

    private void createNotification (String from_name, String chat_name, String text, Bitmap logo, String chat_id, String chat_info) {

        if(chat_name.equals(""))
            chat_name = from_name;

        Bitmap chat_logo = VKImage.get(null, Integer.parseInt(chat_id), VKUser.PREFIX, getApplicationContext()).getImg();

        NotificationManager mNotificationManager = (NotificationManager) getSystemService( NOTIFICATION_SERVICE );

        boolean is_first_in_group = true;
        for(StatusBarNotification not : mNotificationManager.getActiveNotifications())
            if(not.getNotification().getGroup().equals(chat_id))
                is_first_in_group = false;
        Intent intent = new Intent(NotificationService.this, MessagesActivity.class);
        intent.putExtra("id", chat_id);
        intent.putExtra("chat_name", chat_name);
        intent.putExtra("chat_info", chat_info);
        intent.putExtra("unreadMsgCount", "0");
        PendingIntent contentIntent = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            contentIntent = PendingIntent. getActivity (NotificationService.this, 0 , intent, PendingIntent.FLAG_MUTABLE+PendingIntent.FLAG_UPDATE_CURRENT);
        }else {
            contentIntent = PendingIntent. getActivity (NotificationService.this, 0 , intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        Notification notif_g = new NotificationCompat.Builder(getApplicationContext(), message_channel)
                .setContentTitle(chat_name)
                .setSmallIcon(R.drawable.ricardo)
                .setLargeIcon(chat_logo)
                .setGroupSummary(true)
                .setGroup(chat_id)
                .setAutoCancel(true)
                .setSilent(true)
                .setStyle(new NotificationCompat.MessagingStyle(from_name + " " + chat_name))
                .setSubText(chat_name)
                .setContentIntent(contentIntent)
                .build();

        mNotificationManager.notify(Integer.parseInt(chat_id), notif_g);

        NotificationCompat.Builder notif = new NotificationCompat.Builder(getApplicationContext(), message_channel)
                .setContentTitle(from_name)
                .setContentText(text)
                .setSmallIcon(R.drawable.ricardo)
                .setLargeIcon(logo)
                .setGroupSummary(false)
                .setGroup(chat_id)
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle()
                .bigText(text)
                .setBigContentTitle(from_name));
        if(is_first_in_group && Integer.parseInt(chat_id) > 2000000000)
            notif.setContentTitle(from_name+" ("+chat_name+")");
        else
            notif.setSilent(true);

        mNotificationManager.notify((int) System.currentTimeMillis(), notif.build()); ;//Integer.parseInt(chat_id)


    }
    private void chatNotification(String peer_id, String text, String from_id, String msg_id){
        String url ="https://api.vk.com/method/messages.getHistory?peer_id="+peer_id+"&access_token="+token+"&count=1&extended=1&start_message_id="+msg_id+"&v=5.131";


// Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        MainActivity.BACKGROUND_THREADS.execute(() -> {//Network on main thread
                            try {
                                JSONObject dialog = new JSONObject(response).getJSONObject("response").getJSONArray("conversations").getJSONObject(0);
                                JSONArray profiles = new JSONObject(response).getJSONObject("response").getJSONArray("profiles");
                                JSONObject profile = null;
                                for (int i = 0; i < profiles.length(); i++) {
                                    writeData(profiles.getJSONObject(i).toString(), "user_" + profiles.getJSONObject(i).get("id"));
                                    if (profiles.getJSONObject(i).get("id").toString().equals(from_id))
                                        profile = profiles.getJSONObject(i);
                                }
                                String title = profile.get("first_name") + " " + profile.get("last_name");
                                Bitmap logo = MainActivity.readBitmap("user_" + from_id, getApplicationContext());
                                if (logo == null) {
                                    if (profile.has("photo_50")) {
                                        logo = MainActivity.getBitmapFromURL(profile.get("photo_50").toString());
                                        if (logo != null)
                                            writeData(logo, "user_" + from_id);
                                    } else
                                        logo = MainActivity.getBitmapFromURL("https://vk.com/images/icons/im_multichat_50.png");
                                }
                                createNotification(title, dialog.getJSONObject("chat_settings").get("title").toString(), text, logo, peer_id, "");

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(),"Error",Toast.LENGTH_SHORT).show();
            }
        });
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(stringRequest);
    }
    private String readSourceData(String name){
        StringBuilder temp = new StringBuilder();
        try
        {
            File f = new File(getCacheDir(),name +".txt");
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
    private void writeData(String info, String name){
        //System.out.println(name);
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
    public void getLongPollServer(String token){
        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
        String poll ="https://api.vk.com/method/messages.getLongPollServer?access_token="+token+"&v=5.131";

        StringRequest longPoll = new StringRequest(Request.Method.GET, poll,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            if(new JSONObject(response).has("response")) {
                                JSONObject rsp = new JSONObject(response).getJSONObject("response");
                                String server = rsp.get("server").toString();
                                String key = rsp.get("key").toString();
                                LongPollService.server = server;
                                LongPollService.key = key;
                                LongPollService.start(Integer.parseInt(rsp.get("ts").toString()), token, getApplicationContext());
                            }else{
                                if(response.contains("invalid access_token (4)")){
                                    //System.out.println(response);
                                    Toast.makeText(getApplicationContext(), "Authorization failed. Retrying...", Toast.LENGTH_LONG).show();
                                    Thread.sleep(300);
                                    getLongPollServer(token);
                                }else{
                                    //System.out.println(response);
                                    Toast.makeText(getApplicationContext(), response, Toast.LENGTH_LONG).show();
                                }

                            }
                        } catch (JSONException | InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), "Error getLongPollServer", Toast.LENGTH_SHORT).show();
            }
        });

// Add the request to the RequestQueue.
        queue.add(longPoll);
    }
}