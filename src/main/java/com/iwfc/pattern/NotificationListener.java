package com.iwfc.pattern;

// Observer interface, implemented by User so any user can receive notifications
public interface NotificationListener {

    String getId();

    void onNotification(String message);
}
