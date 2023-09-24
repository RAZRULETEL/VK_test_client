package com.mastik.vk_test_mod;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.mastik.vk_test_mod.dataTypes.VKDialog;
import com.mastik.vk_test_mod.dataTypes.VKImage;
import com.mastik.vk_test_mod.dataTypes.VKMessage;
import com.mastik.vk_test_mod.dataTypes.VKUser;
import com.mastik.vk_test_mod.dataTypes.attachments.MessageAttachment;
import com.mastik.vk_test_mod.dataTypes.attachments.Photo;
import com.mastik.vk_test_mod.dataTypes.attachments.PhotoSize;
import com.mastik.vk_test_mod.dataTypes.attachments.Sticker;
import com.mastik.vk_test_mod.dataTypes.attachments.Video;
import com.mastik.vk_test_mod.polling.LongPollService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

public class MessagesActivity extends AppCompatActivity {
    private static final SimpleDateFormat LAST_ONLINE_PATTERN = new SimpleDateFormat("dd.MM.yyyy,HH:mm", Locale.getDefault()),
            SAME_YEAR_LAST_ONLINE_PATTERN = new SimpleDateFormat("dd.MM,HH:mm", Locale.getDefault()),
            SAME_DAY_LAST_ONLINE_PATTERN = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private static final String TAG = MessagesActivity.class.getSimpleName();
    private int id, unread_msgs, last_conversation_id;
    private LinearLayout scroll;
    private boolean history_loaded = false;
    private VKDialog dialog;

