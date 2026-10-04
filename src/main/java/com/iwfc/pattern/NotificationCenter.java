package com.iwfc.pattern;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class NotificationCenter {

    public static final String ADMINS = "ADMINS";
    public static final String INSTRUCTORS = "INSTRUCTORS";
    public static final String MEMBERS = "MEMBERS";

    // Singleton pattern (creational): one shared notification hub for the whole system
    private static NotificationCenter instance;

    private final Map<String, List<NotificationListener>> subscribers = new HashMap<>();
    private final List<String> history = new ArrayList<>();

    private NotificationCenter() {
    }

    public static synchronized NotificationCenter getInstance() {
        if (instance == null) {
            instance = new NotificationCenter();
        }
        return instance;
    }

    public void subscribe(String topic, NotificationListener listener) {
        List<NotificationListener> list = subscribers.computeIfAbsent(topic, key -> new ArrayList<>());
        if (!list.contains(listener)) {
            list.add(listener);
        }
    }

    public void unsubscribe(String topic, NotificationListener listener) {
        List<NotificationListener> list = subscribers.get(topic);
        if (list != null) {
            list.remove(listener);
        }
    }

    // Observer pattern (behavioural): every listener subscribed to the topic is notified automatically
    public int publish(String topic, String message) {
        List<NotificationListener> list = subscribers.getOrDefault(topic, Collections.emptyList());
        String stamped = "[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) + "] " + message;
        for (NotificationListener listener : new ArrayList<>(list)) {
            listener.onNotification(stamped);
        }
        history.add(topic + " -> " + message);
        return list.size();
    }

    public int notifyUser(String userId, String message) {
        return publish(userId, message);
    }

    public List<String> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public void reset() {
        subscribers.clear();
        history.clear();
    }
}
