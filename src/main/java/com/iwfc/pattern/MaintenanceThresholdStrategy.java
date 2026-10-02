package com.iwfc.pattern;

import com.iwfc.model.Equipment;

public interface MaintenanceThresholdStrategy {

    double getThreshold(Equipment equipment);

    boolean needsMaintenance(Equipment equipment);

    String getName();
}
