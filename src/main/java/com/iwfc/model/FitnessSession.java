package com.iwfc.model;

import com.iwfc.repository.Identifiable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class FitnessSession implements Identifiable<String> {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("EEE dd MMM HH:mm");

    private final String id;
    private final String title;
    private final SessionType type;
    private final String instructorId;
    private final String studio;
    private final List<String> equipmentIds;
    private final LocalDateTime start;
    private final LocalDateTime end;
    private final int capacity;
    private final String recurringGroupId;
    private final Set<String> bookedMembers;

    private FitnessSession(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.type = builder.type;
        this.instructorId = builder.instructorId;
        this.studio = builder.studio;
        this.equipmentIds = new ArrayList<>(builder.equipmentIds);
        this.start = builder.start;
        this.end = builder.end;
        this.capacity = builder.capacity;
        this.recurringGroupId = builder.recurringGroupId;
        this.bookedMembers = new LinkedHashSet<>();
    }

    public boolean overlaps(LocalDateTime otherStart, LocalDateTime otherEnd) {
        return start.isBefore(otherEnd) && otherStart.isBefore(end);
    }

    public boolean overlaps(FitnessSession other) {
        return overlaps(other.start, other.end);
    }

    public boolean usesEquipment(String equipmentId) {
        return equipmentIds.contains(equipmentId);
    }

    public boolean isFull() {
        return bookedMembers.size() >= capacity;
    }

    public int getAvailableSpots() {
        return capacity - bookedMembers.size();
    }

    public boolean hasMember(String memberId) {
        return bookedMembers.contains(memberId);
    }

    public boolean addMember(String memberId) {
        return bookedMembers.add(memberId);
    }

    public boolean removeMember(String memberId) {
        return bookedMembers.remove(memberId);
    }

    public Builder copyShiftedByWeeks(String newId, int weeks) {
        return new Builder()
                .id(newId)
                .title(title)
                .type(type)
                .instructor(instructorId)
                .studio(studio)
                .equipment(equipmentIds)
                .time(start.plusWeeks(weeks), end.plusWeeks(weeks))
                .capacity(capacity)
                .recurringGroup(recurringGroupId);
    }

    @Override
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public SessionType getType() {
        return type;
    }

    public String getInstructorId() {
        return instructorId;
    }

    public String getStudio() {
        return studio;
    }

    public List<String> getEquipmentIds() {
        return Collections.unmodifiableList(equipmentIds);
    }

    public LocalDateTime getStart() {
        return start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public int getCapacity() {
        return capacity;
    }

    public String getRecurringGroupId() {
        return recurringGroupId;
    }

    public boolean isRecurring() {
        return recurringGroupId != null;
    }

    public Set<String> getBookedMembers() {
        return Collections.unmodifiableSet(bookedMembers);
    }

    @Override
    public String toString() {
        return String.format("%-6s %-26s %-9s %-10s %s - %s  %d/%d booked%s",
                id, title, type, studio, start.format(FORMAT), end.toLocalTime(),
                bookedMembers.size(), capacity, isRecurring() ? "  (weekly)" : "");
    }

    // Builder pattern (creational): a session has many fields, so it is built step by step
    // and validated once in build() instead of using a long constructor
    public static class Builder {

        private String id;
        private String title;
        private SessionType type;
        private String instructorId;
        private String studio;
        private final List<String> equipmentIds = new ArrayList<>();
        private LocalDateTime start;
        private LocalDateTime end;
        private int capacity = 10;
        private String recurringGroupId;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder type(SessionType type) {
            this.type = type;
            return this;
        }

        public Builder instructor(String instructorId) {
            this.instructorId = instructorId;
            return this;
        }

        public Builder studio(String studio) {
            this.studio = studio;
            return this;
        }

        public Builder equipment(String... ids) {
            for (String equipmentId : ids) {
                equipmentIds.add(equipmentId.trim().toUpperCase());
            }
            return this;
        }

        public Builder equipment(List<String> ids) {
            return equipment(ids.toArray(new String[0]));
        }

        public Builder time(LocalDateTime start, LocalDateTime end) {
            this.start = start;
            this.end = end;
            return this;
        }

        public Builder capacity(int capacity) {
            this.capacity = capacity;
            return this;
        }

        public Builder recurringGroup(String recurringGroupId) {
            this.recurringGroupId = recurringGroupId;
            return this;
        }

        public FitnessSession build() {
            if (id == null || title == null || type == null || studio == null || instructorId == null) {
                throw new IllegalStateException("Session is missing required details");
            }
            if (start == null || end == null) {
                throw new IllegalStateException("Session start and end time are required");
            }
            if (capacity <= 0) {
                throw new IllegalStateException("Capacity must be at least 1");
            }
            return new FitnessSession(this);
        }
    }
}
