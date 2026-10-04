package com.iwfc.pattern;

import com.iwfc.exception.UnauthorizedAccessException;
import com.iwfc.model.Equipment;
import com.iwfc.model.EquipmentStatus;
import com.iwfc.model.FitnessSession;
import com.iwfc.model.MaintenanceRequest;
import com.iwfc.model.Role;
import com.iwfc.model.Urgency;
import com.iwfc.model.User;
import com.iwfc.service.EquipmentService;
import com.iwfc.service.MaintenanceService;
import com.iwfc.service.SchedulingService;
import com.iwfc.service.UserService;

import java.util.List;

public class FitnessCenterFacade {

    private final UserService userService;
    private final EquipmentService equipmentService;
    private final SchedulingService schedulingService;
    private final MaintenanceService maintenanceService;
    private User currentUser;

    public FitnessCenterFacade(UserService userService, EquipmentService equipmentService,
                               SchedulingService schedulingService, MaintenanceService maintenanceService) {
        this.userService = userService;
        this.equipmentService = equipmentService;
        this.schedulingService = schedulingService;
        this.maintenanceService = maintenanceService;
    }

    public User login(String userId) {
        currentUser = userService.login(userId);
        return currentUser;
    }

    public void logout() {
        currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public Equipment addEquipment(String id, String name, String type, String location) {
        require(currentUser().canManageEquipment(), "add equipment");
        return equipmentService.addEquipment(id, name, type, location);
    }

    public Equipment editEquipment(String id, String name, String location) {
        require(currentUser().canManageEquipment(), "edit equipment");
        return equipmentService.editEquipment(id, name, location);
    }

    public Equipment changeEquipmentStatus(String id, EquipmentStatus status) {
        require(currentUser().canManageEquipment(), "change equipment status");
        return equipmentService.updateStatus(id, status);
    }

    public void deactivateEquipment(String id) {
        require(currentUser().canManageEquipment(), "deactivate equipment");
        equipmentService.deactivate(id);
    }

    public void reactivateEquipment(String id) {
        require(currentUser().canManageEquipment(), "reactivate equipment");
        equipmentService.reactivate(id);
    }

    public List<Equipment> listEquipment() {
        currentUser();
        return equipmentService.getAll();
    }

    public List<Equipment> getDueForMaintenance() {
        require(currentUser().canManageEquipment(), "view maintenance alerts");
        return equipmentService.getDueForMaintenance();
    }

    public void setThresholdStrategy(MaintenanceThresholdStrategy strategy) {
        require(currentUser().canManageEquipment(), "change maintenance rules");
        equipmentService.setThresholdStrategy(strategy);
    }

    public String getThresholdStrategyName() {
        return equipmentService.getThresholdStrategy().getName();
    }

    public boolean logUsage(String equipmentId, double hours) {
        require(currentUser().canScheduleSessions(), "log equipment usage");
        return equipmentService.logUsage(equipmentId, hours);
    }

    public List<MaintenanceRequest> getMaintenanceLog() {
        require(currentUser().canViewMaintenanceLog(), "view the maintenance log");
        return maintenanceService.getGlobalLog();
    }

    public List<MaintenanceRequest> getOpenRequests() {
        require(currentUser().canViewMaintenanceLog(), "view open maintenance requests");
        return maintenanceService.getOpenRequests();
    }

    public MaintenanceRequest reportFault(String equipmentId, String description, Urgency urgency) {
        require(currentUser().canReportFaults(), "report faults");
        return maintenanceService.reportFault(currentUser.getId(), equipmentId, description, urgency);
    }

    public List<MaintenanceRequest> getMyReports() {
        require(currentUser().canReportFaults(), "view fault reports");
        return maintenanceService.getReportedBy(currentUser.getId());
    }

    public MaintenanceRequest assignRequest(String requestId, String technician) {
        require(currentUser().canManageEquipment(), "assign maintenance tasks");
        return maintenanceService.assign(requestId, technician);
    }

    public MaintenanceRequest completeRequest(String requestId) {
        require(currentUser().canManageEquipment(), "complete maintenance tasks");
        return maintenanceService.complete(requestId);
    }

    public FitnessSession scheduleSession(FitnessSession.Builder builder) {
        require(currentUser().canScheduleSessions(), "schedule sessions");
        return schedulingService.createSession(withInstructor(builder));
    }

    public List<FitnessSession> scheduleRecurring(FitnessSession.Builder builder, int weeks) {
        require(currentUser().canScheduleSessions(), "schedule sessions");
        return schedulingService.createRecurring(withInstructor(builder), weeks);
    }

    public void cancelSession(String sessionId) {
        require(currentUser().canScheduleSessions(), "cancel sessions");
        FitnessSession session = schedulingService.getById(sessionId);
        if (currentUser.getRole() != Role.ADMINISTRATOR && !session.getInstructorId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("You can only cancel your own sessions");
        }
        schedulingService.cancelSession(sessionId);
    }

    public List<FitnessSession> getMySessions() {
        require(currentUser().canScheduleSessions(), "view teaching schedule");
        return schedulingService.getSessionsForInstructor(currentUser.getId());
    }

    public List<FitnessSession> getAllSessions() {
        currentUser();
        return schedulingService.getAllSessions();
    }

    public List<FitnessSession> getAvailableSessions() {
        currentUser();
        return schedulingService.getAvailableSessions();
    }

    public FitnessSession bookSession(String sessionId) {
        require(currentUser().canBookSessions(), "book sessions");
        return schedulingService.bookSession(currentUser.getId(), sessionId);
    }

    public List<FitnessSession> bookSeries(String sessionId) {
        require(currentUser().canBookSessions(), "book sessions");
        return schedulingService.bookSeries(currentUser.getId(), sessionId);
    }

    public void cancelBooking(String sessionId) {
        require(currentUser().canBookSessions(), "cancel bookings");
        schedulingService.cancelBooking(currentUser.getId(), sessionId);
    }

    public List<FitnessSession> getMyBookings() {
        require(currentUser().canBookSessions(), "view bookings");
        return schedulingService.getBookingsFor(currentUser.getId());
    }

    public User registerUser(Role role, String id, String name, String email, String extra) {
        require(currentUser().canManageUsers(), "register users");
        return userService.register(role, id, name, email, extra);
    }

    public List<User> listUsers() {
        require(currentUser().canManageUsers(), "view user accounts");
        return userService.getAll();
    }

    public void deactivateUser(String userId) {
        require(currentUser().canManageUsers(), "deactivate users");
        if (currentUser.getId().equalsIgnoreCase(userId.trim())) {
            throw new IllegalArgumentException("You cannot deactivate your own account");
        }
        userService.deactivate(userId);
    }

    public List<String> getNotifications() {
        return currentUser().getInbox();
    }

    public void clearNotifications() {
        currentUser().clearInbox();
    }

    private FitnessSession.Builder withInstructor(FitnessSession.Builder builder) {
        if (currentUser.getRole() == Role.INSTRUCTOR) {
            builder.instructor(currentUser.getId());
        }
        return builder;
    }

    private User currentUser() {
        if (currentUser == null) {
            throw new UnauthorizedAccessException("Please log in first");
        }
        return currentUser;
    }

    private void require(boolean allowed, String action) {
        if (!allowed) {
            throw new UnauthorizedAccessException(currentUser.getRole() + " is not allowed to " + action);
        }
    }
}
