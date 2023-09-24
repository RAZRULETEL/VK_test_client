package com.mastik.vk_test_mod.dialogs;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.widget.LinearLayout.HORIZONTAL;
import static com.mastik.vk_test_mod.polling.LongPollService.getLongPollServer;
import static com.mastik.vk_test_mod.MainActivity.UNREAD_MESSAGE_COLOR;
import static com.mastik.vk_test_mod.MainActivity.getAction;
import static com.mastik.vk_test_mod.MainActivity.getFileType;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.mastik.vk_test_mod.polling.LongPollService;
import com.mastik.vk_test_mod.MainActivity;
import com.mastik.vk_test_mod.MessagesActivity;
import com.mastik.vk_test_mod.R;
import com.mastik.vk_test_mod.RandomTools;
import com.mastik.vk_test_mod.dataTypes.VKDialog;
import com.mastik.vk_test_mod.dataTypes.VKImage;
import com.mastik.vk_test_mod.dataTypes.VKMessage;
import com.mastik.vk_test_mod.dataTypes.VKUser;
import com.mastik.vk_test_mod.dataTypes.listeners.IntListener;
import com.mastik.vk_test_mod.dataTypes.listeners.MessageListener;
import com.mastik.vk_test_mod.db.AppDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class DialogsFragment extends Fragment {
    private static final int LOAD_DIALOGS_COUNT = 30;
    private static final String TAG = DialogsFragment.class.getSimpleName();
    private Context context;
    private LinearLayout scroll;
    private boolean isError = true;
    private final HashSet<VKDialog> loadedDialogs = new HashSet<>();

    public DialogsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getContext();

        // Instantiate the RequestQueue.
        String url = "https://api.vk.com/method/messages.getConversations?access_token=" + MainActivity.getToken(context) + "&count=" + LOAD_DIALOGS_COUNT + "&extended=1&fields=" + context.getResources().getString(R.string.default_user_fields) + "&v=5.131";//

// Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Timber.tag(TAG).d("Received response: %s, from: %s", response, url);
                        if (scroll != null)
                            scroll.removeAllViews();
                        AppDatabase.getInstance(getContext());
                        try {
                            JSONObject responseObject = new JSONObject(response).getJSONObject("response");
                            JSONArray users = responseObject.getJSONArray("profiles");
                            JSONArray groups = responseObject.has("groups") ? responseObject.getJSONArray("groups") : null;
                            JSONArray dialogs = responseObject.getJSONArray("items");
                            MainActivity.BACKGROUND_THREADS.execute(() -> {
                                VKUser.saveUsersFromJSON(users, getContext());
                                if(groups != null) VKUser.saveUsersFromGroupsJSON(groups, getContext());
                                List<VKDialog> receivedDialogs =  RandomTools.JSONToVKDialogs(dialogs, getContext());
                                loadedDialogs.addAll(receivedDialogs);
                                renderDialogs(receivedDialogs);
                            });
                        } catch (JSONException e) {
                            Timber.tag(TAG).e(e, "Received response: %s, from: %s", response, url);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.err.println(error.toString());
                isError = true;
            }
        });

        MainActivity.getVolleyQueue(context).add(stringRequest);
        if (!LongPollService.isActive())
            getLongPollServer(context, MainActivity.getToken(), scroll);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_dialogs, container, false);
        assert container != null;
        scroll = root.findViewById(R.id.scroll);
        if (isError) {
            TextView textView = new TextView(context);
            textView.setText("That didn't work!");
            textView.setBackgroundColor(Color.RED);
            textView.setTextSize(20);
            textView.setGravity(Gravity.CENTER);
            textView.setTextColor(Color.BLACK);
            scroll.addView(textView);
        } else if (scroll != null)
            scroll.removeAllViews();
