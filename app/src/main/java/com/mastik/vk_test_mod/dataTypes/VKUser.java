package com.mastik.vk_test_mod.dataTypes;

import android.content.Context;

import androidx.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.mastik.vk_test_mod.MainActivity;
import com.mastik.vk_test_mod.dataTypes.listeners.UserInitListener;
import com.mastik.vk_test_mod.db.AppDatabase;
import com.mastik.vk_test_mod.db.UserEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.util.HashMap;

import timber.log.Timber;

public class VKUser {
    private static final String TAG = VKUser.class.getSimpleName();
    private static final HashMap<Integer, VKUser> loadedUsers = new HashMap<>();
    public static final String PREFIX = "user_";
    private UserInitListener listener;
    private final int id;
    private String firstName, lastName, screenName;
    private boolean isFriend, isMale;
    private String avatarUrl;
    private Instant lastSeenDate;

    public VKUser(int id, String firstName, String lastName, String screenName, boolean isFriend, boolean isMale, String avatarUrl) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.screenName = screenName;
        this.isFriend = isFriend;
        this.isMale = isMale;
        this.avatarUrl = avatarUrl;
        this.lastSeenDate = Instant.EPOCH;
        loadedUsers.put(id, this);
    }

    private VKUser(int id) {
        this.id = id;
    }

    @SuppressWarnings("UnusedReturnValue")
    public VKUser init(Context context) {
        MainActivity.BACKGROUND_THREADS.execute(() -> {
            String url = "https://api.vk.com/method/users.get&access_token=" + MainActivity.getToken() + "&fields=" + context.getResources().getString(com.mastik.vk_test_mod.R.string.default_user_fields) + "&v=5.131";
            StringRequest userRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        JSONObject user = new JSONObject(response);
                        VKUser.this.firstName = user.getString("first_name");
                        VKUser.this.lastName = user.getString("last_name");
                        VKUser.this.screenName = user.getString("screen_name");
                        VKUser.this.isFriend = user.getInt("is_friend") == 1;
                        VKUser.this.avatarUrl = user.getString("photo");
                        save(VKUser.this, context);
                        loadedUsers.put(VKUser.this.id, VKUser.this);
                        listener.onInitListener(VKUser.this);
                    } catch (JSONException ignored) {
                    }
                }
            }, error -> Timber.tag(TAG).e(error, "users.get error from: %s", url));
        });
        return this;
    }

    // getters *************************************

    public int getId() {
        return id;
    }

    public String getScreenName() {
        return screenName;
    }

    public boolean isFriend() {
        return isFriend;
    }

    public boolean isMale() {
        return isMale;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public Instant getLastSeenDate() {
        return lastSeenDate;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    // *************************************

    public VKUser updateOnline(Instant lastSeenDate){
        this.lastSeenDate = lastSeenDate;
        return this;
    }

    public void setOnInitListener(UserInitListener list) {
        if(avatarUrl != null)
            list.onInitListener(this);
        else
            listener = list;
    }

    public UserEntity toUserEntity(){
        return new UserEntity(id, firstName, lastName, screenName, isFriend, isMale, avatarUrl, lastSeenDate.toEpochMilli());
    };

    public static VKUser getFromJSON(JSONObject user) {
        if (user.has("id")) {
            try {
                String photoUrl = null;
                if(user.has("photo"))
                    photoUrl = user.getString("photo");
                if(user.has("photo_50"))
                    photoUrl = user.getString("photo_50");
                return new VKUser(user.getInt("id"), user.getString("first_name"), user.getString("last_name"), user.getString("screen_name"), !user.has("deactivated") && user.has("is_friend") && user.getInt("is_friend") == 1, user.has("sex") && user.getInt("sex") == 2, photoUrl).updateOnline(user.has("last_seen") ? Instant.ofEpochMilli(user.getJSONObject("last_seen").getLong("time") * 1000) : Instant.EPOCH);
            } catch (JSONException e) {
                Timber.tag(TAG).e(e, user.toString());
            }
        }
        return null;
    }

    public static VKUser getFromGroupJSON(JSONObject group) {
        if (group.has("id")) {
            try {
                String photoUrl = null;
                if(group.has("photo"))
                    photoUrl = group.getString("photo");
                if(group.has("photo_50"))
                    photoUrl = group.getString("photo_50");
                return new VKUser(-group.getInt("id"), group.getString("name"), "", group.getString("screen_name"), !group.has("is_member") && group.getInt("is_member") == 1, false, photoUrl);
            } catch (JSONException e) {
                Timber.tag(TAG).e(e, group.toString());
            }
        }
        return null;
    }

    public static VKUser getFromEntity(UserEntity user) {
        return new VKUser(user.getId(), user.getFirstName(), user.getLastName(), user.getScreenName(), user.isFriend(), user.isMale(), user.getAvatarUrl());
    }

    public static VKUser getById(int id, Context context) {
        synchronized (loadedUsers) {
            if (loadedUsers.get(id) != null)
                return loadedUsers.get(id);
        }
        UserEntity entity = AppDatabase.getInstance(context).getUserDAO().getUserById(id);
        if(entity != null)
            return VKUser.getFromEntity(entity);
        else {
            VKUser user = new VKUser(id);
            user.init(context);
            return user;
        }

    }

    public static void save(VKUser user, Context context) {
        AppDatabase.getInstance(context).getUserDAO().addUser(user.toUserEntity());
    }

    public static void saveUsersFromJSON(JSONArray users, Context context) {
        try {
            for (int i = 0; i < users.length(); i++) {
                JSONObject user = users.getJSONObject(i);
                VKUser vkUser = VKUser.getFromJSON(user);
                if (vkUser != null)
                    VKUser.save(vkUser, context);
            }
        } catch (JSONException e) {
            Timber.tag(TAG).e(e);
        }
    }

    public static void saveUsersFromGroupsJSON(JSONArray groups, Context context) {
        try {
            for (int i = 0; i < groups.length(); i++) {
                JSONObject user = groups.getJSONObject(i);
                VKUser vkUser = VKUser.getFromGroupJSON(user);
                if (vkUser != null)
                    VKUser.save(vkUser, context);
            }
        } catch (JSONException e) {
            Timber.tag(TAG).e(e);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "VKUser{" +
                "listener=" + listener +
                ", id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", screenName='" + screenName + '\'' +
                ", isFriend=" + isFriend +
                ", isMale=" + isMale +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", lastSeenDate=" + lastSeenDate +
                '}';
    }
}
