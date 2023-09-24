package com.mastik.vk_test_mod.dataTypes;

import android.graphics.Bitmap;

import com.mastik.vk_test_mod.dataTypes.listeners.BitmapListener;
import com.mastik.vk_test_mod.dataTypes.listeners.IntListener;
import com.mastik.vk_test_mod.dataTypes.listeners.MessageListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

public class VKDialog {
    private static final HashMap<Integer, VKDialog> loadedDialogs = new HashMap<>();
    private final ArrayList<MessageListener> listeners = new ArrayList<>();
    private final ArrayList<BitmapListener> bitmapListeners = new ArrayList<>();
    private final ArrayList<IntListener> unreadListeners = new ArrayList<>();
    private final SortedSet<VKMessage> lastMessages = new TreeSet<>();
    private String title;
    private VKMessage last_msg;
    private Bitmap logo;
    private final int id;
    private int unreadCount = 0, totalUsersCount = 1, onlineUsersCount = 0;

    public VKDialog(String title, Bitmap logo, int id){
        this.title = title;
        this.logo = logo;
        this.id = id;
        loadedDialogs.put(id, this);
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Bitmap getLogo() {
        return logo;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    /**
     * Checks if the dialog is a private dialog with one user
     * @return true if the dialog is a private messages with user, false otherwise (chat or group)
     */
    public boolean isUserDialog() {
        return id < 2_000_000_000 && id > 0;
    }

    public boolean isGroupDialog() {
        return id < 0;
    }

    public boolean isChat(){
        return id > 2_000_000_000;
    }

    public int getOnlineUsersCount() {
        return onlineUsersCount;
    }

    public int getTotalUsersCount() {
        return totalUsersCount;
    }

    public VKMessage getLastMessage() {
        return last_msg;
    }

    public SortedSet<VKMessage> getMessageHistory(){
        return new TreeSet<>(lastMessages);
    }



    public void setTitle(String title) {
        this.title = title;
    }

    public void addMessage(VKMessage message) {
        if(message == null)
            return;
        if(message.getPeerId() != this.id)
            throw new IllegalArgumentException("message from wrong dialog");
        this.last_msg = message;
        lastMessages.add(message);
        for (MessageListener listener : listeners)
            listener.onNewMessage(message);
    }

    public void setUnreadCount(int unreadCount) {
        for (IntListener unreadListener : unreadListeners)
            unreadListener.onNewValue(unreadCount);
        this.unreadCount = unreadCount;
    }

    public void setLogo(Bitmap logo) {
        this.logo = logo;
        for (BitmapListener listener : bitmapListeners) {
            listener.onNewBitmap(logo);
        }
    }

    public void addNewMessageListener(MessageListener listener) {
        listeners.add(listener);
    }

    public void addLogoChangeListener(BitmapListener listener) {
        bitmapListeners.add(listener);
    }

    public void addUnreadMessageListener(IntListener listener) {
        unreadListeners.add(listener);
    }

    @Override
    public String toString() {
        return "VKDialog{" +
                "listeners=" + listeners +
                ", bitmapListeners=" + bitmapListeners +
                ", lastMessages=" + lastMessages +
                ", title='" + title + '\'' +
                ", last_msg=" + last_msg +
                ", logo=" + logo +
                ", id=" + id +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VKDialog vkDialog = (VKDialog) o;
        return id == vkDialog.id && Objects.equals(listeners, vkDialog.listeners) && Objects.equals(bitmapListeners, vkDialog.bitmapListeners) && Objects.equals(lastMessages, vkDialog.lastMessages) && Objects.equals(title, vkDialog.title) && Objects.equals(last_msg, vkDialog.last_msg) && Objects.equals(logo, vkDialog.logo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(listeners, bitmapListeners, lastMessages, title, last_msg, logo, id);
    }

    public static VKDialog getById(int id){
        return loadedDialogs.get(id);
    }
}