    @Override
    public void onBackPressed() {
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
        dialog = VKDialog.getById(id);
        unread_msgs = dialog.getUnreadCount();
        scroll = findViewById(R.id.scroll);
        scroll.setId(id);

        ImageView chat_logo = findViewById(R.id.chatLogo);
        chat_logo.post(() -> chat_logo.setImageBitmap(dialog.getLogo()));
        dialog.addLogoChangeListener(bit -> chat_logo.post(() -> chat_logo.setImageBitmap(bit)));
        TextView chat_name = findViewById(R.id.chatName);
        chat_name.setText(dialog.getTitle());
        TextView chat_info = findViewById(R.id.chatInfo);
        chat_info.setText(null);
        if (dialog.isChat() || dialog.isGroupDialog()) {
            chat_info.setText(dialog.getOnlineUsersCount());
            chat_info.append("/" + dialog.getTotalUsersCount() + " | ");
        }
        if (dialog.isUserDialog()) {
            Calendar timestamp = Calendar.getInstance();
            timestamp.setTimeInMillis(VKUser.getById(id, getApplicationContext()).getLastSeenDate().toEpochMilli());
            if (timestamp.toInstant().plusSeconds(5 * 60).isBefore(Instant.now())) {
                Calendar now = Calendar.getInstance(Locale.getDefault());

                if (now.get(Calendar.YEAR) == timestamp.get(Calendar.YEAR)) {
                    if (now.get(Calendar.DAY_OF_YEAR) == timestamp.get(Calendar.DAY_OF_YEAR)) {
                        String dateString = SAME_DAY_LAST_ONLINE_PATTERN.format(timestamp.getTime());
                        chat_info.setText(dateString + " | ");
                    } else {
                        String dateString = SAME_YEAR_LAST_ONLINE_PATTERN.format(timestamp.getTime());
                        chat_info.setText(dateString + " | ");
                    }
                } else {
                    if(timestamp.get(Calendar.YEAR) != 1970) {
                        String dateString = LAST_ONLINE_PATTERN.format(timestamp.getTime());
                        chat_info.setText(dateString + " | ");
                    }
                }
            } else {
                chat_info.setText("онлайн");
                String dateString = SAME_DAY_LAST_ONLINE_PATTERN.format(timestamp);
                chat_info.append("(" + dateString + ") | ");
            }


        }

        String url;
        if (dialog.isUserDialog())
            url = "https://api.vk.com/method/messages.getHistory?peer_id=" + id + "&access_token=" + MainActivity.getToken() + "&v=5.131";
        else
            url = "https://api.vk.com/method/messages.getHistory?peer_id=" + id + "&access_token=" + MainActivity.getToken() + "&extended=1&count=30&v=5.131";

// Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            chat_info.append(new JSONObject(response).getJSONObject("response").get("count") + "✉");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        MainActivity.BACKGROUND_THREADS.execute(() -> {
                            try {
                                JSONArray lastMessages = new JSONObject(response).getJSONObject("response").getJSONArray("items");
                                if (new JSONObject(response).getJSONObject("response").has("profiles")) {
                                    JSONArray profiles = new JSONObject(response).getJSONObject("response").getJSONArray("profiles");
                                    MainActivity.BACKGROUND_THREADS.execute(() -> VKUser.saveUsersFromJSON(profiles, getApplicationContext()));
                                }
                                for (int i = 0; i < lastMessages.length(); i++) {
                                    try {
                                        VKMessage vkMessage = VKMessage.getFromJSON(lastMessages.getJSONObject(i), dialog).save(getApplicationContext());
                                        MainActivity.BACKGROUND_THREADS.execute(() -> addMessage(scroll, vkMessage, false));
                                    } catch (JSONException ex) {
                                        Timber.tag(TAG).e(ex, lastMessages.getJSONObject(i).toString());
                                    }
                                }
                                last_conversation_id = lastMessages.getJSONObject(lastMessages.length() - 1).getInt("conversation_message_id");
                            } catch (JSONException e) {
                                Timber.tag(TAG).e(e, response);
                                e.printStackTrace();
                            }
                        });
                        ViewTreeObserver vto = scroll.getViewTreeObserver();
                        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                //scroll.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                LinearLayout background = findViewById(R.id.main_back);
                                double height = scroll.getMeasuredHeight() - background.getHeight();
                                if (background.getHeight() < 1800)
                                    height = 1800;

                                MovingRgbGradient.init(getApplicationContext());
                                int[] colors = MovingRgbGradient.generateColors(MovingRgbGradient.getFrequency(), MovingRgbGradient.getScale());
                                for (int i = 0; i < Math.ceil(height / 400); i++) {
                                    TextView back_cell = new TextView(getApplicationContext());
                                    back_cell.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, 400, 1));
                                    GradientDrawable cont_gradient = new GradientDrawable();
                                    cont_gradient.setColors(colors);
                                    colors = MovingRgbGradient.generateNextColors(colors, MovingRgbGradient.getScale());
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
//                Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_SHORT).show();
//                String[] messages = readSourceData("chat_" + id).split("\n");
//                last_conversation_id = 0;
//                for (String message : messages) {
//                    JSONObject this_msg = null;
//                    try {
//                        this_msg = new JSONObject(message);
//                        if (this_msg.getInt("conversation_message_id") > last_conversation_id)
//                            last_conversation_id = this_msg.getInt("conversation_message_id");
//                        addMessage(scroll, this_msg, false);
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                }
                ViewTreeObserver vto = scroll.getViewTreeObserver();
                vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        scroll.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                        double height = scroll.getMeasuredHeight();

                        LinearLayout background = findViewById(R.id.main_back);
                        MovingRgbGradient.init(getApplicationContext());
                        int[] colors = MovingRgbGradient.generateColors(MovingRgbGradient.getFrequency(), MovingRgbGradient.getScale());
                        for (int i = 0; i < Math.ceil(height / 400); i++) {
                            TextView back_cell = new TextView(getApplicationContext());
                            back_cell.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, 400, 1));
                            GradientDrawable cont_gradient = new GradientDrawable();
                            cont_gradient.setColors(colors);
                            colors = MovingRgbGradient.generateNextColors(colors, MovingRgbGradient.getScale());
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
        MainActivity.getVolleyQueue(getApplicationContext()).add(stringRequest);

        GradientDrawable nice = new GradientDrawable();
        nice.setColors(MovingRgbGradient.generateColors(5));

        LongPollService.addLongPollListener(new LongPollService.longPollService() {
            @Override
            public void onNewEvent(JSONArray update) {
                try {
                    for (int i = 0; i < update.length(); i++) {
                        if (update.getJSONArray(i).get(0).toString().equals("4")) {
                            int peer_id = Integer.parseInt(update.getJSONArray(i).get(3).toString());
                            if (peer_id != id)
                                continue;
                            String from_id;
                            if (update.getJSONArray(i).length() >= 7 && update.getJSONArray(i).getJSONObject(6).has("from"))
                                from_id = update.getJSONArray(i).getJSONObject(6).get("from").toString();
                            else if ((Integer.parseInt(update.getJSONArray(i).get(2).toString()) & 2) == 0)
                                from_id = update.getJSONArray(i).get(3).toString();
                            else
                                from_id = "53464";
                            JSONObject message = new JSONObject();
                            message.put("date", update.getJSONArray(i).get(4).toString());
                            message.put("from_id", from_id);
                            message.put("id", update.getJSONArray(i).get(1).toString());
                            if ((Integer.parseInt(update.getJSONArray(i).get(2).toString()) & 2) == 0)
                                message.put("out", "0");
                            else
                                message.put("out", "1");
                            message.put("attachments", new JSONArray());
                            message.put("conversation_message_id", ++last_conversation_id);
                            message.put("text", update.getJSONArray(i).get(5).toString());


                            message.put("peer_id", peer_id);
                            if (history_loaded)
                                addMessage(scroll, message, true);
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        });
    }

    public int convertDpToPixel(float dp) {
        return MainActivity.convertDpToPixel(dp, getApplicationContext());
    }

    @SuppressLint("RtlHardcoded")
    private void addMessage(LinearLayout msgContainer, VKMessage message, boolean to_end) {
//        Timber.tag(TAG).d(message.toString());
        LinearLayout main = new LinearLayout(getApplicationContext());
        main.setOrientation(LinearLayout.HORIZONTAL);
        main.setPadding(convertDpToPixel(5), convertDpToPixel(6), convertDpToPixel(13), convertDpToPixel(6));
        LinearLayout.LayoutParams msg_params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        if (!msgContainer.equals(scroll)) {
            msg_params.width = WRAP_CONTENT;
            msg_params.weight = 1;
        }
        msg_params.setMargins(0, convertDpToPixel(5), 0, convertDpToPixel(5));
        main.setLayoutParams(msg_params);
        if (msgContainer == scroll && unread_msgs++ < 0) {
            main.setBackgroundColor(MainActivity.UNREAD_MESSAGE_COLOR);
        }


        LinearLayout msg_content = new LinearLayout(getApplicationContext());
        msg_content.setOrientation(LinearLayout.VERTICAL);
        msg_content.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, 1));


        TextView msgText = new TextView(getApplicationContext());
        msgText.setId(View.generateViewId());
        msgText.setTextSize(15);
        msgText.setText(message.getText());

        LinearLayout.LayoutParams txt = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        txt.setMargins(convertDpToPixel(10), 0, convertDpToPixel(10), 0);
        msgText.setLayoutParams(txt);


        msg_content.addView(msgText);
        if (message.isReply() || message.getForwardedMessages() != null && message.getForwardedMessages().size() > 0) {
            msg_content.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, 1));
            GradientDrawable reply = new GradientDrawable();
            reply.setColor(Color.LTGRAY);
            reply.setCornerRadius(VKImage.getCornerRadius());
            msg_content.setBackground(reply);
            msg_content.setId(View.generateViewId());

            if (message.isReply()) {
                VKMessage replyMsg = message.getReplyMessage();
                try {
                    addMessage(msg_content, replyMsg, false);
                } catch (NullPointerException e) {
                    Timber.tag(TAG).e(e, message.toString());
                }
            }
            if (message.getForwardedMessages() != null && message.getForwardedMessages().size() > 0) {
                for (VKMessage forwardedMsg : message.getForwardedMessages()) {
                    addMessage(msg_content, forwardedMsg, false);
                }
            }
        }


        LinearLayout msg_layout = new LinearLayout(getApplicationContext());
        msg_layout.setId(View.generateViewId());


        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm", Locale.ROOT);
        String dateString = formatter.format(new Date(message.getTimestamp().toEpochMilli()));

        TextView time = new TextView(getApplicationContext());
        time.setId(View.generateViewId());
        time.setTextSize(15);
        time.setText(dateString);
        time.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams time_ls = new LinearLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT);
        time.setLayoutParams(time_ls);


        ImageView logo = new ImageView(getApplicationContext());
        if (!message.isOutgoing()) {
//            try {
            VKUser vkUser = VKUser.getById(message.getUserId(), getApplicationContext());
            if (vkUser.getAvatarUrl() == null)
                logo.post(() -> logo.setImageBitmap(VKImage.getDefault(getApplicationContext()).getImg()));
            else
                VKImage.get(vkUser.getAvatarUrl(), message.getUserId(), VKUser.PREFIX, getApplicationContext())
                        .init(getApplicationContext())
                        .addOnInitListener(img -> logo.post(() -> logo.setImageBitmap(img.getImg())));
//            } catch (IOException e) {
//                String url = "https://api.vk.com/method/" + (message_object.getUserId() < 0 ? VKImage.ImageOwner.GROUP.signature : VKImage.ImageOwner.USER.signature) + "&access_token=" + MainActivity.getToken() + "&v=5.131";
//                StringRequest imgGet = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
//                    @Override
//                    public void onResponse(String response) {
//                        try {
//                            JSONObject res = new JSONObject(response).getJSONArray("response").getJSONObject(0);
//                            String imgUrl = res.has("photo_50") ? res.getString("photo_50") : res.getString("photo").toString();
//                            VKImage.get(imgUrl, message_object.getUserId(), getApplicationContext()).init(getApplicationContext()).addOnInitListener(vkImg -> logo.post(() -> logo.setImageBitmap(vkImg.getImg())));
//                        } catch (JSONException ex) {
//                            logo.post(() -> logo.setImageBitmap(VKImage.getDefault(getApplicationContext()).getImg()));
//                        }
//                    }
//                }, new Response.ErrorListener() {
//                    @Override
//                    public void onErrorResponse(VolleyError error) {
//                        logo.post(() -> logo.setImageBitmap(VKImage.getDefault(getApplicationContext()).getImg()));
//                    }
//                });
//                queue.add(imgGet);
//            }
        }
        if (!message.isOutgoing()) {
            txt.weight = 1;
            msg_layout.addView(msg_content);
        } else {
            msg_layout.setHorizontalGravity(Gravity.RIGHT);
            msg_content.setHorizontalGravity(Gravity.CENTER);

            msg_content.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
            msg_layout.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

            msg_layout.addView(time);

        }
        if (!message.isOutgoing())
            main.addView(logo);
        main.addView(msg_layout);


        GradientDrawable msg_style = new GradientDrawable();
        msg_style.setCornerRadius(10);
        msg_style.setColor(Color.parseColor("#99000000"));//#FFD6D6D6
        msgText.setTextColor(Color.WHITE);
        if (!msgText.getText().equals("")) {
            msgText.setBackground(msg_style);
            msgText.setPadding(convertDpToPixel(5), convertDpToPixel(3), convertDpToPixel(5), convertDpToPixel(3));
        } else {
            if (message.hasAction()) {
                msg_style.setColor(Color.GRAY);
                msgText.setBackground(msg_style);
                msgText.setTextColor(getResources().getColor(R.color.test, null));
                msgText.setPadding(convertDpToPixel(5), convertDpToPixel(3), convertDpToPixel(5), convertDpToPixel(3));

                VKUser profile = VKUser.getById(message.getMemberId(), getApplicationContext());
                profile.setOnInitListener(user -> {
                    msgText.setText(user.getFullName());
                    msgText.append(" " + MainActivity.getAction(message.getAction().toString()));
//                        if (message_object.getAction().toString().contains("title")) //TODO: add new chat name out
//                            newMsg.append(message_object.getJSONObject("action").get("text").toString());

                });
            }
        }
        if (!message.isOutgoing()) {
            logo.getLayoutParams().width = convertDpToPixel(35);
            logo.getLayoutParams().height = convertDpToPixel(35);
            LinearLayout.LayoutParams logo_params = (LinearLayout.LayoutParams) logo.getLayoutParams();
            logo_params.gravity = Gravity.TOP;
            runOnUiThread(() -> logo.setLayoutParams(logo_params));
        }

        if (message.getAttachments() != null && message.getAttachments().size() > 0) {
            GradientDrawable attach_style = new GradientDrawable();
            attach_style.setCornerRadius(10);
            attach_style.setColor(Color.WHITE);
            LinearLayout.LayoutParams margins = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            margins.setMargins(convertDpToPixel(10), 0, convertDpToPixel(10), 0);
            for (MessageAttachment attachment : message.getAttachments()) {
                switch (attachment.getDBPrefix()) {
                    case Photo.DB_PREFIX -> {
                        ImageView img_attachment = new ImageView(getApplicationContext());
                        Photo photo_attach = (Photo) attachment;
                        LinearLayout.LayoutParams imgV = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, 1);

                        img_attachment.setLayoutParams(imgV);
                        img_attachment.setImageBitmap(VKImage.getDefault(getApplicationContext()).getImg());
                        img_attachment.setMinimumHeight(photo_attach.getHeight(PhotoSize.x));
                        img_attachment.setMinimumWidth(photo_attach.getWidth(PhotoSize.x));
                        VKImage vkImage = VKImage.get(photo_attach.getUrl(PhotoSize.x), photo_attach.getId(), getApplicationContext()).init(getApplicationContext());
                        vkImage.addOnInitListener(img1 -> runOnUiThread(() -> img_attachment.setImageBitmap(img1.getImg())));
                        photo_attach.setCachedSize(PhotoSize.x, vkImage.getSavePath());

                        img_attachment.setId(View.generateViewId());
                        img_attachment.setBackground(attach_style);
                        img_attachment.setPadding(convertDpToPixel(5), convertDpToPixel(5), convertDpToPixel(5), convertDpToPixel(5));
                        img_attachment.setLayoutParams(margins);
                        img_attachment.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent intent = new Intent(MessagesActivity.this, FullscreenImage.class);
                                intent.putExtra("id", photo_attach.getId());
                                startActivity(intent);
                            }
                        });


                        msg_content.addView(img_attachment);
                    }
                    case Sticker.DB_PREFIX -> {
                        Sticker sticker = (Sticker) attachment;
                        ImageView sticker_img = new ImageView(getApplicationContext());
                        String sticker_url = sticker.getUrl(Arrays.stream(sticker.getSizes()).max().getAsInt());//TODO: move size choose to settings
                        LinearLayout.LayoutParams stick = new LinearLayout.LayoutParams(convertDpToPixel(192), convertDpToPixel(192));
                        sticker_img.setLayoutParams(stick);
                        VKImage.get(sticker_url, sticker.getId(), Sticker.DB_PREFIX, getApplicationContext())
                                .init(getApplicationContext())
                                .addOnInitListener(vkSticker -> runOnUiThread(() -> sticker_img.setImageBitmap(vkSticker.getImg())));
                        sticker_img.setId(View.generateViewId());

                        msg_content.addView(sticker_img);
                    }
                    case "video" -> {

                        Video video = (Video) attachment;

                        ImageView video_preview = new ImageView(getApplicationContext());
                        VKImage.get(video.getPreviewImage().getUrl(video.getPreviewImage().getMaxAvailableSize()), video.getId(), Video.DB_PREFIX, getApplicationContext())
                                .init(getApplicationContext())
                                .addOnInitListener(img -> video_preview.post(() -> video_preview.setImageBitmap(img.getImg())));

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
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                if (!video.getLinks().containsKey("external")) {
                                    String[] keys = video.getLinks().keySet().stream().filter(key -> !key.contains("dash") && !key.contains("failover")).sorted(Video.QUALITY_COMPARATOR).toArray(String[]::new);
                                    new AlertDialog.Builder(MessagesActivity.this)
                                            .setItems(keys, (dialog, chosenIndex) -> {
                                                intent.setDataAndType(Uri.parse(video.getLinks().get(keys[chosenIndex])), "video/*");
                                                startActivity(intent);
                                            })
                                            .show();
                                } else {
                                    intent.setData(Uri.parse(video.getLinks().get("external")));
                                    startActivity(intent);
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
//                        video_play.setRotation(-90);
                        Runnable rotate = new Runnable() {
                            @Override
                            public void run() {
                                video_play.animate().rotation(video_play.getRotation() + 360).setDuration(1000).withEndAction(this).start();
                            }
                        };
                        runOnUiThread(() -> video_play.animate().rotation(360 - 90).setDuration(1000).withEndAction(rotate).start());
                        video_play.setScaleX(2);
                        video_play.setScaleY(2);
                        video_frame.addView(video_play);

                        msg_content.addView(video_frame);
                        break;
                    }
                }
            }
        }


