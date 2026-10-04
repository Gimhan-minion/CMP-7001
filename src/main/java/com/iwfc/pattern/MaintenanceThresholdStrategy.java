package com.iwfc.pattern;

import com.iwfc.model.Equipment;

// Strategy pattern (behavioural): the rule that decides when equipment needs servicing
// can be swapped at runtime without changing EquipmentService
public interface MaintenanceThresholdStrategy {

    double getThreshold(Equipment equipment);

    boolean needsMaintenance(Equipment equipment);

    String getName();
}
