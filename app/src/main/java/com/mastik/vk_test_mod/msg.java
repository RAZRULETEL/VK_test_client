package com.mastik.vk_test_mod;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

public class msg extends AppCompatActivity {


    private int id, unread_msgs, last_conversation_id;
    private String token;
    private static RequestQueue queue;
    private LinearLayout scroll;
    private boolean history_loaded = false;

    @Override
    public void onBackPressed(){
        MovingRgbGradient.stop();
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_msg);

        ImageButton back = findViewById(R.id.backButton);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MovingRgbGradient.stop();
                finish();
            }
        });

        id = Integer.parseInt(getIntent().getStringExtra("id"));
        unread_msgs = Integer.parseInt(getIntent().getStringExtra("unreadMsgCount"));
        token = getResources().getString(R.string.token);
        scroll = findViewById(R.id.scroll);
        scroll.setId(id);//
        /*ScrollView msg_scroll = findViewById(R.id.msg_scroll);
        msg_scroll.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View view, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                Log.i("drag list", oldScrollY+" -> "+scrollY);

            }
        });*/
        System.out.println(isInternetAvailable());

        ImageView chat_logo = findViewById(R.id.chatLogo);
        VKImage logo_bit;
        try {
            logo_bit = new VKImage(null, getIntent().getStringExtra("id"), getApplicationContext()).init();
        } catch (IOException e) {
            logo_bit = new VKImage(getApplicationContext());
            //e.printStackTrace();
        }
        chat_logo.setImageBitmap(logo_bit.getRoundedCorner());
        TextView chat_name = findViewById(R.id.chatName);
        chat_name.setText(getIntent().getStringExtra("chat_name"));
        TextView chat_info = findViewById(R.id.chatInfo);
        chat_info.setText(null);
        if(getIntent().getStringExtra("chat_info") != null && !getIntent().getStringExtra("chat_info").equals("")){
            try {
                JSONObject chat = new JSONObject(getIntent().getStringExtra("chat_info"));
                if(chat.has("title")) {
                    chat_info.setText(Integer.toString(chat.getJSONArray("active_ids").length()));
                    chat_info.append("/" + chat.get("members_count") + " | ");
                }
                if(chat.has("sex")){
                    long epochInMillis = Long.parseLong(chat.getJSONObject("online_info").get("last_seen").toString()) * 1000;
                    if(chat.getJSONObject("online_info").get("is_online").toString().equals("false")){
                    Calendar now = Calendar.getInstance();
                    Calendar timeToCheck = Calendar.getInstance();
                    timeToCheck.setTimeInMillis(epochInMillis);

                    if(now.get(Calendar.YEAR) == timeToCheck.get(Calendar.YEAR)) {
                        if(now.get(Calendar.DAY_OF_YEAR) == timeToCheck.get(Calendar.DAY_OF_YEAR)) {
                            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm", Locale.ROOT);
                            String dateString = formatter.format(new Date(epochInMillis));
                            chat_info.setText(dateString+" | ");
                        }else{
                            SimpleDateFormat formatter = new SimpleDateFormat("dd.MM,HH:mm", Locale.ROOT);
                            String dateString = formatter.format(new Date(epochInMillis));
                            chat_info.setText(dateString+" | ");
                        }
                    }
                    }else {
                        chat_info.setText("онлайн");
                        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm", Locale.ROOT);
                        String dateString = formatter.format(new Date(epochInMillis));
                        chat_info.append("("+dateString+") | ");
                    }


                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        queue = Volley.newRequestQueue(this);
        JSONObject chat = null;
        try {
            chat = new JSONObject(getIntent().getStringExtra("chat_info"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String url;
        if(chat != null && chat.has("sex"))
            url ="https://api.vk.com/method/messages.getHistory?peer_id="+id+"&access_token="+token+"&v=5.131";
        else
            url ="https://api.vk.com/method/messages.getHistory?peer_id="+id+"&access_token="+token+"&extended=1&count=30&v=5.131";

// Request a string response from the provided URL.
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                chat_info.append(new JSONObject(response).getJSONObject("response").get("count")+"✉");
                                JSONArray last_msgs = new JSONObject(response).getJSONObject("response").getJSONArray("items");
                                JSONArray profiles = null;
                                if(new JSONObject(response).getJSONObject("response").has("profiles")){
                                    profiles = new JSONObject(response).getJSONObject("response").getJSONArray("profiles");
                                    for(int i = 0; i < profiles.length(); i++)
                                        writeData(profiles.getJSONObject(i).toString(), "user_"+profiles.getJSONObject(i).get("id"));
                                }
                                //System.out.println(profiles.toString());
                                for(int i = 0; i < last_msgs.length(); i++) {
                                    JSONObject this_msg = last_msgs.getJSONObject(i);
                                    /*if(i > 0 && this_msg.getInt("conversation_message_id") - 1 != last_msgs.getJSONObject(i-1).getInt("conversation_message_id")){
                                        String[] msg_db = readSourceData("chat_" + id).split("\n");
                                        for(String msg : msg_db)
                                            if(msg.contains(this_msg.getInt("conversation_message_id") - 1 + ""))
                                                addMessage(scroll, new JSONObject(msg), false);
                                    }*/
                                    int finalI = i;
                                    new Thread(()-> {
                                        try {
                                            Thread.sleep(finalI * 50L);
                                            addMessage(scroll, this_msg, false);
                                        } catch (JSONException | InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }).start();

                                }
                                last_conversation_id = last_msgs.getJSONObject(last_msgs.length() - 1).getInt("conversation_message_id");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            ViewTreeObserver vto = scroll.getViewTreeObserver();
                            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    //scroll.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                    ScrollView msg_scroll = findViewById(R.id.msg_scroll);
                                    LinearLayout background = findViewById(R.id.main_back);
                                    double height = scroll.getMeasuredHeight()-background.getHeight();
//                                    if(height < 0)
//                                        height = 0;
                                    //Log.i("heights", height+" "+scroll.getMeasuredHeight()+ " - "+background.getHeight());

                                    MovingRgbGradient.init(getApplicationContext());
                                    int[] colors = MovingRgbGradient.generateColors(MovingRgbGradient.getFrequency(), MovingRgbGradient.getScale());
                                    for(int i = 0; i < Math.ceil(height/400); i++) {
                                        TextView back_cell = new TextView(getApplicationContext());
                                        back_cell.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, 400, 1));
                                        GradientDrawable cont_gradient = new GradientDrawable();
                                        cont_gradient.setColors(colors);
                                        colors = MovingRgbGradient.generateContinue(colors, MovingRgbGradient.getScale());
                                        back_cell.setBackground(cont_gradient);
                                        background.addView(back_cell);
                                    }
                                    MovingRgbGradient.animate(background);
                                }
                            });
                            history_loaded = true;
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_SHORT).show();
                    String[] messages = readSourceData("chat_"+id).split("\n");
                    last_conversation_id = 0;
                    for (String message : messages) {
                        JSONObject this_msg = null;
                        try {
                            this_msg = new JSONObject(message);
                            if(this_msg.getInt("conversation_message_id") > last_conversation_id)
                                last_conversation_id = this_msg.getInt("conversation_message_id");
                            addMessage(scroll, this_msg, false);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    ViewTreeObserver vto = scroll.getViewTreeObserver();
                    vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            scroll.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            ScrollView msg_scroll = findViewById(R.id.msg_scroll);

                            double height = scroll.getMeasuredHeight();

                            Log.i("heights", height+" "+msg_scroll.getMeasuredHeight());
                            LinearLayout background = findViewById(R.id.main_back);
                            MovingRgbGradient.init(getApplicationContext());
                            int[] colors = MovingRgbGradient.generateColors(MovingRgbGradient.getFrequency(), MovingRgbGradient.getScale());
                            for(int i = 0; i < Math.ceil(height/400); i++) {
                                TextView back_cell = new TextView(getApplicationContext());
                                back_cell.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, 400, 1));
                                GradientDrawable cont_gradient = new GradientDrawable();
                                cont_gradient.setColors(colors);
                                colors = MovingRgbGradient.generateContinue(colors, MovingRgbGradient.getScale());
                                back_cell.setBackground(cont_gradient);
                                background.addView(back_cell);

                            }
                            MovingRgbGradient.animate(background);
                        }
                    });
                    history_loaded = false;
                }
            });

