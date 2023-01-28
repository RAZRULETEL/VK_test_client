package com.mastik.vk_test_mod;

import static com.mastik.vk_test_mod.MainActivity.readBitmap;
import static com.mastik.vk_test_mod.MainActivity.token;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

public class LongPollService{
    public static String key, server;
    private static longPollService[] mListener = new longPollService[0];
    private static longPollService notificationListener;
    private static longPollServiceError errorListener;
    private static boolean status = false;
    public static int unread_msg_count = 0;

    public static void start(int ts, Context context){
        status = true;
        System.out.println("Threads: "+Thread.activeCount());

        int TIMEOUT_MS=30000;

        RequestQueue queue = Volley.newRequestQueue(context);

        String poll = "https://"+server+"?act=a_check&key="+key+"&ts="+ts+"&wait=25&mode=2&version=2";

        StringRequest longPoll = new StringRequest(Request.Method.GET, poll,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        Log.i("long", "work "+(mListener != null ? mListener.length : 0)+" "+response);
                        try {
                            for (longPollService li : mListener)
                                li.onNewEvent(new JSONObject(response).getJSONArray("updates"));
                            if (notificationListener != null)
                                notificationListener.onNewEvent(new JSONObject(response).getJSONArray("updates"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
/*
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

//                                                if(Integer.parseInt(last_msgs.getJSONObject(i).get("from_id").toString()) < 0)
//                                                    if(logo_bit == null){
//                                                        AsyncSetImage("groups.getById?group_id="+last_msgs.getJSONObject(i).get("from_id").toString().substring(1),"response/[0]/smth",logo);
//                                                    }else
//                                                        logo.setImageBitmap(getRoundedCornerBitmap(logo_bit, 20));
//                                                else
//                                                if(logo_bit == null){
//                                                    AsyncSetImage("users.get?user_ids="+last_msgs.getJSONObject(i).get("from_id").toString()+"&fields=photo","response/[0]/smth",logo);
//                                                }else
//                                                    logo.setImageBitmap(getRoundedCornerBitmap(logo_bit, 20));

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
*/
                            try {
                                JSONObject rsp = new JSONObject(response);
                                if(rsp.has("ts"))
                                    start(Integer.parseInt(rsp.get("ts").toString()), context);
                                else
                                    getLongPollServer(context, token, null);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                errorListener.onError(error.toString());
                Toast.makeText(context, "Error longPollServer "+error, Toast.LENGTH_SHORT).show();
                System.out.println(error.getNetworkTimeMs()+" "+error);
                if(error.toString().contains("com.android.volley.NoConnectionError")){
                    try{
                        Thread.sleep(3000);
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                    start(ts, context);
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
    public static void getLongPollServer(Context context, String token, View scroll){
        RequestQueue queue = Volley.newRequestQueue(context);
        String poll ="https://api.vk.com/method/messages.getLongPollServer?access_token="+token+"&v=5.131";

        StringRequest longPoll = new StringRequest(Request.Method.GET, poll,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            if(new JSONObject(response).has("response")) {
                                JSONObject rsp = new JSONObject(response).getJSONObject("response");
                                server = rsp.get("server").toString();
                                key = rsp.get("key").toString();
                                LongPollService.server = server;
                                LongPollService.key = key;
                                LongPollService.start(Integer.parseInt(rsp.get("ts").toString()), context);//
                                if(scroll != null)
                                    LongPollService.addLongPollListener(new LongPollService.longPollService() {
                                    @Override
                                    public void onNewEvent(JSONArray update){
                                        try {
                                            Log.i("long poll", update.toString());

                                            for(int i = 0; i < update.length(); i++) {
                                                if(update.getJSONArray(i).get(0).toString().equals("4")){
                                                    int peer_id = Integer.parseInt(update.getJSONArray(i).get(3).toString());
                                                    String from_id = "";
                                                    if(update.getJSONArray(i).length() >= 7 && update.getJSONArray(i).getJSONObject(6).has("from"))
                                                        from_id = update.getJSONArray(i).getJSONObject(6).get("from").toString();
                                                    System.out.println(peer_id);
                                                    for (int index = 0; index < ((ViewGroup) scroll).getChildCount(); index++) {
                                                        View nextChild = ((ViewGroup) scroll).getChildAt(index);
                                                        if (nextChild.getId() == peer_id) {
                                                            View last_msg_cont = ((ViewGroup) ((ViewGroup) nextChild).getChildAt(1)).getChildAt(1);
                                                            ImageView last_msg_logo = (ImageView) ((ViewGroup) last_msg_cont).getChildAt(0);
                                                            TextView last_msg = (TextView) ((ViewGroup) last_msg_cont).getChildAt(1);
                                                            last_msg.setText(update.getJSONArray(i).get(5).toString());
                                                            if (peer_id < 2000000000){
                                                                if ((Integer.parseInt(update.getJSONArray(i).get(2).toString()) & 2) == 0)
                                                                    last_msg_logo.setImageBitmap(readBitmap(String.valueOf(peer_id), context));
                                                            }else
                                                                last_msg_logo.setImageBitmap(readBitmap(from_id, context));
                                                            if ((Integer.parseInt(update.getJSONArray(i).get(2).toString()) & 2) != 0)
                                                                last_msg_logo.setImageBitmap(readBitmap("216663221", context));
                                                        }
                                                    }
                                                }
                                            }
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                                //longPollServer(Integer.parseInt(rsp.get("ts").toString()), context, activity_name, scroll);//rsp.get("server").toString(),rsp.get("key").toString(),
                            }else{
                                if(response.contains("invalid access_token (4)")){
                                    System.out.println(response);
                                    System.out.println(poll);
                                    Toast.makeText(context, "Authorization failed. Retrying...", Toast.LENGTH_LONG).show();
                                    Thread.sleep(3000);
                                    getLongPollServer(context, token, scroll);
                                }else{
                                    System.out.println(response);
                                    Toast.makeText(context, response, Toast.LENGTH_LONG).show();
                                }

                            }
                        } catch (JSONException | InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(context, "Error getLongPollServer", Toast.LENGTH_SHORT).show();
            }
        });

// Add the request to the RequestQueue.
        queue.add(longPoll);
    }
    public static boolean isActive(){
        return status;
    }
    public static void addLongPollListener(LongPollService.longPollService listener){
        if(mListener == null)
            mListener = new longPollService[1];
        else {
            longPollService[] buff = mListener.clone();
            mListener = new longPollService[mListener.length + 1];
            System.arraycopy(buff, 0, mListener, 0, buff.length);
        }
        mListener[mListener.length-1] = listener;
    }
    public interface longPollService{
        void onNewEvent(JSONArray update);
    }
    public interface longPollServiceError{
        void onError(String error);
    }
    public static void setLongPollListener(LongPollService.longPollService listener){
        notificationListener = listener;
    }
    public static void setErrorListener(LongPollService.longPollServiceError listener){
        errorListener = listener;
    }
}
