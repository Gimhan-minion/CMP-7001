package com.iwfc;

import com.iwfc.exception.DuplicateDataException;
import com.iwfc.exception.EntityNotFoundException;
import com.iwfc.exception.InvalidStatusTransitionException;
import com.iwfc.model.EquipmentStatus;
import com.iwfc.model.MaintenanceRequest;
import com.iwfc.model.RequestStatus;
import com.iwfc.model.Urgency;
import com.iwfc.model.User;
import com.iwfc.pattern.NotificationCenter;
import com.iwfc.pattern.NotificationListener;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaintenanceWorkflowTest extends ServiceTestBase {

    private MaintenanceRequest report() {
        return maintenanceService.reportFault("I1", "EQ1", "Spin bike 04 resistance failure", Urgency.HIGH);
    }

    @Test
    void newRequestStartsAsPendingAndMarksEquipmentFaulty() {
        MaintenanceRequest request = report();

        assertEquals(RequestStatus.PENDING, request.getStatus());
        assertEquals("EQ1", request.getEquipmentId());
        assertEquals(Urgency.HIGH, request.getUrgency());
        assertEquals(EquipmentStatus.FAULTY, equipmentService.getById("EQ1").getStatus());
    }

    @Test
    void fullWorkflowPendingAssignedCompleted() {
        MaintenanceRequest request = report();

        maintenanceService.assign(request.getId(), "TechFit Services");
        assertEquals(RequestStatus.ASSIGNED, request.getStatus());
        assertEquals("TechFit Services", request.getAssignedTo());
        assertEquals(EquipmentStatus.UNDER_MAINTENANCE, equipmentService.getById("EQ1").getStatus());

        maintenanceService.complete(request.getId());
        assertEquals(RequestStatus.COMPLETED, request.getStatus());
        assertEquals(EquipmentStatus.OPERATIONAL, equipmentService.getById("EQ1").getStatus());
        assertEquals(3, request.getHistory().size());
    }

    @Test
    void cannotCompleteBeforeAssigning() {
        MaintenanceRequest request = report();

        assertThrows(InvalidStatusTransitionException.class, () -> maintenanceService.complete(request.getId()));
        assertEquals(RequestStatus.PENDING, request.getStatus());
    }

    @Test
    void cannotReopenCompletedRequest() {
        MaintenanceRequest request = report();
        maintenanceService.assign(request.getId(), "Tech");
        maintenanceService.complete(request.getId());

        assertThrows(InvalidStatusTransitionException.class, () -> maintenanceService.assign(request.getId(), "Tech"));
        assertThrows(InvalidStatusTransitionException.class, () -> request.moveTo(RequestStatus.PENDING));
    }

    @Test
    void cannotAssignTwice() {
        MaintenanceRequest request = report();
        maintenanceService.assign(request.getId(), "Tech");

        assertThrows(InvalidStatusTransitionException.class, () -> maintenanceService.assign(request.getId(), "Other"));
    }

    @Test
    void statusTransitionRules() {
        assertTrue(RequestStatus.PENDING.canMoveTo(RequestStatus.ASSIGNED));
        assertTrue(RequestStatus.ASSIGNED.canMoveTo(RequestStatus.COMPLETED));
        assertFalse(RequestStatus.PENDING.canMoveTo(RequestStatus.COMPLETED));
        assertFalse(RequestStatus.COMPLETED.canMoveTo(RequestStatus.PENDING));
        assertFalse(RequestStatus.ASSIGNED.canMoveTo(RequestStatus.PENDING));
    }

    @Test
    void reporterIsNotifiedOnEveryStatusChange() {
        User instructor = userService.getById("I1");
        MaintenanceRequest request = report();
        instructor.clearInbox();

        maintenanceService.assign(request.getId(), "Tech");
        maintenanceService.complete(request.getId());

        List<String> inbox = instructor.getInbox();
        assertEquals(2, inbox.size());
        assertTrue(inbox.get(0).contains("ASSIGNED"));
        assertTrue(inbox.get(1).contains("COMPLETED"));
    }

    @Test
    void adminsAreNotifiedThroughObserver() {
        List<String> received = new ArrayList<>();
        NotificationListener listener = new NotificationListener() {
            @Override
            public String getId() {
                return "test-listener";
            }

            @Override
            public void onNotification(String message) {
                received.add(message);
            }
        };
        NotificationCenter.getInstance().subscribe(NotificationCenter.ADMINS, listener);

        MaintenanceRequest request = report();
        maintenanceService.assign(request.getId(), "Tech");
        maintenanceService.complete(request.getId());

        assertEquals(3, received.size());
        assertTrue(received.get(0).contains("HIGH urgency"));
    }

    @Test
    void completingRequestResetsServiceHours() {
        equipmentService.logUsage("EQ1", 120);
        MaintenanceRequest request = report();
        maintenanceService.assign(request.getId(), "Tech");
        maintenanceService.complete(request.getId());

        assertEquals(0, equipmentService.getById("EQ1").getHoursSinceService());
        assertEquals(120, equipmentService.getById("EQ1").getUsageHours());
    }

    @Test
    void rejectsSecondOpenRequestForSameEquipment() {
        report();

        assertThrows(DuplicateDataException.class,
                () -> maintenanceService.reportFault("I2", "EQ1", "Still broken", Urgency.LOW));
    }

    @Test
    void canReportAgainAfterCompletion() {
        MaintenanceRequest first = report();
        maintenanceService.assign(first.getId(), "Tech");
        maintenanceService.complete(first.getId());

        report();

        assertEquals(2, maintenanceService.getGlobalLog().size());
    }

    @Test
    void rejectsFaultForUnknownEquipment() {
        assertThrows(EntityNotFoundException.class,
                () -> maintenanceService.reportFault("I1", "EQ99", "Broken", Urgency.LOW));
    }

    @Test
    void openRequestsAreOrderedByUrgency() {
        maintenanceService.reportFault("I1", "EQ2", "Loose seat", Urgency.LOW);
        maintenanceService.reportFault("I1", "EQ3", "Belt snapped", Urgency.HIGH);

        List<MaintenanceRequest> open = maintenanceService.getOpenRequests();

        assertEquals(Urgency.HIGH, open.get(0).getUrgency());
        assertEquals(Urgency.LOW, open.get(1).getUrgency());
    }
}
