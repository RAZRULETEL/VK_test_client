package com.mastik.vk_test_mod.polling;

import android.content.Context;
import android.util.Log;
import android.view.View;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.mastik.vk_test_mod.MainActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import timber.log.Timber;

public class LongPollService {
    private static final String TAG = LongPollService.class.getSimpleName();
    private static final int TIMEOUT_MS = 30000;
    public static String key, server;
    private static longPollService[] mListener = new longPollService[0];
    private static longPollService notificationListener;
    private static longPollServiceError errorListener;
    private static boolean status = false;
    public static int unread_msg_count = 0;

    public static void start(int ts, String token, Context context) {
        status = true;

        String poll = "https://" + server + "?act=a_check&key=" + key + "&ts=" + ts + "&wait=25&mode=2&version=2";

        StringRequest longPoll = new StringRequest(Request.Method.GET, poll,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject rsp = new JSONObject(response);
                            if (rsp.has("failed")) {
                                if (rsp.getInt("failed") == 1)
                                    start(rsp.getInt("ts"), token, context);
                                else
                                    getLongPollServer(context, token, null);
                                return;
                            }
                            for (longPollService li : mListener)
                                li.onNewEvent(rsp.getJSONArray("updates"));
                            if (notificationListener != null)
                                notificationListener.onNewEvent(rsp.getJSONArray("updates"));
                        } catch (JSONException e) {
                            Timber.tag(TAG).e(e, response);
                        }
                        try {
                            JSONObject rsp = new JSONObject(response);
                            if (rsp.has("ts"))
                                start(rsp.getInt("ts"), token, context);
                            else
                                getLongPollServer(context, token, null);
                        } catch (JSONException e) {
                            Timber.tag(TAG).e(e, response);
                            onErrorApi(context, token, -1);
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (errorListener != null)
                    errorListener.onError(error.toString());
                Timber.tag(TAG).e(error);
                onErrorApi(context, token, ts);
            }
        });

// Add the request to the RequestQueue.
        longPoll.setRetryPolicy(new DefaultRetryPolicy(
                TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        MainActivity.getVolleyQueue(context).add(longPoll);
    }

    public static void getLongPollServer(Context context, String token, View scroll) {
        String poll = "https://api.vk.com/method/messages.getLongPollServer?access_token=" + token + "&v=5.131";

        StringRequest longPoll = new StringRequest(Request.Method.GET, poll,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            if (new JSONObject(response).has("response")) {
                                JSONObject rsp = new JSONObject(response).getJSONObject("response");
                                server = rsp.get("server").toString();
                                key = rsp.get("key").toString();
                                LongPollService.start(Integer.parseInt(rsp.get("ts").toString()), token, context);
//                                if (scroll != null)
//                                    LongPollService.addLongPollListener(new LongPollService.longPollService() {
//                                        @Override
//                                        public void onNewEvent(JSONArray update) {
//                                            try {
//                                                Timber.tag(TAG).i(update.toString());
//
//                                                for (int i = 0; i < update.length(); i++) {
//                                                    if (update.getJSONArray(i).get(0).toString().equals("4")) {
//                                                        int peer_id = Integer.parseInt(update.getJSONArray(i).get(3).toString());
//                                                        String from_id = "";
//                                                        if (update.getJSONArray(i).length() >= 7 && update.getJSONArray(i).getJSONObject(6).has("from"))
//                                                            from_id = update.getJSONArray(i).getJSONObject(6).get("from").toString();
//                                                        System.out.println(peer_id);
//                                                        for (int index = 0; index < ((ViewGroup) scroll).getChildCount(); index++) {
//                                                            View nextChild = ((ViewGroup) scroll).getChildAt(index);
//                                                            if (nextChild.getId() == peer_id) {
//                                                                View last_msg_cont = ((ViewGroup) ((ViewGroup) nextChild).getChildAt(1)).getChildAt(1);
//                                                                ImageView last_msg_logo = (ImageView) ((ViewGroup) last_msg_cont).getChildAt(0);
//                                                                TextView last_msg = (TextView) ((ViewGroup) last_msg_cont).getChildAt(1);
//                                                                last_msg.setText(update.getJSONArray(i).get(5).toString());
//                                                                if (peer_id < 2_000_000_000) {
//                                                                    if ((Integer.parseInt(update.getJSONArray(i).get(2).toString()) & 2) == 0)
//                                                                        last_msg_logo.setImageBitmap(readBitmap(String.valueOf(peer_id), context));
//                                                                } else
//                                                                    last_msg_logo.setImageBitmap(readBitmap(from_id, context));
//                                                                if ((Integer.parseInt(update.getJSONArray(i).get(2).toString()) & 2) != 0)
//                                                                    last_msg_logo.setImageBitmap(readBitmap(MainActivity.getCurrentUserId(), context));
//                                                            }
//                                                        }
//                                                    }
//                                                }
//                                            } catch (JSONException e) {
//                                                e.printStackTrace();
//                                            }
//                                        }
//                                    });
                                //longPollServer(Integer.parseInt(rsp.get("ts").toString()), context, activity_name, scroll);//rsp.get("server").toString(),rsp.get("key").toString(),
                            } else {
//                                if (response.contains("invalid access_token (4)"))
                                Timber.tag(TAG).e(response);
                                onErrorApi(context, token, -1);
                            }
                        } catch (JSONException e) {
                            Timber.tag(TAG).e(e);
                            onErrorApi(context, token, -1);
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                onErrorApi(context, token, -1);
            }
        });

// Add the request to the RequestQueue.
        MainActivity.getVolleyQueue(context).add(longPoll);
    }

    private static void onErrorApi(Context context, String token, int ts) {
        MainActivity.BACKGROUND_THREADS.execute(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Timber.tag(TAG).e(e);
            }
            if (ts > 0)
                start(ts, token, context);
            else
                getLongPollServer(context, token, null);
        });
    }

    public static boolean isActive() {
        return status;
    }

    public static void addLongPollListener(LongPollService.longPollService listener) {
        if (mListener == null)
            mListener = new longPollService[1];
        else {
            longPollService[] buff = mListener.clone();
            mListener = new longPollService[mListener.length + 1];
            System.arraycopy(buff, 0, mListener, 0, buff.length);
        }
        mListener[mListener.length - 1] = listener;
    }

    public interface longPollService {
        void onNewEvent(JSONArray update);
    }

    public interface longPollServiceError {
        void onError(String error);
    }

    public static void setLongPollListener(LongPollService.longPollService listener) {
        notificationListener = listener;
    }

    public static void setErrorListener(LongPollService.longPollServiceError listener) {
        errorListener = listener;
    }
}
