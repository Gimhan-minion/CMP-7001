package com.iwfc.model;

import com.iwfc.pattern.NotificationListener;
import com.iwfc.repository.Identifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Abstraction: User is abstract and cannot be created directly. It defines what every user
// has in common and leaves role specific behaviour to the subclasses.
public abstract class User implements Identifiable<String>, NotificationListener {

    // Encapsulation: fields are private and only changed through validated setters
    private final String id;
    private String name;
    private String email;
    private boolean active;
    private final List<String> inbox;

    protected User(String id, String name, String email) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("User id is required");
        }
        this.id = id.trim().toUpperCase();
        setName(name);
        setEmail(email);
        this.active = true;
        this.inbox = new ArrayList<>();
    }

    // Abstract methods that each subclass must implement differently (polymorphism)
    public abstract Role getRole();

    public abstract String getMenuTitle();

    public abstract boolean canManageEquipment();

    public abstract boolean canScheduleSessions();

    public abstract boolean canBookSessions();

    public abstract boolean canReportFaults();

    public boolean canViewMaintenanceLog() {
        return canManageEquipment();
    }

    public boolean canManageUsers() {
        return false;
    }

    @Override
    public void onNotification(String message) {
        inbox.add(message);
    }

    // Returns a read only view so the inbox cannot be modified from outside
    public List<String> getInbox() {
        return Collections.unmodifiableList(inbox);
    }

    public void clearInbox() {
        inbox.clear();
    }

    @Override
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        this.name = name.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email: " + email);
        }
        this.email = email.trim().toLowerCase();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return String.format("%-6s %-20s %-28s %-13s %s", id, name, email, getRole(), active ? "Active" : "Inactive");
    }
}