//        buildDialogs(readSourceData("last"));
        return root;
    }

    /**
     * Renders dialog with given info
     *
     * @param main           container returned with dialog view
     * @param title          of dialog
     * @param last_msg       text
     * @param timestamp      last message date
     * @param logo           of dialog
     * @param logo_last      logo of last message sender
     * @param unreadMsgCount count of unread messages
     * @return main with all elements
     * @deprecated
     */
    @Deprecated
    private LinearLayout renderDialog(LinearLayout main, String title, String last_msg, Long timestamp, ImageView logo, ImageView logo_last, int unreadMsgCount) {
        main.setOrientation(HORIZONTAL);
        main.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ripple, null));
        ViewGroup.LayoutParams main_params = new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        main.setLayoutParams(main_params);
        main.setPadding(0, 20, 0, 20);


        LinearLayout text_layout = new LinearLayout(context);
        text_layout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams text_params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        text_layout.setLayoutParams(text_params);


        LinearLayout title_layout = new LinearLayout(context);
        title_layout.setId(View.generateViewId());

        LinearLayout.LayoutParams title_params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        title_layout.setLayoutParams(title_params);


        LinearLayout last_msg_layout = new LinearLayout(context);
        last_msg_layout.setOrientation(HORIZONTAL);
        last_msg_layout.setLayoutParams(text_params);
        LinearLayout.LayoutParams last_msg_params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        last_msg_params.setMargins(0, convertDpToPixel(7), 0, 0);
        last_msg_layout.setLayoutParams(last_msg_params);


        TextView titleV = new TextView(context);
        titleV.setId(View.generateViewId());
        titleV.setTextSize(16);
        LinearLayout.LayoutParams text = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, 1);
        titleV.setLayoutParams(text);
        titleV.setText(title);
        titleV.setMaxLines(1);
        titleV.setEllipsize(TextUtils.TruncateAt.END);


        TextView last_msgV = new TextView(context);
        last_msgV.setTextSize(15);
        last_msgV.setText(last_msg);
        last_msgV.setMaxLines(1);
        last_msgV.setEllipsize(TextUtils.TruncateAt.END);


        TextView last_time = new TextView(context);
        last_time.setId(View.generateViewId());
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm", Locale.ROOT);
        String dateString = formatter.format(new Date(timestamp * 1000));
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

        main.post(() -> {
            main.addView(logo);
            main.addView(text_layout);

            title_layout.addView(titleV);
            title_layout.addView(last_time);

            text_layout.addView(title_layout);
            text_layout.addView(last_msg_layout);

            last_msg_layout.addView(logo_last);
            last_msg_layout.addView(last_msgV);
        });

        Log.i("msg", String.valueOf(unreadMsgCount));
        if (unreadMsgCount > 0) {
            PorterDuffColorFilter greyFilter = new PorterDuffColorFilter(UNREAD_MESSAGE_COLOR, PorterDuff.Mode.MULTIPLY);
            main.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.unread_ripple, null));
            main.getBackground().setColorFilter(greyFilter);

            TextView unreadMsgs = new TextView(context);
            unreadMsgs.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            unreadMsgs.setText(String.valueOf(unreadMsgCount));
            unreadMsgs.setTextColor(Color.WHITE);

            GradientDrawable round_back = new GradientDrawable();
            round_back.setColor(Color.BLACK);
            round_back.setCornerRadius(50);
            unreadMsgs.setBackground(round_back);

            unreadMsgs.setMaxLines(1);
            last_msgV.post(() -> {
                LinearLayout.LayoutParams last_params = (LinearLayout.LayoutParams) last_msgV.getLayoutParams();
                last_params.weight = 1;
                last_msgV.setLayoutParams(last_params);
            });
            unreadMsgs.post(() -> {
                LinearLayout.LayoutParams last_params1 = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                last_params1.setMargins(0, 0, convertDpToPixel(23), 0);
                unreadMsgs.setLayoutParams(last_params1);
                unreadMsgs.setPadding(convertDpToPixel(11), 3, convertDpToPixel(11), 3);
            });
            last_msg_layout.post(() -> last_msg_layout.addView(unreadMsgs));
        } else if (unreadMsgCount < 0)
            last_msg_layout.setBackgroundColor(UNREAD_MESSAGE_COLOR);
        last_msgV.post(() -> {
            LinearLayout.LayoutParams last_params = (LinearLayout.LayoutParams) last_msgV.getLayoutParams();
            last_params.setMargins(convertDpToPixel(5), 0, 0, 0);
            last_msgV.setLayoutParams(last_params);
        });

        main.setClickable(true);
        main.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Toast.makeText(context,"negr  "+view.getId(),Toast.LENGTH_SHORT).show();
                TextView chat_name = (TextView) ((ViewGroup) (((ViewGroup) ((ViewGroup) view).getChildAt(1)).getChildAt(0))).getChildAt(0);
                Intent intent = new Intent(getActivity(), MessagesActivity.class);
                intent.putExtra("id", Integer.toString(view.getId()));
                if (view.getTag().toString().contains("split")) {
                    intent.putExtra("unreadMsgCount", view.getTag().toString().split("split")[0]);
                    intent.putExtra("chat_info", view.getTag().toString().split("split")[1]);
                } else {
                    intent.putExtra("unreadMsgCount", view.getTag().toString());
                    intent.putExtra("chat_info", "");
                }
                intent.putExtra("chat_name", chat_name.getText());

                startActivity(intent);
            }
        });

        return main;
    }

    private void renderDialogs(List<VKDialog> dialogs) {
        for (VKDialog dialog : dialogs) {
            LinearLayout diag = renderDialog(dialog);
            scroll.post(() -> scroll.addView(diag));
            scroll.post(() -> scroll.addView(makeDialogDivider()));
        }
    }

    private LinearLayout renderDialog(VKDialog dialog) {
        LinearLayout main = new LinearLayout(getContext());
        main.setTag(dialog.getId());
        main.setOrientation(HORIZONTAL);
        main.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ripple, null));
        ViewGroup.LayoutParams main_params = new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        main.setLayoutParams(main_params);
        main.setPadding(0, 20, 0, 20);


        LinearLayout text_layout = new LinearLayout(context);
        text_layout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams text_params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        text_layout.setLayoutParams(text_params);


        LinearLayout title_layout = new LinearLayout(context);
        title_layout.setId(View.generateViewId());

        LinearLayout.LayoutParams title_params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        title_layout.setLayoutParams(title_params);


        LinearLayout last_msg_layout = new LinearLayout(context);
        last_msg_layout.setOrientation(HORIZONTAL);
        last_msg_layout.setLayoutParams(text_params);
        LinearLayout.LayoutParams last_msg_params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        last_msg_params.setMargins(0, convertDpToPixel(7), 0, 0);
        last_msg_layout.setLayoutParams(last_msg_params);


        TextView titleText = new TextView(context);
        titleText.setId(View.generateViewId());
        titleText.setTextSize(16);
        LinearLayout.LayoutParams text = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, 1);
        titleText.setLayoutParams(text);
        titleText.setText(dialog.getTitle());
        titleText.setMaxLines(1);
        titleText.setEllipsize(TextUtils.TruncateAt.END);


        TextView lastMessageText = new TextView(context);
        lastMessageText.setTextSize(15);
        lastMessageText.setText(dialog.getLastMessage().getExtendedText());
        lastMessageText.setMaxLines(1);
        lastMessageText.setEllipsize(TextUtils.TruncateAt.END);
        lastMessageText.setLayoutParams(new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1));
        ((LinearLayout.LayoutParams) lastMessageText.getLayoutParams()).setMargins(convertDpToPixel(5), 0, 0, 0);


        TextView lastMessageTime = new TextView(context);
        lastMessageTime.setId(View.generateViewId());
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm", Locale.ROOT);
        String dateString = formatter.format(new Date(dialog.getLastMessage().getTimestamp().toEpochMilli()));
        lastMessageTime.setText(dateString);
        lastMessageTime.setPadding(0, 0, convertDpToPixel(20), 0);


        ImageView lastMessageSenderLogo = new ImageView(context);
        lastMessageSenderLogo.setLayoutParams(new ViewGroup.LayoutParams(convertDpToPixel(28), convertDpToPixel(28)));
        VKUser messageOwner = VKUser.getById(dialog.getLastMessage().getUserId(), context);
