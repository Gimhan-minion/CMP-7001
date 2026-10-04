package com.iwfc.model;

import com.iwfc.repository.Identifiable;

public class Equipment implements Identifiable<String> {

    private final String id;
    private String name;
    private String type;
    private String location;
    private EquipmentStatus status;
    private boolean active;
    private double usageHours;
    private double hoursSinceService;

    public Equipment(String id, String name, String type, String location) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Equipment id is required");
        }
        this.id = id.trim().toUpperCase();
        setName(name);
        setType(type);
        setLocation(location);
        this.status = EquipmentStatus.OPERATIONAL;
        this.active = true;
    }

    public void addUsage(double hours) {
        if (hours <= 0) {
            throw new IllegalArgumentException("Usage hours must be greater than zero");
        }
        usageHours += hours;
        hoursSinceService += hours;
    }

    public void markServiced() {
        hoursSinceService = 0;
    }

    public boolean isAvailable() {
        return active && status == EquipmentStatus.OPERATIONAL;
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
            throw new IllegalArgumentException("Equipment name cannot be empty");
        }
        this.name = name.trim();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Equipment type cannot be empty");
        }
        this.type = type.trim();
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("Location cannot be empty");
        }
        this.location = location.trim();
    }

    public EquipmentStatus getStatus() {
        return status;
    }

    public void setStatus(EquipmentStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        this.status = status;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public double getUsageHours() {
        return usageHours;
    }

    public double getHoursSinceService() {
        return hoursSinceService;
    }

    @Override
    public String toString() {
        return String.format("%-6s %-20s %-19s %-12s %-18s %7.1fh %s",
                id, name, type, location, status, usageHours, active ? "" : "(deactivated)");
    }
}