//        int[] attachments_ids = new int[message_object.getJSONArray("attachments").length()];
//
//        for (int l = 0; l < message_object.getJSONArray("attachments").length(); l++) {
//            switch (message_object.getJSONArray("attachments").getJSONObject(l).get("type").toString()) {
//                case "group_call_in_progress":
//                    msg_style.setColor(Color.GRAY);
//                    newMsg.setBackground(msg_style);
//                    newMsg.setTextColor(getResources().getColor(R.color.test, null));
//                    newMsg.setPadding(convertDpToPixel(5), convertDpToPixel(3), convertDpToPixel(5), convertDpToPixel(3));
//
//                    JSONObject group_call = message_object.getJSONArray("attachments").getJSONObject(l);
//                    newMsg.setText("Групповой звонок в прогрессе \n Участников: " + group_call.getJSONObject("group_call_in_progress").getJSONObject("participants").get("count"));
//                    break;
//                case "doc":
//                    JSONObject doc = message_object.getJSONArray("attachments").getJSONObject(l).getJSONObject("doc");
//                    //.setPadding(convertDpToPixel(5), convertDpToPixel(5), convertDpToPixel(5), convertDpToPixel(5));
//                    LinearLayout doc_main = new LinearLayout(getApplicationContext());
//                    doc_main.setOrientation(LinearLayout.HORIZONTAL);
//
//                    ImageView icon = new ImageView(getApplicationContext());
//                    icon.setImageDrawable(AppCompatResources.getDrawable(getApplicationContext(), R.drawable.document));
//                    icon.setLayoutParams(new LinearLayout.LayoutParams(50, 50));
//                    icon.setScaleType(ImageView.ScaleType.FIT_XY);
//                    doc_main.addView(icon);
//
//                    TextView doc_name = new TextView(getApplicationContext());
//                    doc_name.setText(doc.getString("title"));
//                    doc_main.addView(doc_name);
//
//                    doc_main.setTag(doc.getString("url"));
//
//                    doc_main.setOnClickListener(new View.OnClickListener() {
//                        @Override
//                        public void onClick(View v) {
//                            Intent intent = new Intent();
//
//                        }
//                    });
//
//
//                    break;
//            }
//        }

        if (!message.isOutgoing()) {
            msg_layout.addView(time);
        } else {
            msg_layout.addView(msg_content);
        }

        runOnUiThread(() -> {
            if (to_end)
                msgContainer.addView(main, 0);
            else
                msgContainer.addView(main);
        });
    }


    @Deprecated
    private void addMessage(LinearLayout msgContainer, JSONObject message_object, boolean to_end) throws JSONException {
        LinearLayout message = new LinearLayout(getApplicationContext());
        message.setOrientation(LinearLayout.HORIZONTAL);
        message.setPadding(convertDpToPixel(5), convertDpToPixel(6), convertDpToPixel(13), convertDpToPixel(6));
        LinearLayout.LayoutParams msg_params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        if (msgContainer != scroll) {
            msg_params.width = WRAP_CONTENT;
            msg_params.weight = 1;
        }
        msg_params.setMargins(0, convertDpToPixel(5), 0, convertDpToPixel(5));
        message.setLayoutParams(msg_params);
        if (msgContainer == scroll && unread_msgs++ < 0) {
            message.setBackgroundColor(MainActivity.UNREAD_MESSAGE_COLOR);
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
        if (message_object.has("reply_message")) {
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
        String dateString = formatter.format(new Date(Long.parseLong(message_object.get("date").toString()) * 1000));

        TextView time = new TextView(getApplicationContext());
        time.setId(View.generateViewId());
        time.setTextSize(15);
        time.setText(dateString);
        time.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams time_ls = new LinearLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT);
        time.setLayoutParams(time_ls);


        ImageView logo = new ImageView(getApplicationContext());
        if (message_object.get("out").toString().equals("0")) {
//            try {
//                logo.setImageBitmap(VKImage.get(null, message_object.getInt("from_id"), getApplicationContext()).syncInit().getImg());
//            } catch (IOException e) {
//                String url = "https://api.vk.com/method/" + (Integer.parseInt(message_object.get("from_id").toString()) < 0 ? VKImage.ImageOwner.GROUP.signature : VKImage.ImageOwner.USER.signature) + "&access_token=" + MainActivity.getToken() + "&v=5.131";
//                StringRequest imgGet = new StringRequest(Request.Method.GET, url, new Response.Listener<>() {
//                    @Override
//                    public void onResponse(String response) {
//                        try {
//                            JSONObject res = new JSONObject(response).getJSONArray("response").getJSONObject(0);
//                            String imgUrl = res.has("photo_50") ? res.getString("photo_50") : res.getString("photo");
//                            VKImage.get(imgUrl, message_object.getInt("from_id"), getApplicationContext()).init(getApplicationContext()).addOnInitListener(vkImg -> logo.post(() -> logo.setImageBitmap(vkImg.getImg())));
//                        } catch (JSONException ex) {
//                            logo.post(() -> logo.setImageBitmap(VKImage.getDefault(getApplicationContext()).getImg()));
//                        }
//                    }
//                }, new Response.ErrorListener() {
//                    @Override
//                    public void onErrorResponse(VolleyError error) {
//                        logo.post(() -> logo.setImageBitmap(VKImage.getDefault(getApplicationContext()).getImg()));
//                    }
//                });
//                queue.add(imgGet);
//            }
        }


        if (message_object.get("out").toString().equals("0")) {
            txt.weight = 1;
            msg_layout.addView(msg_content);
        } else {
            msg_layout.setHorizontalGravity(Gravity.RIGHT);
            msg_content.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
            msg_layout.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
            msg_layout.addView(time);

        }
        if (message_object.get("out").toString().equals("0"))
            message.addView(logo);
        message.addView(msg_layout);


        GradientDrawable msg_style = new GradientDrawable();
        msg_style.setCornerRadius(10);
        msg_style.setColor(Color.parseColor("#ffffff"));//#FFD6D6D6
        newMsg.setTextColor(Color.BLACK);
        if (!newMsg.getText().equals("")) {
            newMsg.setBackground(msg_style);
            newMsg.setPadding(convertDpToPixel(5), convertDpToPixel(3), convertDpToPixel(5), convertDpToPixel(3));
        } else {
            if (message_object.has("action")) {
                msg_style.setColor(Color.GRAY);
                newMsg.setBackground(msg_style);
                newMsg.setTextColor(getResources().getColor(R.color.test, null));
                newMsg.setPadding(convertDpToPixel(5), convertDpToPixel(3), convertDpToPixel(5), convertDpToPixel(3));

                VKUser profile = VKUser.getById(message_object.getJSONObject("action").getInt("member_id"), getApplicationContext());
                profile.setOnInitListener(user -> {
                    newMsg.setText(user.getFullName());
                    try {
                        newMsg.append(" " + MainActivity.getAction(message_object.getJSONObject("action").get("type").toString()));
                        if (message_object.getJSONObject("action").get("type").toString().contains("title"))
                            newMsg.append(message_object.getJSONObject("action").get("text").toString());
                    } catch (JSONException e) {
                        Timber.tag(TAG).e(e, "get action error");
                    }
                });
            } else if (!message_object.getJSONArray("attachments").toString().contains("group_call_in_progress")) {
                txt = new LinearLayout.LayoutParams(0, 0);
                newMsg.setLayoutParams(txt);
            }
        }
        runOnUiThread(() -> {
            if (to_end)
                msgContainer.addView(message, 0);
            else
                msgContainer.addView(message);
        });


        if (message_object.get("out").toString().equals("0")) {
            logo.getLayoutParams().width = convertDpToPixel(35);
            logo.getLayoutParams().height = convertDpToPixel(35);
            LinearLayout.LayoutParams logo_params = (LinearLayout.LayoutParams) logo.getLayoutParams();
            logo_params.gravity = Gravity.TOP;
            runOnUiThread(() -> logo.setLayoutParams(logo_params));
        }
//
//        int[] attachments_ids = new int[message_object.getJSONArray("attachments").length()];
//        GradientDrawable attach_style = new GradientDrawable();
//        attach_style.setCornerRadius(10);
//        attach_style.setColor(Color.WHITE);
//        LinearLayout.LayoutParams margins = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
//        margins.setMargins(convertDpToPixel(10), 0, convertDpToPixel(10), 0);
//        for (int l = 0; l < message_object.getJSONArray("attachments").length(); l++) {
//            switch (message_object.getJSONArray("attachments").getJSONObject(l).get("type").toString()) {
//                case "photo":
//                    ImageView img_attachment = new ImageView(getApplicationContext());
//                    msg_content.addView(img_attachment);
//                    Photo photo_attach = Photo.getFromJSON(message_object.getJSONArray("attachments").getJSONObject(l).getJSONObject("photo"));
//                    LinearLayout.LayoutParams imgV = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, 1);
//
//                    img_attachment.setLayoutParams(imgV);
//                    img_attachment.setImageBitmap(VKImage.getDefault(getApplicationContext()).getImg());
//                    img_attachment.setMinimumHeight(photo_attach.getHeight(PhotoSize.x));
//                    img_attachment.setMinimumWidth(photo_attach.getWidth(PhotoSize.x));
//                    VKImage.get(photo_attach.getUrl(PhotoSize.x), photo_attach.getId(), getApplicationContext()).init(getApplicationContext()).addOnInitListener(img1 -> runOnUiThread(() -> img_attachment.setImageBitmap(img1.getImg())));
//
//                    runOnUiThread(() -> {
//                        img_attachment.setId(View.generateViewId());
//                        img_attachment.setBackground(attach_style);
//                        img_attachment.setPadding(convertDpToPixel(5), convertDpToPixel(5), convertDpToPixel(5), convertDpToPixel(5));
//                        img_attachment.setLayoutParams(margins);
//                        img_attachment.setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View view) {
//                                Intent intent = new Intent(MessagesActivity.this, FullscreenImage.class);
//                                intent.putExtra("img", photo_attach.getId());
//
//                                startActivity(intent);
//                            }
//                        });
//                    });
//                    attachments_ids[l] = img_attachment.getId();
//                    break;
//                case "sticker":
//                    JSONObject sticker = message_object.getJSONArray("attachments").getJSONObject(l).getJSONObject("sticker");
//                    ImageView sticker_img = new ImageView(getApplicationContext());
//                    msg_content.addView(sticker_img);
//                    String sticker_url = sticker.getJSONArray("images").getJSONObject(1).get("url").toString();
//                    LinearLayout.LayoutParams stick = new LinearLayout.LayoutParams(convertDpToPixel(192), convertDpToPixel(192));
//                    sticker_img.setLayoutParams(stick);
//                    VKImage.get(sticker_url, sticker.getInt("sticker_id"), "stick_", getApplicationContext()).init(getApplicationContext())
//                            .addOnInitListener(VKsticker -> runOnUiThread(() -> sticker_img.setImageBitmap(VKsticker.getImg())));
//
//                    sticker_img.setId(View.generateViewId());
//                    attachments_ids[l] = sticker_img.getId();
//                    break;
//                case "group_call_in_progress":
//                    msg_style.setColor(Color.GRAY);
//                    newMsg.setBackground(msg_style);
//                    newMsg.setTextColor(getResources().getColor(R.color.test, null));
//                    newMsg.setPadding(convertDpToPixel(5), convertDpToPixel(3), convertDpToPixel(5), convertDpToPixel(3));
//
//                    JSONObject group_call = message_object.getJSONArray("attachments").getJSONObject(l);
//                    newMsg.setText("Групповой звонок в прогрессе \n Участников: " + group_call.getJSONObject("group_call_in_progress").getJSONObject("participants").get("count"));
//                    break;
//                case "video":
//                    JSONObject video = message_object.getJSONArray("attachments").getJSONObject(l).getJSONObject("video");
//                    ImageView video_preview = new ImageView(getApplicationContext());
//
//                    JSONObject preview_obj = video.getJSONArray("image").getJSONObject(1);
//                    String preview_url = preview_obj.get("url").toString();
//
//                    VKImage preview_bit = null;
//                    try {
//                        preview_bit = VKImage.get(preview_url, video.getInt("id"), "video_", getApplicationContext()).syncInit();
//                    } catch (IOException e) {
//                        preview_bit = VKImage.getDefault(getApplicationContext());
//                        e.printStackTrace();
//                    }
//                    video_preview.setImageBitmap(preview_bit.getImg());
//
//                    GradientDrawable vid_style = new GradientDrawable();
//                    vid_style.setCornerRadius(10);
//                    vid_style.setColor(Color.WHITE);
//
//                    FrameLayout video_frame = new FrameLayout(getApplicationContext());
//                    video_frame.setId(View.generateViewId());
//                    video_frame.addView(video_preview);
//                    video_frame.setLayoutParams(new FrameLayout.LayoutParams(convertDpToPixel(300), convertDpToPixel(169)));
//                    video_frame.setBackground(vid_style);
//                    video_frame.setOnClickListener(new View.OnClickListener() {
//                        @Override
//                        public void onClick(View view) {
//                            String videourl;
//                            Intent intent = new Intent(Intent.ACTION_VIEW);
//                            try {
//                                if (!video.getJSONObject("files").has("external")) {
//                                    //FrameLayout main = findViewById(R.id.main_frame);
//                                    LinearLayout video_quality = new LinearLayout(getApplicationContext());
//                                    video_quality.setOrientation(LinearLayout.VERTICAL);
//                                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(getApplicationContext().getResources().getDisplayMetrics().widthPixels / 2, WRAP_CONTENT);
//                                    params.gravity = Gravity.CENTER;
//                                    video_quality.setLayoutParams(params);
//                                    video_quality.setBackgroundColor(Color.YELLOW);
//                                    video_quality.setTranslationZ(10);
//                                    Iterator<String> keys = video.getJSONObject("files").keys();
//                                    video_quality.setGravity(Gravity.CENTER);
//                                    while (keys.hasNext()) {
//                                        String key = keys.next();
//                                        if (key.contains("dash") || key.contains("failover"))
//                                            continue;
//                                        TextView variant = new TextView(getApplicationContext());
//                                        variant.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
//                                        variant.setText(key);
//                                        variant.setGravity(Gravity.CENTER);
//                                        video_quality.addView(variant);
//                                        variant.setClickable(true);
//                                        variant.setTextSize(30);
//                                        variant.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, (int) variant.getTextSize() * 2, 1));
//                                        variant.setOnClickListener(new View.OnClickListener() {
//                                            @Override
//                                            public void onClick(View v) {
//                                                try {
//                                                    String videourl = video.getJSONObject("files").get(((TextView) v).getText().toString()).toString();
//                                                    intent.setDataAndType(Uri.parse(videourl), "video/*");
//                                                    startActivity(intent);
//                                                } catch (JSONException e) {
//                                                    e.printStackTrace();
//                                                    Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_SHORT).show();
//
//                                                }
//
//                                            }
//                                        });
//                                    }
//                                    //main.addView(video_quality);
//                                    Log.e("height", video_quality.getChildAt(0).getHeight() + "");
//                                    new AlertDialog.Builder(MessagesActivity.this)
//                                            .setView(video_quality)
//                                            .show().getWindow().setLayout((int) (getApplicationContext().getResources().getDisplayMetrics().widthPixels / 1.3), (int) (((TextView) video_quality.getChildAt(0)).getTextSize() * 2 * video_quality.getChildCount()));
//
////                                    if (isConnectedWifi())
////                                        videourl = video.getJSONObject("files").get("hls").toString();
////                                    else
////                                        videourl = video.getJSONObject("files").get("mp4_360").toString();
////                                    intent.setDataAndType(Uri.parse(videourl), "video/*");
//                                } else {
//                                    videourl = video.getJSONObject("files").get("external").toString();
//                                    intent.setData(Uri.parse(videourl));
//                                    startActivity(intent);
//                                }
//
//                            } catch (JSONException e) {
//                                e.printStackTrace();
//                            }
//
//
//                        }
//                    });
//
//                    video_preview.setPadding(convertDpToPixel(5), convertDpToPixel(5), convertDpToPixel(5), convertDpToPixel(5));
//                    video_preview.setLayoutParams(new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
//                    video_preview.setAdjustViewBounds(true);
//                    video_preview.setScaleType(ImageView.ScaleType.FIT_XY);
//
//                    ImageView video_play = new ImageView(getApplicationContext());
//                    video_play.setImageBitmap(BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_menu_more));
//                    video_play.setLayoutParams(new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
//                    ((FrameLayout.LayoutParams) video_play.getLayoutParams()).gravity = Gravity.CENTER;
//                    video_play.setRotation(-90);
//                    video_play.setScaleX(2);
//                    video_play.setScaleY(2);
//                    video_frame.addView(video_play);
//
//                    msg_content.addView(video_frame);
//                    attachments_ids[l] = video_frame.getId();
//                    break;
//                case "doc":
//                    JSONObject doc = message_object.getJSONArray("attachments").getJSONObject(l).getJSONObject("doc");
//                    //.setPadding(convertDpToPixel(5), convertDpToPixel(5), convertDpToPixel(5), convertDpToPixel(5));
//                    LinearLayout doc_main = new LinearLayout(getApplicationContext());
//                    doc_main.setOrientation(LinearLayout.HORIZONTAL);
//
//                    ImageView icon = new ImageView(getApplicationContext());
//                    icon.setImageDrawable(AppCompatResources.getDrawable(getApplicationContext(), R.drawable.document));
//                    icon.setLayoutParams(new LinearLayout.LayoutParams(50, 50));
//                    icon.setScaleType(ImageView.ScaleType.FIT_XY);
//                    doc_main.addView(icon);
//
//                    TextView doc_name = new TextView(getApplicationContext());
//                    doc_name.setText(doc.getString("title"));
//                    doc_main.addView(doc_name);
//
//                    doc_main.setTag(doc.getString("url"));
//
//                    doc_main.setOnClickListener(new View.OnClickListener() {
//                        @Override
//                        public void onClick(View v) {
//                            Intent intent = new Intent();
//
//                        }
//                    });
//
//
//                    break;
//            }
//        }
        runOnUiThread(() -> {
            try {
                if (message_object.get("out").toString().equals("0")) {
                    msg_layout.addView(time);
                } else {
                    msg_layout.addView(msg_content);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    public boolean isConnectedWifi() {
        NetworkInfo info = ((ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return info.toString().replaceAll("([|])", "").split(",")[0].split(":")[1].contains("WIFI");
    }

    public boolean isConnectedMobile() {
        NetworkInfo info = ((ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return info.toString().replaceAll("([|])", "").split(",")[0].split(":")[1].contains("MOBILE");
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
}