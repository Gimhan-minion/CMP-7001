package com.iwfc.service;

import com.iwfc.model.Equipment;
import com.iwfc.model.EquipmentStatus;
import com.iwfc.pattern.MaintenanceThresholdStrategy;
import com.iwfc.pattern.NotificationCenter;
import com.iwfc.repository.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EquipmentService {

    private final Repository<Equipment, String> equipmentRepo;
    private final NotificationCenter notifications;
    private MaintenanceThresholdStrategy thresholdStrategy;
    private final Set<String> alerted = new HashSet<>();

    public EquipmentService(Repository<Equipment, String> equipmentRepo, MaintenanceThresholdStrategy thresholdStrategy) {
        this.equipmentRepo = equipmentRepo;
        this.thresholdStrategy = thresholdStrategy;
        this.notifications = NotificationCenter.getInstance();
    }

    public Equipment addEquipment(String id, String name, String type, String location) {
        return addEquipment(new Equipment(id, name, type, location));
    }

    public Equipment addEquipment(Equipment equipment) {
        return equipmentRepo.add(equipment);
    }

    public Equipment editEquipment(String id, String name, String location) {
        Equipment equipment = equipmentRepo.getById(normalise(id));
        if (name != null && !name.isBlank()) {
            equipment.setName(name);
        }
        if (location != null && !location.isBlank()) {
            equipment.setLocation(location);
        }
        return equipmentRepo.update(equipment);
    }

    public Equipment updateStatus(String id, EquipmentStatus status) {
        Equipment equipment = equipmentRepo.getById(normalise(id));
        equipment.setStatus(status);
        return equipmentRepo.update(equipment);
    }

    public void deactivate(String id) {
        Equipment equipment = equipmentRepo.getById(normalise(id));
        equipment.setActive(false);
        notifications.publish(NotificationCenter.INSTRUCTORS,
                equipment.getName() + " (" + equipment.getId() + ") has been taken out of service");
    }

    public void reactivate(String id) {
        equipmentRepo.getById(normalise(id)).setActive(true);
    }

    public boolean logUsage(String id, double hours) {
        Equipment equipment = equipmentRepo.getById(normalise(id));
        if (!equipment.isActive()) {
            throw new IllegalStateException(equipment.getId() + " is deactivated");
        }
        equipment.addUsage(hours);
        // Delegates the decision to whichever strategy is currently set
        if (thresholdStrategy.needsMaintenance(equipment) && alerted.add(equipment.getId())) {
            notifications.publish(NotificationCenter.ADMINS, String.format(
                    "Preventative maintenance due for %s (%s): %.1f hours since last service, limit %.0f",
                    equipment.getName(), equipment.getId(), equipment.getHoursSinceService(),
                    thresholdStrategy.getThreshold(equipment)));
            return true;
        }
        return false;
    }

    public void markServiced(String id) {
        Equipment equipment = equipmentRepo.getById(normalise(id));
        equipment.markServiced();
        alerted.remove(equipment.getId());
    }

    public Equipment getById(String id) {
        return equipmentRepo.getById(normalise(id));
    }

    public List<Equipment> getAll() {
        return equipmentRepo.findAll();
    }

    public List<Equipment> getAvailable() {
        return equipmentRepo.findBy(Equipment::isAvailable);
    }

    public List<Equipment> getByStatus(EquipmentStatus status) {
        return equipmentRepo.findBy(e -> e.getStatus() == status);
    }

    public List<Equipment> getDueForMaintenance() {
        return equipmentRepo.findBy(e -> e.isActive() && thresholdStrategy.needsMaintenance(e));
    }

    public MaintenanceThresholdStrategy getThresholdStrategy() {
        return thresholdStrategy;
    }

    public void setThresholdStrategy(MaintenanceThresholdStrategy thresholdStrategy) {
        this.thresholdStrategy = thresholdStrategy;
        alerted.clear();
    }

    private String normalise(String id) {
        return id == null ? "" : id.trim().toUpperCase();
    }
}
