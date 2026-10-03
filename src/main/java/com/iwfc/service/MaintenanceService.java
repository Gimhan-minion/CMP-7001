package com.iwfc.service;

import com.iwfc.exception.DuplicateDataException;
import com.iwfc.model.Equipment;
import com.iwfc.model.EquipmentStatus;
import com.iwfc.model.MaintenanceRequest;
import com.iwfc.model.RequestStatus;
import com.iwfc.model.Urgency;
import com.iwfc.pattern.NotificationCenter;
import com.iwfc.repository.Repository;

import java.util.Comparator;
import java.util.List;

public class MaintenanceService {

    private final Repository<MaintenanceRequest, String> requestRepo;
    private final EquipmentService equipmentService;
    private final NotificationCenter notifications;
    private int requestCounter = 0;

    public MaintenanceService(Repository<MaintenanceRequest, String> requestRepo, EquipmentService equipmentService) {
        this.requestRepo = requestRepo;
        this.equipmentService = equipmentService;
        this.notifications = NotificationCenter.getInstance();
    }

    public MaintenanceRequest reportFault(String reporterId, String equipmentId, String description, Urgency urgency) {
        Equipment equipment = equipmentService.getById(equipmentId);
        boolean alreadyOpen = !requestRepo.findBy(r -> r.getEquipmentId().equals(equipment.getId())
                && r.getStatus() != RequestStatus.COMPLETED).isEmpty();
        if (alreadyOpen) {
            throw new DuplicateDataException("An open maintenance request already exists for " + equipment.getId());
        }

        MaintenanceRequest request = new MaintenanceRequest(nextId(), equipment.getId(), description, urgency, reporterId);
        requestRepo.add(request);
        equipmentService.updateStatus(equipment.getId(), EquipmentStatus.FAULTY);

        notifications.publish(NotificationCenter.ADMINS, urgency + " urgency fault reported on "
                + equipment.getName() + " (" + equipment.getId() + "): " + description);
        notifications.notifyUser(reporterId, "Your request " + request.getId() + " has been logged as PENDING");
        return request;
    }

    public MaintenanceRequest assign(String requestId, String technician) {
        MaintenanceRequest request = getById(requestId);
        request.assignTo(technician);
        equipmentService.updateStatus(request.getEquipmentId(), EquipmentStatus.UNDER_MAINTENANCE);
        statusChanged(request);
        return request;
    }

    public MaintenanceRequest complete(String requestId) {
        MaintenanceRequest request = getById(requestId);
        request.moveTo(RequestStatus.COMPLETED);
        equipmentService.updateStatus(request.getEquipmentId(), EquipmentStatus.OPERATIONAL);
        equipmentService.markServiced(request.getEquipmentId());
        statusChanged(request);
        return request;
    }

    public MaintenanceRequest getById(String requestId) {
        return requestRepo.getById(requestId == null ? "" : requestId.trim().toUpperCase());
    }

    public List<MaintenanceRequest> getGlobalLog() {
        return requestRepo.findAll();
    }

    public List<MaintenanceRequest> getByStatus(RequestStatus status) {
        return requestRepo.findBy(r -> r.getStatus() == status);
    }

    public List<MaintenanceRequest> getOpenRequests() {
        List<MaintenanceRequest> open = requestRepo.findBy(r -> r.getStatus() != RequestStatus.COMPLETED);
        open.sort(Comparator.comparing(MaintenanceRequest::getUrgency).reversed()
                .thenComparing(MaintenanceRequest::getCreatedAt));
        return open;
    }

    public List<MaintenanceRequest> getReportedBy(String userId) {
        return requestRepo.findBy(r -> r.getReportedBy().equals(userId));
    }

    private void statusChanged(MaintenanceRequest request) {
        String message = "Request " + request.getId() + " for " + request.getEquipmentId()
                + " is now " + request.getStatus()
                + (request.getAssignedTo() != null ? " (technician: " + request.getAssignedTo() + ")" : "");
        notifications.notifyUser(request.getReportedBy(), message);
        notifications.publish(NotificationCenter.ADMINS, message);
    }

    private String nextId() {
        String id;
        do {
            id = String.format("M%03d", ++requestCounter);
        } while (requestRepo.existsById(id));
        return id;
    }
}
