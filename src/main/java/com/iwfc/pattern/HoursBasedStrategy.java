package com.iwfc.pattern;

import com.iwfc.model.Equipment;

import java.util.HashMap;
import java.util.Map;

public class HoursBasedStrategy implements MaintenanceThresholdStrategy {

    private static final double DEFAULT_LIMIT = 250;

    private final Map<String, Double> limitsByType = new HashMap<>();

    public HoursBasedStrategy() {
        limitsByType.put("treadmill", 500.0);
        limitsByType.put("spin bike", 300.0);
        limitsByType.put("rowing machine", 400.0);
        limitsByType.put("cross trainer", 450.0);
        limitsByType.put("heart rate monitor", 150.0);
    }

    public void setLimit(String type, double hours) {
        if (hours <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }
        limitsByType.put(type.toLowerCase(), hours);
    }

    @Override
    public double getThreshold(Equipment equipment) {
        return limitsByType.getOrDefault(equipment.getType().toLowerCase(), DEFAULT_LIMIT);
    }

    @Override
    public boolean needsMaintenance(Equipment equipment) {
        return equipment.getHoursSinceService() >= getThreshold(equipment);
    }

    @Override
    public String getName() {
        return "Hours based";
    }
}
