package com.iwfc;

import com.iwfc.exception.DuplicateDataException;
import com.iwfc.exception.UnauthorizedAccessException;
import com.iwfc.model.Administrator;
import com.iwfc.model.FitnessSession;
import com.iwfc.model.Instructor;
import com.iwfc.model.Member;
import com.iwfc.model.Role;
import com.iwfc.model.Urgency;
import com.iwfc.model.User;
import com.iwfc.pattern.UserFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccessControlTest extends ServiceTestBase {

    @Test
    void memberCannotViewMaintenanceLog() {
        facade.login("M1");

        assertThrows(UnauthorizedAccessException.class, () -> facade.getMaintenanceLog());
    }

    @Test
    void memberCannotAddEquipment() {
        facade.login("M1");

        assertThrows(UnauthorizedAccessException.class,
                () -> facade.addEquipment("EQ9", "Bike", "Spin Bike", "Studio B"));
        assertEquals(3, equipmentService.getAll().size());
    }

    @Test
    void memberCannotScheduleSessions() {
        facade.login("M1");

        assertThrows(UnauthorizedAccessException.class,
                () -> facade.scheduleSession(session("Sneaky", "Studio A", 10, 11)));
    }

    @Test
    void memberCannotReportFaults() {
        facade.login("M2");

        assertThrows(UnauthorizedAccessException.class,
                () -> facade.reportFault("EQ1", "Broken", Urgency.LOW));
    }

    @Test
    void instructorCannotManageEquipmentOrViewLog() {
        facade.login("I1");

        assertThrows(UnauthorizedAccessException.class, () -> facade.getMaintenanceLog());
        assertThrows(UnauthorizedAccessException.class, () -> facade.deactivateEquipment("EQ1"));
        assertThrows(UnauthorizedAccessException.class, () -> facade.listUsers());
    }

    @Test
    void instructorCannotBookAsMember() {
        FitnessSession hiit = schedulingService.createSession(session("HIIT", "Studio A", 18, 19));
        facade.login("I2");

        assertThrows(UnauthorizedAccessException.class, () -> facade.bookSession(hiit.getId()));
    }

    @Test
    void instructorCannotCancelAnotherInstructorsSession() {
        FitnessSession hiit = schedulingService.createSession(session("HIIT", "Studio A", 18, 19));
        facade.login("I2");

        assertThrows(UnauthorizedAccessException.class, () -> facade.cancelSession(hiit.getId()));
    }

    @Test
    void instructorSessionsAreAlwaysAssignedToThemselves() {
        facade.login("I2");

        FitnessSession yoga = facade.scheduleSession(session("Yoga", "Studio C", 7, 8).instructor("I1"));

        assertEquals("I2", yoga.getInstructorId());
    }

    @Test
    void adminCanViewLogAndManageEquipment() {
        facade.login("A1");

        assertDoesNotThrow(() -> facade.getMaintenanceLog());
        assertDoesNotThrow(() -> facade.addEquipment("EQ9", "Rower", "Rowing Machine", "Cardio Zone"));
        assertEquals(4, facade.listEquipment().size());
    }

    @Test
    void actionsRequireLogin() {
        assertThrows(UnauthorizedAccessException.class, () -> facade.getAvailableSessions());
    }

    @Test
    void deactivatedUserCannotLogIn() {
        userService.deactivate("M3");

        assertThrows(UnauthorizedAccessException.class, () -> facade.login("M3"));
    }

    @Test
    void rejectsDuplicateUserId() {
        facade.login("A1");

        assertThrows(DuplicateDataException.class,
                () -> facade.registerUser(Role.MEMBER, "M1", "Someone Else", "else@gmail.com", "Standard"));
    }

    @Test
    void factoryCreatesCorrectUserType() {
        User admin = UserFactory.create(Role.ADMINISTRATOR, "X1", "Admin", "a@x.com");
        User instructor = UserFactory.create(Role.INSTRUCTOR, "X2", "Coach", "c@x.com", "Spin");
        User member = UserFactory.create(Role.MEMBER, "X3", "Client", "m@x.com", "Gold");

        assertInstanceOf(Administrator.class, admin);
        assertInstanceOf(Instructor.class, instructor);
        assertInstanceOf(Member.class, member);
        assertEquals(true, admin.canViewMaintenanceLog());
        assertEquals(false, member.canViewMaintenanceLog());
    }

    @Test
    void memberCannotManageUsers() {
        facade.login("M1");

        assertThrows(UnauthorizedAccessException.class, () -> facade.listUsers());
        assertThrows(UnauthorizedAccessException.class,
                () -> facade.registerUser(Role.MEMBER, "M9", "New Person", "new@gmail.com", "Standard"));
    }

    @Test
    void adminCanRegisterAndListUsers() {
        facade.login("A1");

        facade.registerUser(Role.INSTRUCTOR, "I9", "New Coach", "coach@iwfc.lk", "Spin");

        assertEquals(7, facade.listUsers().size());
    }

    @Test
    void adminCannotDeactivateOwnAccount() {
        facade.login("A1");

        assertThrows(IllegalArgumentException.class, () -> facade.deactivateUser("A1"));
    }
}
