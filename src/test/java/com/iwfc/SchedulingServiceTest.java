package com.iwfc;

import com.iwfc.exception.EntityNotFoundException;
import com.iwfc.exception.InvalidBookingException;
import com.iwfc.model.EquipmentStatus;
import com.iwfc.model.FitnessSession;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchedulingServiceTest extends ServiceTestBase {

    @Test
    void createsValidSession() {
        FitnessSession created = schedulingService.createSession(session("HIIT Blast", "Studio A", 18, 19));

        assertNotNull(created.getId());
        assertEquals(1, schedulingService.getAllSessions().size());
        assertEquals("I1", created.getInstructorId());
    }

    @Test
    void rejectsSameStudioAtSameTime() {
        schedulingService.createSession(session("HIIT Blast", "Studio A", 18, 19));
        FitnessSession.Builder clash = session("Yoga Flow", "Studio A", 18, 19).instructor("I2");

        InvalidBookingException ex = assertThrows(InvalidBookingException.class,
                () -> schedulingService.createSession(clash));
        assertTrue(ex.getMessage().contains("Studio A"));
    }

    @Test
    void rejectsPartiallyOverlappingStudioBooking() {
        schedulingService.createSession(session("HIIT Blast", "Studio A", 18, 20));

        assertThrows(InvalidBookingException.class,
                () -> schedulingService.createSession(session("Late Yoga", "Studio A", 19, 21).instructor("I2")));
    }

    @Test
    void rejectsSameEquipmentInDifferentStudio() {
        schedulingService.createSession(session("Spin", "Studio B", 9, 10).equipment("EQ1"));
        FitnessSession.Builder clash = session("Cardio Mix", "Cardio Zone", 9, 10).instructor("I2").equipment("EQ1");

        InvalidBookingException ex = assertThrows(InvalidBookingException.class,
                () -> schedulingService.createSession(clash));
        assertTrue(ex.getMessage().contains("EQ1"));
    }

    @Test
    void rejectsInstructorTeachingTwoClassesAtOnce() {
        schedulingService.createSession(session("HIIT Blast", "Studio A", 18, 19));

        assertThrows(InvalidBookingException.class,
                () -> schedulingService.createSession(session("HIIT Two", "Studio C", 18, 19)));
    }

    @Test
    void allowsBackToBackSessionsInSameStudio() {
        schedulingService.createSession(session("Morning", "Studio A", 8, 9));

        assertDoesNotThrow(() -> schedulingService.createSession(session("Next", "Studio A", 9, 10)));
    }

    @Test
    void rejectsSessionBeforeOpeningTime() {
        FitnessSession.Builder early = session("Early", "Studio A", 5, 6);

        assertThrows(InvalidBookingException.class, () -> schedulingService.createSession(early));
    }

    @Test
    void rejectsSessionAfterClosingTime() {
        FitnessSession.Builder late = session("Late", "Studio A", 21, 23);

        assertThrows(InvalidBookingException.class, () -> schedulingService.createSession(late));
    }

    @Test
    void rejectsEndTimeBeforeStartTime() {
        FitnessSession.Builder backwards = session("Backwards", "Studio A", 10, 9);

        assertThrows(InvalidBookingException.class, () -> schedulingService.createSession(backwards));
    }

    @Test
    void rejectsSessionInThePast() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        FitnessSession.Builder past = session("Old", "Studio A", 9, 10)
                .time(yesterday.atTime(9, 0), yesterday.atTime(10, 0));

        assertThrows(InvalidBookingException.class, () -> schedulingService.createSession(past));
    }

    @Test
    void rejectsFaultyEquipment() {
        equipmentService.updateStatus("EQ1", EquipmentStatus.FAULTY);

        assertThrows(InvalidBookingException.class,
                () -> schedulingService.createSession(session("Spin", "Studio B", 9, 10).equipment("EQ1")));
    }

    @Test
    void rejectsDeactivatedEquipment() {
        equipmentService.deactivate("EQ2");

        assertThrows(InvalidBookingException.class,
                () -> schedulingService.createSession(session("Spin", "Studio B", 9, 10).equipment("EQ2")));
    }

    @Test
    void rejectsUnknownEquipment() {
        assertThrows(EntityNotFoundException.class,
                () -> schedulingService.createSession(session("Spin", "Studio B", 9, 10).equipment("NOPE")));
    }

    @Test
    void createsWeeklyRecurringSessions() {
        List<FitnessSession> series = schedulingService.createRecurring(session("Monday Pilates", "Studio A", 7, 8), 4);

        assertEquals(4, series.size());
        assertEquals(4, schedulingService.getAllSessions().size());
        for (int i = 1; i < series.size(); i++) {
            assertEquals(series.get(0).getStart().plusWeeks(i), series.get(i).getStart());
            assertEquals(series.get(0).getRecurringGroupId(), series.get(i).getRecurringGroupId());
        }
    }

    @Test
    void recurringClashRollsBackWholeSeries() {
        LocalDate thirdWeek = day.plusWeeks(2);
        schedulingService.createSession(session("One Off", "Studio A", 7, 8).instructor("I2")
                .time(thirdWeek.atTime(7, 0), thirdWeek.atTime(8, 0)));

        assertThrows(InvalidBookingException.class,
                () -> schedulingService.createRecurring(session("Monday Pilates", "Studio A", 7, 8), 4));
        assertEquals(1, schedulingService.getAllSessions().size());
    }

    @Test
    void rejectsInvalidNumberOfWeeks() {
        assertThrows(InvalidBookingException.class,
                () -> schedulingService.createRecurring(session("Pilates", "Studio A", 7, 8), 1));
        assertThrows(InvalidBookingException.class,
                () -> schedulingService.createRecurring(session("Pilates", "Studio A", 7, 8), 20));
    }
}
