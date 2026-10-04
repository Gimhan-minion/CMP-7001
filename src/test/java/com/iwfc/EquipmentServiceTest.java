package com.iwfc;

import com.iwfc.exception.DuplicateDataException;
import com.iwfc.exception.EntityNotFoundException;
import com.iwfc.model.Equipment;
import com.iwfc.model.EquipmentStatus;
import com.iwfc.model.User;
import com.iwfc.pattern.EarlyWarningStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EquipmentServiceTest extends ServiceTestBase {

    @Test
    void addsEquipmentAsOperational() {
        Equipment rower = equipmentService.addEquipment("EQ4", "Rowing Machine 01", "Rowing Machine", "Cardio Zone");

        assertEquals(EquipmentStatus.OPERATIONAL, rower.getStatus());
        assertTrue(rower.isActive());
        assertEquals(4, equipmentService.getAll().size());
    }

    @Test
    void rejectsDuplicateEquipmentId() {
        DuplicateDataException ex = assertThrows(DuplicateDataException.class,
                () -> equipmentService.addEquipment("EQ1", "Another Bike", "Spin Bike", "Studio B"));
        assertTrue(ex.getMessage().contains("EQ1"));
    }

    @Test
    void duplicateCheckIgnoresCase() {
        assertThrows(DuplicateDataException.class,
                () -> equipmentService.addEquipment("eq1", "Another Bike", "Spin Bike", "Studio B"));
    }

    @Test
    void editsNameAndLocation() {
        equipmentService.editEquipment("EQ3", "Treadmill Pro", "Studio A");

        Equipment treadmill = equipmentService.getById("EQ3");
        assertEquals("Treadmill Pro", treadmill.getName());
        assertEquals("Studio A", treadmill.getLocation());
    }

    @Test
    void blankEditKeepsOldValues() {
        equipmentService.editEquipment("EQ3", "", "  ");

        assertEquals("Treadmill 01", equipmentService.getById("EQ3").getName());
        assertEquals("Cardio Zone", equipmentService.getById("EQ3").getLocation());
    }

    @Test
    void deactivatedEquipmentIsNotAvailable() {
        equipmentService.deactivate("EQ2");

        assertFalse(equipmentService.getById("EQ2").isAvailable());
        assertEquals(2, equipmentService.getAvailable().size());
    }

    @Test
    void editingUnknownEquipmentThrows() {
        assertThrows(EntityNotFoundException.class, () -> equipmentService.editEquipment("EQ99", "x", "y"));
    }

    @Test
    void tracksCumulativeUsageHours() {
        equipmentService.logUsage("EQ3", 2.5);
        equipmentService.logUsage("EQ3", 1.5);

        assertEquals(4.0, equipmentService.getById("EQ3").getUsageHours());
    }

    @Test
    void rejectsInvalidUsageHours() {
        assertThrows(IllegalArgumentException.class, () -> equipmentService.logUsage("EQ3", -2));
    }

    @Test
    void alertsAdminsWhenServiceLimitReached() {
        User admin = userService.getById("A1");
        admin.clearInbox();

        assertFalse(equipmentService.logUsage("EQ1", 299));
        assertTrue(equipmentService.logUsage("EQ1", 2));

        assertEquals(1, admin.getInbox().size());
        assertTrue(admin.getInbox().get(0).contains("Preventative maintenance"));
        assertEquals(1, equipmentService.getDueForMaintenance().size());
    }

    @Test
    void alertIsOnlySentOnceUntilServiced() {
        assertTrue(equipmentService.logUsage("EQ1", 310));
        assertFalse(equipmentService.logUsage("EQ1", 5));

        equipmentService.markServiced("EQ1");
        assertTrue(equipmentService.logUsage("EQ1", 300));
    }

    @Test
    void earlyWarningStrategyAlertsSooner() {
        equipmentService.setThresholdStrategy(new EarlyWarningStrategy(0.8));

        assertTrue(equipmentService.logUsage("EQ3", 400));
    }

    @Test
    void loggingUsageForUnknownEquipmentThrows() {
        assertThrows(EntityNotFoundException.class, () -> equipmentService.logUsage("EQ99", 3));
    }

    @Test
    void deactivatingUnknownEquipmentThrows() {
        assertThrows(EntityNotFoundException.class, () -> equipmentService.deactivate("EQ99"));
    }

    @Test
    void cannotLogUsageOnDeactivatedEquipment() {
        equipmentService.deactivate("EQ3");

        assertThrows(IllegalStateException.class, () -> equipmentService.logUsage("EQ3", 1));
    }

    @Test
    void maintenanceListExcludesDeactivatedEquipment() {
        equipmentService.logUsage("EQ1", 350);
        equipmentService.deactivate("EQ1");

        assertTrue(equipmentService.getDueForMaintenance().isEmpty());
    }

    @Test
    void changesEquipmentStatus() {
        equipmentService.updateStatus("EQ2", EquipmentStatus.UNDER_MAINTENANCE);

        assertEquals(EquipmentStatus.UNDER_MAINTENANCE, equipmentService.getById("EQ2").getStatus());
        assertEquals(1, equipmentService.getByStatus(EquipmentStatus.UNDER_MAINTENANCE).size());
    }
}