//        if (dialog.isUserDialog()) { //TODO fix groups
            if (messageOwner.getAvatarUrl() == null)
                Timber.tag(TAG).e(messageOwner.toString());
            VKImage.get(messageOwner.getAvatarUrl(), messageOwner.getId(), VKUser.PREFIX, context).init(context).addOnInitListener(img -> lastMessageSenderLogo.post(() -> lastMessageSenderLogo.setImageBitmap(img.getRoundedCorner())));
//        }


        dialog.addNewMessageListener(new MessageListener() {
            @Override
            public void onNewMessage(VKMessage msg) {
                lastMessageText.setText(msg.getExtendedText());
                SimpleDateFormat formatter = new SimpleDateFormat("HH:mm", Locale.ROOT);
                String dateString = formatter.format(new Date(msg.getTimestamp().toEpochMilli()));
                lastMessageTime.setText(dateString);
                VKUser.getById(msg.getUserId(), getContext()).setOnInitListener(
                        user -> VKImage.get(user.getAvatarUrl(), user.getId(), VKUser.PREFIX, getContext())
                                .init(getContext())
                                .addOnInitListener(img -> lastMessageSenderLogo.post(() -> lastMessageSenderLogo.setImageBitmap(img.getRoundedCorner())))
                );
            }
        });


        ImageView logo = new ImageView(context);
        logo.setAdjustViewBounds(true);
        logo.setScaleType(ImageView.ScaleType.FIT_XY);
        logo.setLayoutParams(new ViewGroup.LayoutParams(WRAP_CONTENT, convertDpToPixel(60)));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(convertDpToPixel(60), convertDpToPixel(60));
        params.setMargins(convertDpToPixel(10), 0, convertDpToPixel(10), 0);
        logo.setLayoutParams(params);
        logo.setImageBitmap(dialog.getLogo());
        dialog.addLogoChangeListener(bit -> logo.post(() -> logo.setImageBitmap(bit)));


        TextView unreadMsgs = new TextView(context);
        unreadMsgs.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        unreadMsgs.setTextColor(Color.WHITE);

        GradientDrawable round_back = new GradientDrawable();
        round_back.setColor(Color.BLACK);
        round_back.setCornerRadius(50);
        unreadMsgs.setBackground(round_back);
        unreadMsgs.setMaxLines(1);


        LinearLayout.LayoutParams last_params1 = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        last_params1.setMargins(0, 0, convertDpToPixel(23), 0);
        unreadMsgs.setLayoutParams(last_params1);
        unreadMsgs.setPadding(convertDpToPixel(11), 3, convertDpToPixel(11), 3);


        last_msg_layout.post(() -> last_msg_layout.addView(unreadMsgs));
        dialog.addUnreadMessageListener(new IntListener() {
            private static Drawable unreadBackground;

            @Override
            public void onNewValue(int unreadMessages) {
                if (unreadMessages > 0) {
                    if (unreadBackground == null) {
                        PorterDuffColorFilter greyFilter = new PorterDuffColorFilter(UNREAD_MESSAGE_COLOR, PorterDuff.Mode.MULTIPLY);
                        unreadBackground = ResourcesCompat.getDrawable(getResources(), R.drawable.unread_ripple, null);
                        unreadBackground.setColorFilter(greyFilter);
                    }
                    main.post(() -> main.setBackground(unreadBackground));

                    unreadMsgs.setText(unreadMsgs.toString());
                    unreadMsgs.post(() -> unreadMsgs.setVisibility(View.VISIBLE));
                } else {
                    unreadMsgs.post(() -> {
                        unreadMsgs.setVisibility(View.GONE);
                        main.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ripple, null));
                        if (unreadMessages < 0)
                            last_msg_layout.setBackgroundColor(UNREAD_MESSAGE_COLOR);
                    });
                }
            }
        });
        dialog.setUnreadCount(dialog.getUnreadCount());


        main.setClickable(true);
        main.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), MessagesActivity.class);
                intent.putExtra("id", Integer.toString(dialog.getId()));
                startActivity(intent);
            }
        });

        main.post(() -> {
            main.addView(logo);
            main.addView(text_layout);

            title_layout.addView(titleText);
            title_layout.addView(lastMessageTime);

            text_layout.addView(title_layout);
            text_layout.addView(last_msg_layout);

            last_msg_layout.addView(lastMessageSenderLogo);
            last_msg_layout.addView(lastMessageText);
        });


        return main;
    }

    @Deprecated
    private void buildDialogs(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response).getJSONObject("response");
            JSONArray answerArray = jsonObject.getJSONArray("items");

            LongPollService.unread_msg_count = Integer.parseInt(jsonObject.get("unread_count").toString());
            if (jsonObject.has("profiles")) {
                JSONArray profiles = jsonObject.getJSONArray("profiles");
                for (int i = 0; i < profiles.length(); i++)
                    VKUser.save(VKUser.getFromJSON(profiles.getJSONObject(i)), context);
            }
            JSONArray groups = null;
            if (jsonObject.has("groups"))
                groups = jsonObject.getJSONArray("groups");

            for (int l = 0; l < answerArray.length(); l++) {
                long now = System.currentTimeMillis();
                JSONObject conversation = answerArray.getJSONObject(l);
                JSONObject last_message = conversation.getJSONObject("last_message");

                String title = "";
                String last_msg;
                Long time = Long.parseLong(last_message.get("date").toString());
                ImageView logo_last = new ImageView(context);
                int unread = unreadMsgCount(conversation.toString());
                LinearLayout main = new LinearLayout(context);

                last_msg = last_message.get("text").toString();
                if (last_message.get("text").toString().equals(""))
                    if (last_message.getJSONArray("attachments").length() > 0)
                        last_msg = getFileType(last_message.getJSONArray("attachments").getJSONObject(0).get("type").toString());
                    else if (last_message.has("action")) {
                        last_msg = VKUser.getById(last_message.getJSONObject("action").getInt("member_id"), getContext()).getFullName() + " ";
                        last_msg += getAction(last_message.getJSONObject("action").get("type").toString());
                    }

                int id = conversation.getJSONObject("conversation").getJSONObject("peer").getInt("id");
                ImageView logo = new ImageView(context);
                if (Integer.parseInt(last_message.get("random_id").toString()) != 0)
                    if (!conversation.getJSONObject("conversation").get("in_read").toString().equals(conversation.getJSONObject("conversation").get("out_read").toString()))
                        unread = Integer.parseInt(conversation.getJSONObject("conversation").get("out_read").toString()) - Integer.parseInt(conversation.getJSONObject("conversation").get("in_read").toString());
                String logo_url = "";
                switch (conversation.getJSONObject("conversation").getJSONObject("peer").get("type").toString()) {
                    case "chat":
                        title = conversation.getJSONObject("conversation").getJSONObject("chat_settings").get("title").toString();
                        if (conversation.getJSONObject("conversation").getJSONObject("chat_settings").has("photo"))
                            logo_url = conversation.getJSONObject("conversation").getJSONObject("chat_settings").getJSONObject("photo").get("photo_50").toString();
                        break;
                    case "user":
                        title = VKUser.getById(id, getContext()).getFullName();
                        logo_url = VKUser.getById(id, getContext()).getAvatarUrl();
                        break;
                    case "group":
                        int i;
                        for (i = 0; i < groups.length(); i++) {
                            if (groups.getJSONObject(i).get("id").toString().equals(Math.abs(id) + ""))
                                break;
                        }
                        title = groups.getJSONObject(i).get("name").toString();
                        logo_url = groups.getJSONObject(i).get("photo_50").toString();
                        break;
                }
                String finalLogo_url = logo_url;

                if (!last_message.has("action")) {
                    int logo_id = last_message.getInt("from_id");
                    if (!last_message.get("from_id").toString().contains("-")) {
                        logo_url = VKUser.getById(last_message.getInt("from_id"), getContext()).getAvatarUrl();
                    } else {
                        int i;
                        for (i = 0; i < groups.length(); i++)
                            if (groups.getJSONObject(i).get("id").toString().equals(last_message.get("from_id").toString().replace("-", "")))
                                break;
                        logo_url = groups.getJSONObject(i).get("photo_50").toString();
                    }
                    String finalLogo_url1 = logo_url;
                    if (logo_url.equals(finalLogo_url)) {
                        if (!finalLogo_url.equals("")) {
                            VKImage.get(finalLogo_url, id, context).init(getContext()).addOnInitListener(img ->
                                    getActivity().runOnUiThread(() -> {
                                        logo.setImageBitmap(img.getRoundedCorner());
                                        logo_last.setImageBitmap(img.getRoundedCorner());
                                    }));
                        } else {
                            VKImage finalImg = VKImage.getDefault(context);
                            getActivity().runOnUiThread(() -> {
                                logo.setImageBitmap(finalImg.getRoundedCorner());
                                logo_last.setImageBitmap(finalImg.getRoundedCorner());
                            });
                        }
                    } else
                        VKImage.get(finalLogo_url, id, context).init(context).addOnInitListener(vkimg -> getActivity().runOnUiThread(() -> logo.setImageBitmap(vkimg.getRoundedCorner())));
                    VKImage.get(finalLogo_url1, logo_id, context).init(getContext()).addOnInitListener(img -> getActivity().runOnUiThread(() -> logo_last.setImageBitmap(img.getImg())));
                }


                main.setId(id);
                main.setTag(unread);
                switch (conversation.getJSONObject("conversation").getJSONObject("peer").get("type").toString()) {
                    case "chat":
                        main.setTag(unread + "split" + conversation.getJSONObject("conversation").getJSONObject("chat_settings"));
                        break;
                    case "user":
                        if (VKUser.getById(id, getContext()) != null)
                            main.setTag(unread + "split" + id);
                        break;

                }

                LinearLayout dialog = renderDialog(main, title, last_msg, time, logo, logo_last, unread);
                scroll.post(() -> scroll.addView(dialog));
                scroll.post(() -> scroll.addView(makeDialogDivider()));
                Log.i("Time", " " + (System.currentTimeMillis() - now));

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private int convertDpToPixel(float dp) {
        return MainActivity.convertDpToPixel(dp, context);
    }

    @Deprecated
    private int unreadMsgCount(String rsp) {
        try {
            return Integer.parseInt(new JSONObject(rsp).getJSONObject("conversation").get("out_read_cmid").toString()) - Integer.parseInt(new JSONObject(rsp).getJSONObject("conversation").get("in_read_cmid").toString());
        } catch (JSONException e) {
            Log.e("Unread msg count", "Error parse JSON: " + e.getMessage());
            return 0;
        }
    }

    private LinearLayout makeDialogDivider() {
        LinearLayout hr_cont = new LinearLayout(context);
        TextView hr = new TextView(context);
        LinearLayout.LayoutParams line = new LinearLayout.LayoutParams((int) (scroll.getWidth() * 0.6), 5);
        line.gravity = Gravity.CENTER;
        hr.setLayoutParams(line);
        hr.setBackgroundColor(Color.parseColor("#E6E6E6"));
        hr_cont.setOrientation(LinearLayout.VERTICAL);
        hr_cont.setBackgroundColor(getResources().getColor(R.color.transparent, null));
        hr_cont.addView(hr);
        return hr_cont;
    }
}