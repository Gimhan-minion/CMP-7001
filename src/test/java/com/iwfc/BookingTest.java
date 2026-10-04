package com.iwfc;

import com.iwfc.exception.EntityNotFoundException;
import com.iwfc.exception.InvalidBookingException;
import com.iwfc.model.FitnessSession;
import com.iwfc.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingTest extends ServiceTestBase {

    private FitnessSession hiit;

    @BeforeEach
    void setUp() {
        hiit = schedulingService.createSession(session("HIIT Blast", "Studio A", 18, 19).capacity(2));
    }

    @Test
    void memberCanBookSession() {
        schedulingService.bookSession("M1", hiit.getId());

        assertTrue(hiit.hasMember("M1"));
        assertEquals(1, hiit.getAvailableSpots());
        assertEquals(1, schedulingService.getBookingsFor("M1").size());
    }

    @Test
    void bookingSendsConfirmationToMember() {
        User member = userService.getById("M1");
        member.clearInbox();

        schedulingService.bookSession("M1", hiit.getId());

        assertTrue(member.getInbox().stream().anyMatch(n -> n.contains("Booking confirmed")));
    }

    @Test
    void rejectsDuplicateBooking() {
        schedulingService.bookSession("M1", hiit.getId());

        assertThrows(InvalidBookingException.class, () -> schedulingService.bookSession("M1", hiit.getId()));
        assertEquals(1, hiit.getBookedMembers().size());
    }

    @Test
    void rejectsBookingWhenFull() {
        schedulingService.bookSession("M1", hiit.getId());
        schedulingService.bookSession("M2", hiit.getId());

        assertThrows(InvalidBookingException.class, () -> schedulingService.bookSession("M3", hiit.getId()));
    }

    @Test
    void rejectsMemberBookingTwoSessionsAtSameTime() {
        FitnessSession yoga = schedulingService.createSession(session("Yoga", "Studio C", 18, 19).instructor("I2"));
        schedulingService.bookSession("M1", hiit.getId());

        assertThrows(InvalidBookingException.class, () -> schedulingService.bookSession("M1", yoga.getId()));
    }

    @Test
    void rejectsUnknownSession() {
        assertThrows(EntityNotFoundException.class, () -> schedulingService.bookSession("M1", "S999"));
    }

    @Test
    void cancellingBookingFreesSeat() {
        schedulingService.bookSession("M1", hiit.getId());
        schedulingService.bookSession("M2", hiit.getId());

        schedulingService.cancelBooking("M1", hiit.getId());

        assertFalse(hiit.hasMember("M1"));
        assertEquals(1, hiit.getAvailableSpots());
    }

    @Test
    void cannotCancelBookingThatDoesNotExist() {
        assertThrows(InvalidBookingException.class, () -> schedulingService.cancelBooking("M1", hiit.getId()));
    }

    @Test
    void fullSessionsAreHiddenFromAvailableList() {
        schedulingService.bookSession("M1", hiit.getId());
        schedulingService.bookSession("M2", hiit.getId());

        assertTrue(schedulingService.getAvailableSessions().isEmpty());
    }

    @Test
    void cancellingSessionNotifiesBookedMembers() {
        schedulingService.bookSession("M1", hiit.getId());
        User member = userService.getById("M1");
        member.clearInbox();

        schedulingService.cancelSession(hiit.getId());

        assertTrue(schedulingService.getAllSessions().isEmpty());
        assertTrue(member.getInbox().get(0).contains("Session cancelled"));
    }

    @Test
    void memberCanBookWholeWeeklySeries() {
        List<FitnessSession> weekly = schedulingService.createRecurring(
                session("Monday Pilates", "Studio C", 7, 8).instructor("I2"), 4);

        List<FitnessSession> booked = schedulingService.bookSeries("M1", weekly.get(0).getId());

        assertEquals(4, booked.size());
        assertTrue(weekly.stream().allMatch(s -> s.hasMember("M1")));
    }

    @Test
    void seriesBookingStartsFromChosenWeek() {
        List<FitnessSession> weekly = schedulingService.createRecurring(
                session("Monday Pilates", "Studio C", 7, 8).instructor("I2"), 4);

        List<FitnessSession> booked = schedulingService.bookSeries("M1", weekly.get(2).getId());

        assertEquals(2, booked.size());
        assertFalse(weekly.get(0).hasMember("M1"));
    }

    @Test
    void seriesBookingIsAllOrNothing() {
        List<FitnessSession> weekly = schedulingService.createRecurring(
                session("Monday Pilates", "Studio C", 7, 8).instructor("I2").capacity(1), 3);
        schedulingService.bookSession("M2", weekly.get(1).getId());

        assertThrows(InvalidBookingException.class, () -> schedulingService.bookSeries("M1", weekly.get(0).getId()));
        assertFalse(weekly.get(0).hasMember("M1"));
        assertFalse(weekly.get(2).hasMember("M1"));
    }

    @Test
    void seriesBookingRejectsOneOffSession() {
        assertThrows(InvalidBookingException.class, () -> schedulingService.bookSeries("M1", hiit.getId()));
    }
}
