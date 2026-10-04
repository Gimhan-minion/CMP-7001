package com.iwfc.service;

import com.iwfc.exception.InvalidBookingException;
import com.iwfc.model.Equipment;
import com.iwfc.model.FitnessSession;
import com.iwfc.pattern.NotificationCenter;
import com.iwfc.repository.Repository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SchedulingService {

    public static final LocalTime OPENING_TIME = LocalTime.of(6, 0);
    public static final LocalTime CLOSING_TIME = LocalTime.of(22, 0);
    public static final int MAX_RECURRING_WEEKS = 12;

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("EEE dd MMM HH:mm");

    private final Repository<FitnessSession, String> sessionRepo;
    private final Repository<Equipment, String> equipmentRepo;
    private final NotificationCenter notifications;
    private int sessionCounter = 0;
    private int groupCounter = 0;

    public SchedulingService(Repository<FitnessSession, String> sessionRepo, Repository<Equipment, String> equipmentRepo) {
        this.sessionRepo = sessionRepo;
        this.equipmentRepo = equipmentRepo;
        this.notifications = NotificationCenter.getInstance();
    }

    public FitnessSession createSession(FitnessSession.Builder builder) {
        FitnessSession session = builder.id(nextSessionId()).build();
        validate(session, new ArrayList<>());
        sessionRepo.add(session);
        notifications.publish(NotificationCenter.MEMBERS, "New session available: " + describe(session));
        return session;
    }

    public List<FitnessSession> createRecurring(FitnessSession.Builder builder, int weeks) {
        if (weeks < 2 || weeks > MAX_RECURRING_WEEKS) {
            throw new InvalidBookingException("Recurring sessions must run between 2 and " + MAX_RECURRING_WEEKS + " weeks");
        }
        String groupId = "R" + (++groupCounter);
        FitnessSession first = builder.id(nextSessionId()).recurringGroup(groupId).build();

        List<FitnessSession> series = new ArrayList<>();
        series.add(first);
        for (int week = 1; week < weeks; week++) {
            series.add(first.copyShiftedByWeeks(nextSessionId(), week).build());
        }

        List<FitnessSession> checked = new ArrayList<>();
        for (FitnessSession occurrence : series) {
            validate(occurrence, checked);
            checked.add(occurrence);
        }
        series.forEach(sessionRepo::add);
        notifications.publish(NotificationCenter.MEMBERS, "New weekly class: " + first.getTitle()
                + " every " + first.getStart().getDayOfWeek() + " for " + weeks + " weeks");
        return series;
    }

    public void cancelSession(String sessionId) {
        FitnessSession session = sessionRepo.getById(normalise(sessionId));
        for (String memberId : session.getBookedMembers()) {
            notifications.notifyUser(memberId, "Session cancelled: " + describe(session));
        }
        sessionRepo.delete(session.getId());
    }

    public FitnessSession bookSession(String memberId, String sessionId) {
        FitnessSession session = sessionRepo.getById(normalise(sessionId));
        checkCanBook(memberId, session);
        session.addMember(memberId);
        notifications.notifyUser(memberId, "Booking confirmed: " + describe(session));
        notifications.notifyUser(session.getInstructorId(),
                memberId + " booked " + session.getTitle() + " (" + session.getAvailableSpots() + " spots left)");
        return session;
    }

    public List<FitnessSession> bookSeries(String memberId, String sessionId) {
        FitnessSession first = sessionRepo.getById(normalise(sessionId));
        if (!first.isRecurring()) {
            throw new InvalidBookingException(first.getId() + " is not part of a weekly series");
        }
        List<FitnessSession> series = sorted(sessionRepo.findBy(s -> first.getRecurringGroupId().equals(s.getRecurringGroupId())
                && !s.getStart().isBefore(first.getStart())));
        for (FitnessSession occurrence : series) {
            checkCanBook(memberId, occurrence);
        }
        series.forEach(s -> s.addMember(memberId));
        notifications.notifyUser(memberId, "Booked " + series.size() + " weeks of " + first.getTitle()
                + " starting " + first.getStart().format(FORMAT));
        notifications.notifyUser(first.getInstructorId(),
                memberId + " booked the full " + first.getTitle() + " series");
        return series;
    }

    public void cancelBooking(String memberId, String sessionId) {
        FitnessSession session = sessionRepo.getById(normalise(sessionId));
        if (!session.removeMember(memberId)) {
            throw new InvalidBookingException("No booking found for " + session.getId());
        }
        notifications.notifyUser(memberId, "Booking cancelled: " + describe(session));
    }

    public List<FitnessSession> getAllSessions() {
        return sorted(sessionRepo.findAll());
    }

    public List<FitnessSession> getUpcomingSessions() {
        LocalDateTime now = LocalDateTime.now();
        return sorted(sessionRepo.findBy(s -> s.getStart().isAfter(now)));
    }

    public List<FitnessSession> getAvailableSessions() {
        return getUpcomingSessions().stream().filter(s -> !s.isFull()).collect(Collectors.toList());
    }

    public List<FitnessSession> getBookingsFor(String memberId) {
        return sorted(sessionRepo.findBy(s -> s.hasMember(memberId)));
    }

    public List<FitnessSession> getSessionsForInstructor(String instructorId) {
        return sorted(sessionRepo.findBy(s -> s.getInstructorId().equals(instructorId)));
    }

    public FitnessSession getById(String sessionId) {
        return sessionRepo.getById(normalise(sessionId));
    }

    private void checkCanBook(String memberId, FitnessSession session) {
        if (!session.getStart().isAfter(LocalDateTime.now())) {
            throw new InvalidBookingException("Session " + session.getId() + " has already started");
        }
        if (session.hasMember(memberId)) {
            throw new InvalidBookingException("You have already booked " + session.getId());
        }
        if (session.isFull()) {
            throw new InvalidBookingException("Session " + session.getId() + " is fully booked");
        }
        for (FitnessSession booked : getBookingsFor(memberId)) {
            if (booked.overlaps(session)) {
                throw new InvalidBookingException("Time clash with your booking " + booked.getId()
                        + " (" + booked.getTitle() + ")");
            }
        }
    }

    // Double booking prevention: checks operating hours, equipment state and any overlapping
    // session using the same studio, equipment or instructor
    private void validate(FitnessSession session, List<FitnessSession> pending) {
        LocalDateTime start = session.getStart();
        LocalDateTime end = session.getEnd();

        if (!end.isAfter(start)) {
            throw new InvalidBookingException("End time must be after start time");
        }
        if (!start.toLocalDate().equals(end.toLocalDate())) {
            throw new InvalidBookingException("A session must start and finish on the same day");
        }
        if (start.toLocalTime().isBefore(OPENING_TIME) || end.toLocalTime().isAfter(CLOSING_TIME)) {
            throw new InvalidBookingException("Sessions must be within operating hours "
                    + OPENING_TIME + " - " + CLOSING_TIME);
        }
        if (start.isBefore(LocalDateTime.now())) {
            throw new InvalidBookingException("Cannot schedule a session in the past");
        }

        for (String equipmentId : session.getEquipmentIds()) {
            Equipment equipment = equipmentRepo.getById(equipmentId);
            if (!equipment.isAvailable()) {
                throw new InvalidBookingException(equipment.getName() + " (" + equipmentId + ") is not available - "
                        + (equipment.isActive() ? equipment.getStatus() : "DEACTIVATED"));
            }
        }

        List<FitnessSession> others = new ArrayList<>(sessionRepo.findAll());
        others.addAll(pending);
        for (FitnessSession other : others) {
            if (!other.overlaps(session)) {
                continue;
            }
            if (other.getStudio().equalsIgnoreCase(session.getStudio())) {
                throw new InvalidBookingException(session.getStudio() + " is already booked for "
                        + other.getTitle() + " (" + other.getId() + ") at " + other.getStart().format(FORMAT));
            }
            for (String equipmentId : session.getEquipmentIds()) {
                if (other.usesEquipment(equipmentId)) {
                    throw new InvalidBookingException("Equipment " + equipmentId + " is already in use by "
                            + other.getTitle() + " (" + other.getId() + ") at " + other.getStart().format(FORMAT));
                }
            }
            if (other.getInstructorId().equals(session.getInstructorId())) {
                throw new InvalidBookingException("Instructor " + session.getInstructorId()
                        + " is already teaching " + other.getTitle() + " at that time");
            }
        }
    }

    private String nextSessionId() {
        String id;
        do {
            id = String.format("S%03d", ++sessionCounter);
        } while (sessionRepo.existsById(id));
        return id;
    }

    private List<FitnessSession> sorted(List<FitnessSession> sessions) {
        sessions.sort(Comparator.comparing(FitnessSession::getStart));
        return sessions;
    }

    private String describe(FitnessSession session) {
        return session.getTitle() + " (" + session.getId() + ") on " + session.getStart().format(FORMAT)
                + " in " + session.getStudio();
    }

    private String normalise(String id) {
        return id == null ? "" : id.trim().toUpperCase();
    }
}