// Add the request to the RequestQueue.
            queue.add(stringRequest);

        GradientDrawable nice = new GradientDrawable();
        nice.setColors(MovingRgbGradient.generateColors(5));
        Log.i("test1", String.valueOf(scroll.getHeight()));

        //MainActivity.getLongPollServer(getApplicationContext(), token, getClass().getName(), scroll);
        LongPollService.addLongPollListener(new LongPollService.longPollService() {
            @Override
            public void onNewEvent(JSONArray update) {
                try {
                    for (int i = 0; i < update.length(); i++) {
                        if (update.getJSONArray(i).get(0).toString().equals("4")) {
                            int peer_id = Integer.parseInt(update.getJSONArray(i).get(3).toString());
                            String from_id;
                            if (update.getJSONArray(i).length() >= 7 && update.getJSONArray(i).getJSONObject(6).has("from"))
                                from_id = update.getJSONArray(i).getJSONObject(6).get("from").toString();
                            else
                                if((Integer.parseInt(update.getJSONArray(i).get(2).toString()) & 2) == 0)
                                    from_id = update.getJSONArray(i).get(3).toString();
                                else
                                    from_id = getResources().getString(R.string.this_id);
                            JSONObject message = new JSONObject();
                            message.put("date", update.getJSONArray(i).get(4).toString());
                            message.put("from_id", from_id);
                            message.put("id", update.getJSONArray(i).get(1).toString());
                            if((Integer.parseInt(update.getJSONArray(i).get(2).toString()) & 2) == 0)
                                message.put("out", "0");
                            else
                                message.put("out", "1");
                            message.put("attachments", new JSONArray());
                            message.put("conversation_message_id", ++last_conversation_id);
                            message.put("text", update.getJSONArray(i).get(5).toString());


                            message.put("peer_id", peer_id);
                            if(history_loaded)
                            addMessage(scroll, message, true);
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        });
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
//    private Bitmap readBitmap(String name){
//        Bitmap bitmap;
//        try
//        {
//            File f = new File(getCacheDir(),name + ".png");
//            FileInputStream fin = new FileInputStream(f);
//            bitmap = BitmapFactory.decodeStream(fin);
//            fin.close();
//            return bitmap;
//        }
//        catch (IOException e)
//        {
//            e.printStackTrace();
//            return null;
//        }
//    }
//    private void writeData(Bitmap img, String value){
//        try
//        {
//            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
//            img.compress(Bitmap.CompressFormat.PNG, 100, bytes);
//            File f = new File(getCacheDir(),value +".png");
//            FileOutputStream fo = new FileOutputStream(f);
//            fo.write(bytes.toByteArray());
//            fo.close();
//        }
//        catch (IOException e)
//        {
//            e.printStackTrace();
//            Toast.makeText(getApplicationContext(),"Error "+e,Toast.LENGTH_SHORT).show();
//            /*if(e.toString().contains("EPERM"))
//                requestPermission();
//            if(e.toString().contains("open failed")) {
//                if(dir.mkdirs())
//                    writeData(img, value, dir);
//            }*/
//        }
//    }
    private void writeData(String info, String name){
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
    /*public void AsyncSetImage(String method_and_params, String path, ImageView imgView){
        String url ="https://api.vk.com/method/"+method_and_params+"&access_token="+token+"&v=5.131";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if(method_and_params.startsWith("users")) {
                            try {
                                writeData(new JSONObject(response).getJSONArray("response").getJSONObject(0).toString(), "user_"+new JSONObject(response).getJSONArray("response").getJSONObject(0).get("id").toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        String rsp = response;
                        String[] path_smpl = path.split("/");
                        try {
                            for(int i = 0;i<path_smpl.length-1;i++){

                                if(path_smpl[i+1].charAt(0) != '['){
                                    if(path_smpl[i].charAt(0) == '['){

                                        rsp = new JSONArray(rsp).getJSONObject(Integer.parseInt(path_smpl[i].replaceAll("[^0-9]",""))).toString();

                                    }else{

                                        rsp = new JSONObject(rsp).getJSONObject(path_smpl[i]).toString();

                                    }
                                }else{

                                    rsp = new JSONObject(rsp).getJSONArray(path_smpl[i]).toString();
                                }

                            }
                            Bitmap logo_img = null;
                            try {
                                if(new JSONObject(rsp).has("photo_50"))
                                    logo_img = getBitmapFromURL(new JSONObject(rsp).get("photo_50").toString());
                                else
                                    logo_img = getBitmapFromURL(new JSONObject(rsp).get("photo").toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            if(logo_img != null) {
                                writeData(logo_img, new JSONObject(rsp).get("id").toString());
                                imgView.setImageBitmap(logo_img);
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_SHORT).show();
            }
        });

// Add the request to the RequestQueue.
        queue.add(stringRequest);

    }*/
    public int convertDpToPixel(float dp){
        return Math.round(dp * ((float) getApplicationContext().getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }
    private void addMessage(LinearLayout msg_cont, JSONObject message_object, boolean to_end) throws JSONException {
        saveMessage(getIntent().getStringExtra("id"), message_object);
        LinearLayout message = new LinearLayout(getApplicationContext());
        message.setOrientation(LinearLayout.HORIZONTAL);
        message.setPadding(convertDpToPixel(5),convertDpToPixel(6),convertDpToPixel(13),convertDpToPixel(6));
        LinearLayout.LayoutParams msg_params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        if(msg_cont != scroll){
            msg_params.width = WRAP_CONTENT;
            msg_params.weight = 1;
        }
        msg_params.setMargins(0, convertDpToPixel(5), 0, convertDpToPixel(5));
        message.setLayoutParams(msg_params);
        if(msg_cont == scroll && unread_msgs++ < 0){
            message.setBackgroundColor(MainActivity.unreadMessageColor);
        }



        LinearLayout msg_content = new LinearLayout(getApplicationContext());
        msg_content.setOrientation(LinearLayout.VERTICAL);
        msg_content.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, 1));



        TextView newMsg = new TextView(getApplicationContext());
        newMsg.setId(View.generateViewId());
        newMsg.setTextSize(15);
        newMsg.setText(message_object.get("text").toString());
        LinearLayout.LayoutParams txt = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        txt.setMargins(convertDpToPixel(10), 0, convertDpToPixel(10), 0);
        newMsg.setLayoutParams(txt);


        msg_content.addView(newMsg);
        if(message_object.has("reply_message")){
            msg_content.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, 1));
            GradientDrawable reply = new GradientDrawable();
            reply.setColor(Color.LTGRAY);
            reply.setCornerRadius(VKImage.getCornerRadius());
            msg_content.setBackground(reply);

            msg_content.setId(View.generateViewId());
            JSONObject reply_msg = message_object.getJSONObject("reply_message");
            reply_msg.put("out", "0");
            addMessage(msg_content, reply_msg, false);
        }



        LinearLayout msg_layout = new LinearLayout(getApplicationContext());
        msg_layout.setId(View.generateViewId());



        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm", Locale.ROOT);
        String dateString = formatter.format(new Date(Long.parseLong(message_object.get("date").toString())*1000));

        TextView time = new TextView(getApplicationContext());
        time.setId(View.generateViewId());
        time.setTextSize(15);
        time.setText(dateString);
        time.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams time_ls = new LinearLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT);
        time.setLayoutParams(time_ls);



        ImageView logo = new ImageView(getApplicationContext());
        if(message_object.get("out").toString().equals("0")) {
            try {
                logo.setImageBitmap(new VKImage(null, message_object.get("from_id").toString(), getApplicationContext()).init().getImg());
            } catch (IOException e) {
                if (Integer.parseInt(message_object.get("from_id").toString()) < 0)
                    VKImage.asyncSetImage(VKImage.ImageOwner.GROUP, message_object.get("from_id").toString(),  logo, getApplicationContext());
                else
                    VKImage.asyncSetImage(VKImage.ImageOwner.USER, message_object.get("from_id").toString(),  logo, getApplicationContext());

            }

        }



        if(message_object.get("out").toString().equals("0")) {
            txt.weight = 1;
            msg_layout.addView(msg_content);
        }else{
            msg_layout.setHorizontalGravity(Gravity.RIGHT);
            msg_content.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
            msg_layout.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
            msg_layout.addView(time);

        }
        if(message_object.get("out").toString().equals("0"))
            message.addView(logo);
        message.addView(msg_layout);



        GradientDrawable msg_style = new GradientDrawable();
        msg_style.setCornerRadius(10);
        msg_style.setColor(Color.parseColor("#ffffff"));//#FFD6D6D6
        newMsg.setTextColor(Color.BLACK);
        if(!newMsg.getText().equals("")) {
            newMsg.setBackground(msg_style);
            newMsg.setPadding(convertDpToPixel(5), convertDpToPixel(3), convertDpToPixel(5), convertDpToPixel(3));
        }else{
            if(message_object.has("action")){
                msg_style.setColor(Color.GRAY);
                newMsg.setBackground(msg_style);
                newMsg.setTextColor(getResources().getColor(R.color.test, null));
                newMsg.setPadding(convertDpToPixel(5), convertDpToPixel(3), convertDpToPixel(5), convertDpToPixel(3));
                JSONObject profile = new JSONObject(readSourceData("user_"+message_object.getJSONObject("action").get("member_id")));
                newMsg.setText(profile.get("first_name")+" "+profile.get("last_name")+" ");
                newMsg.append(MainActivity.actions.get(message_object.getJSONObject("action").get("type").toString()));
                if(message_object.getJSONObject("action").get("type").toString().contains("title"))
                    newMsg.append(message_object.getJSONObject("action").get("text").toString());
            }else
                if(!message_object.getJSONArray("attachments").toString().contains("group_call_in_progress")) {
                    txt = new LinearLayout.LayoutParams(0, 0);
                    newMsg.setLayoutParams(txt);
                }
        }
        runOnUiThread(()->{
            if(to_end)
                msg_cont.addView(message, 0);
            else
                msg_cont.addView(message);
        });




        if(message_object.get("out").toString().equals("0")) {
            logo.getLayoutParams().width = convertDpToPixel(35);
            logo.getLayoutParams().height = convertDpToPixel(35);
            LinearLayout.LayoutParams logo_params = (LinearLayout.LayoutParams) logo.getLayoutParams();
            logo_params.gravity = Gravity.TOP;
            runOnUiThread(()->logo.setLayoutParams(logo_params));
        }

        int[] attachments_ids = new int[message_object.getJSONArray("attachments").length()];
        GradientDrawable attach_style = new GradientDrawable();
        attach_style.setCornerRadius(10);
        attach_style.setColor(Color.WHITE);
        LinearLayout.LayoutParams margins = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        margins.setMargins(convertDpToPixel(10), 0, convertDpToPixel(10), 0);
        for(int  l = 0; l < message_object.getJSONArray("attachments").length(); l++){
            switch(message_object.getJSONArray("attachments").getJSONObject(l).get("type").toString()){
                case "photo":
                    ImageView img_attachment = new ImageView(getApplicationContext());
                    msg_content.addView(img_attachment);
                    JSONObject photo_attach = message_object.getJSONArray("attachments").getJSONObject(l).getJSONObject("photo");
                    LinearLayout.LayoutParams imgV = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, 1);
                    img_attachment.setLayoutParams(imgV);
                    //VKImage img = null;
                    for(int indx = 0; indx < photo_attach.getJSONArray("sizes").length(); indx++){
                        if(photo_attach.getJSONArray("sizes").getJSONObject(indx).get("type").equals("x")){
//                            imgV = new LinearLayout.LayoutParams(Integer.parseInt(photo_attach.getJSONArray("sizes").getJSONObject(indx).get("width").toString()), Integer.parseInt(photo_attach.getJSONArray("sizes").getJSONObject(indx).get("height").toString()));
//                            img_attachment.setLayoutParams(imgV);
                            img_attachment.setImageBitmap(new VKImage(getApplicationContext()).getImg());
                            img_attachment.setMinimumHeight(Integer.parseInt(photo_attach.getJSONArray("sizes").getJSONObject(indx).get("height").toString()));
                            img_attachment.setMinimumWidth(Integer.parseInt(photo_attach.getJSONArray("sizes").getJSONObject(indx).get("width").toString()));
                            int finalIndx = indx;
                            new Thread(() -> {
                                VKImage img;
                                try {
                                    img = new VKImage(photo_attach.getJSONArray("sizes").getJSONObject(finalIndx).get("url").toString(), photo_attach.get("id").toString(), getApplicationContext()).init();
                                } catch (JSONException | IOException e) {
                                    img = new VKImage(getApplicationContext());
                                }
                                VKImage finalImg = img;
                                runOnUiThread(()-> img_attachment.setImageBitmap(finalImg.getImg()));
                            }).start();
                            break;
                        }
                    }


                    runOnUiThread(()->{
                    //img_attachment.setImageBitmap(img.getImg());
                    img_attachment.setId(View.generateViewId());
                        try {
                            img_attachment.setTag("img_"+photo_attach.get("id"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        img_attachment.setBackground(attach_style);
                    img_attachment.setPadding(convertDpToPixel(5), convertDpToPixel(5), convertDpToPixel(5), convertDpToPixel(5));
                    img_attachment.setLayoutParams(margins);
                    img_attachment.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(msg.this, FullscreenImage.class);
                            intent.putExtra("img", view.getTag().toString());

                            startActivity(intent);
                        }
                    });
                    });
                    attachments_ids[l] = img_attachment.getId();
                break;
                case "sticker":
                    JSONObject sticker = message_object.getJSONArray("attachments").getJSONObject(l).getJSONObject("sticker");
                    ImageView sticker_img = new ImageView(getApplicationContext());
                    msg_content.addView(sticker_img);
                    String sticker_url = sticker.getJSONArray("images").getJSONObject(1).get("url").toString();
                    LinearLayout.LayoutParams stick = new LinearLayout.LayoutParams(convertDpToPixel(192), convertDpToPixel(192));
                    sticker_img.setLayoutParams(stick);
                    VKImage stick_bit = null;
                    try {
                        stick_bit = new VKImage(sticker_url, sticker.get("sticker_id").toString(), "stick_", getApplicationContext()).init();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    sticker_img.setImageBitmap(stick_bit.getImg());
                    sticker_img.setId(View.generateViewId());
                    attachments_ids[l] = sticker_img.getId();
                break;
                case "group_call_in_progress":
                    msg_style.setColor(Color.GRAY);
                    newMsg.setBackground(msg_style);
                    newMsg.setTextColor(getResources().getColor(R.color.test, null));
                    newMsg.setPadding(convertDpToPixel(5), convertDpToPixel(3), convertDpToPixel(5), convertDpToPixel(3));

                    JSONObject group_call = message_object.getJSONArray("attachments").getJSONObject(l);
                    newMsg.setText("Групповой звонок в прогрессе \n Участников: " + group_call.getJSONObject("group_call_in_progress").getJSONObject("participants").get("count"));
                break;
                case "video":
                    JSONObject video = message_object.getJSONArray("attachments").getJSONObject(l).getJSONObject("video");
                    ImageView video_preview = new ImageView(getApplicationContext());

                    JSONObject preview_obj = video.getJSONArray("image").getJSONObject(1);
                    String preview_url = preview_obj.get("url").toString();

                    VKImage preview_bit = null;
                    try {
                        preview_bit = new VKImage(preview_url, video.get("id").toString(), "video_", getApplicationContext()).init();
                    } catch (IOException e) {
                        preview_bit = new VKImage(getApplicationContext());
                        e.printStackTrace();
                    }
                    video_preview.setImageBitmap(preview_bit.getImg());

                    GradientDrawable vid_style = new GradientDrawable();
                    vid_style.setCornerRadius(10);
                    vid_style.setColor(Color.WHITE);

                    FrameLayout video_frame = new FrameLayout(getApplicationContext());
                    video_frame.setId(View.generateViewId());
                    video_frame.addView(video_preview);
                    video_frame.setLayoutParams(new FrameLayout.LayoutParams(convertDpToPixel(300), convertDpToPixel(169)));
                    video_frame.setBackground(vid_style);
                    video_frame.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String videourl;
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            try {
                                if(!video.getJSONObject("files").has("external")) {
                                    //FrameLayout main = findViewById(R.id.main_frame);
                                    LinearLayout video_quality = new LinearLayout(getApplicationContext());
                                    video_quality.setOrientation(LinearLayout.VERTICAL);
                                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(getApplicationContext().getResources().getDisplayMetrics().widthPixels/2, WRAP_CONTENT);
                                    params.gravity = Gravity.CENTER;
                                    video_quality.setLayoutParams(params);
                                    video_quality.setBackgroundColor(Color.YELLOW);
                                    video_quality.setTranslationZ(10);
                                    Iterator<String> keys = video.getJSONObject("files").keys();
                                    video_quality.setGravity(Gravity.CENTER);
                                    while (keys.hasNext()) {
                                        String key = keys.next();
                                        if(key.contains("dash") || key.contains("failover"))
                                            continue;
                                        TextView variant = new TextView(getApplicationContext());
                                        variant.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                        variant.setText(key);
                                        variant.setGravity(Gravity.CENTER);
                                        video_quality.addView(variant);
                                        variant.setClickable(true);
                                        variant.setTextSize(30);
                                        variant.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, (int) variant.getTextSize()*2, 1));
                                        variant.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                try {
                                                    String videourl = video.getJSONObject("files").get(((TextView)v).getText().toString()).toString();
                                                    intent.setDataAndType(Uri.parse(videourl), "video/*");
                                                    startActivity(intent);
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                    Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_SHORT).show();

                                                }

                                            }
                                        });
                                    }
                                    //main.addView(video_quality);
                                    Log.e("height",video_quality.getChildAt(0).getHeight()+"");
                                    new AlertDialog.Builder(msg.this)
                                            .setView(video_quality)
                                            .show().getWindow().setLayout((int) (getApplicationContext().getResources().getDisplayMetrics().widthPixels/1.3), (int) (((TextView)video_quality.getChildAt(0)).getTextSize()*2*video_quality.getChildCount()));

//                                    if (isConnectedWifi())
//                                        videourl = video.getJSONObject("files").get("hls").toString();
//                                    else
//                                        videourl = video.getJSONObject("files").get("mp4_360").toString();
//                                    intent.setDataAndType(Uri.parse(videourl), "video/*");
                                }else{
                                    videourl = video.getJSONObject("files").get("external").toString();
                                    intent.setData(Uri.parse(videourl));
                                    startActivity(intent);
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }


                        }
                    });

                    video_preview.setPadding(convertDpToPixel(5), convertDpToPixel(5), convertDpToPixel(5), convertDpToPixel(5));
                    video_preview.setLayoutParams(new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
                    video_preview.setAdjustViewBounds(true);
                    video_preview.setScaleType(ImageView.ScaleType.FIT_XY);

                    ImageView video_play = new ImageView(getApplicationContext());
                    video_play.setImageBitmap(BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_menu_more));
                    video_play.setLayoutParams(new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
                    ((FrameLayout.LayoutParams) video_play.getLayoutParams()).gravity = Gravity.CENTER;
                    video_play.setRotation(-90);
                    video_play.setScaleX(2);
                    video_play.setScaleY(2);
                    video_frame.addView(video_play);

                    msg_content.addView(video_frame);
                    attachments_ids[l] = video_frame.getId();
                break;
                case "doc":
                    JSONObject doc = message_object.getJSONArray("attachments").getJSONObject(l).getJSONObject("doc");
                    //.setPadding(convertDpToPixel(5), convertDpToPixel(5), convertDpToPixel(5), convertDpToPixel(5));
                    LinearLayout doc_main = new LinearLayout(getApplicationContext());
                    doc_main.setOrientation(LinearLayout.HORIZONTAL);

                    ImageView icon = new ImageView(getApplicationContext());
                    icon.setImageDrawable(AppCompatResources.getDrawable(getApplicationContext(), R.drawable.document));
                    icon.setLayoutParams(new LinearLayout.LayoutParams(50,50));
                    icon.setScaleType(ImageView.ScaleType.FIT_XY);
                    doc_main.addView(icon);

                    TextView doc_name = new TextView(getApplicationContext());
                    doc_name.setText(doc.getString("title"));
                    doc_main.addView(doc_name);

                    doc_main.setTag(doc.getString("url"));

                    doc_main.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent();

                        }
                    });








                break;
            }
        }
        runOnUiThread(()->{
            try {
                if(message_object.get("out").toString().equals("0")){
                    msg_layout.addView(time);
                }else{
                    msg_layout.addView(msg_content);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }
    private void saveMessage(String peer_id, JSONObject message) throws JSONException {
        String msg_cache = readSourceData("chat_"+peer_id);
        //if(msg_cache.length() > 3){
            if(!msg_cache.contains(message.get("id").toString()))
                msg_cache += message + "\n";


            writeData(msg_cache, "chat_"+peer_id);
        //}
    }
    public boolean isConnectedWifi(){
        NetworkInfo info = ((ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return info.toString().replaceAll("([|])","").split(",")[0].split(":")[1].contains("WIFI");
    }
    public boolean isConnectedMobile(){
        NetworkInfo info = ((ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return info.toString().replaceAll("([|])","").split(",")[0].split(":")[1].contains("MOBILE");
    }
    public boolean isInternetAvailable() {
        try {
            InetAddress address = InetAddress.getByName("www.google.com");
            return address.isReachable(3000);
        } catch (IOException e) {
            // Log error
        }
        return false;
    }

    public static RequestQueue getQueue() {
        return queue;
    }
}