package com.iwfc.pattern;

public interface NotificationListener {

    String getId();

    void onNotification(String message);
}
