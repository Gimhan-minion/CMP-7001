package com.iwfc.model;

public class Instructor extends User {

    private String specialisation;

    public Instructor(String id, String name, String email, String specialisation) {
        super(id, name, email);
        this.specialisation = specialisation == null ? "General" : specialisation;
    }

    @Override
    public Role getRole() {
        return Role.INSTRUCTOR;
    }

    @Override
    public String getMenuTitle() {
        return "Instructor Panel - " + getName() + " (" + specialisation + ")";
    }

    @Override
    public boolean canManageEquipment() {
        return false;
    }

    @Override
    public boolean canScheduleSessions() {
        return true;
    }

    @Override
    public boolean canBookSessions() {
        return false;
    }

    @Override
    public boolean canReportFaults() {
        return true;
    }

    public String getSpecialisation() {
        return specialisation;
    }

    public void setSpecialisation(String specialisation) {
        this.specialisation = specialisation;
    }
}
