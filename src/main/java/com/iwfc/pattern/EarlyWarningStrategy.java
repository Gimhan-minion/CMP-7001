package com.iwfc.pattern;

import com.iwfc.model.Equipment;

public class EarlyWarningStrategy implements MaintenanceThresholdStrategy {

    private final HoursBasedStrategy base;
    private final double ratio;

    public EarlyWarningStrategy(double ratio) {
        if (ratio <= 0 || ratio > 1) {
            throw new IllegalArgumentException("Ratio must be between 0 and 1");
        }
        this.base = new HoursBasedStrategy();
        this.ratio = ratio;
    }

    @Override
    public double getThreshold(Equipment equipment) {
        return base.getThreshold(equipment) * ratio;
    }

    @Override
    public boolean needsMaintenance(Equipment equipment) {
        return equipment.getHoursSinceService() >= getThreshold(equipment);
    }

    @Override
    public String getName() {
        return "Early warning (" + Math.round(ratio * 100) + "%)";
    }
}
