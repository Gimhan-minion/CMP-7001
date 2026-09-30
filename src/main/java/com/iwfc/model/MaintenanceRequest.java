package com.iwfc.model;

import com.iwfc.exception.InvalidStatusTransitionException;
import com.iwfc.repository.Identifiable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MaintenanceRequest implements Identifiable<String> {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final String id;
    private final String equipmentId;
    private final String description;
    private final Urgency urgency;
    private final String reportedBy;
    private final LocalDateTime createdAt;
    private RequestStatus status;
    private String assignedTo;
    private LocalDateTime updatedAt;
    private final List<String> history;

    public MaintenanceRequest(String id, String equipmentId, String description, Urgency urgency, String reportedBy) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description is required");
        }
        if (urgency == null) {
            throw new IllegalArgumentException("Urgency is required");
        }
        this.id = id;
        this.equipmentId = equipmentId;
        this.description = description.trim();
        this.urgency = urgency;
        this.reportedBy = reportedBy;
        this.status = RequestStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = createdAt;
        this.history = new ArrayList<>();
        history.add(createdAt.format(FORMAT) + " reported by " + reportedBy);
    }

    public void moveTo(RequestStatus next) {
        if (!status.canMoveTo(next)) {
            throw new InvalidStatusTransitionException(
                    "Request " + id + " cannot move from " + status + " to " + next);
        }
        status = next;
        updatedAt = LocalDateTime.now();
        history.add(updatedAt.format(FORMAT) + " status changed to " + next);
    }

    public void assignTo(String technician) {
        if (technician == null || technician.isBlank()) {
            throw new IllegalArgumentException("Technician name is required");
        }
        moveTo(RequestStatus.ASSIGNED);
        this.assignedTo = technician.trim();
    }

    @Override
    public String getId() {
        return id;
    }

    public String getEquipmentId() {
        return equipmentId;
    }

    public String getDescription() {
        return description;
    }

    public Urgency getUrgency() {
        return urgency;
    }

    public String getReportedBy() {
        return reportedBy;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<String> getHistory() {
        return Collections.unmodifiableList(history);
    }

    @Override
    public String toString() {
        return String.format("%-6s %-6s %-7s %-10s %-40s by %-6s %s",
                id, equipmentId, urgency, status, description, reportedBy,
                assignedTo == null ? "" : "-> " + assignedTo);
    }
}
