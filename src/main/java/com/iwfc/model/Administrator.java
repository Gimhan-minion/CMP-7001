package com.iwfc.model;

// Inheritance: Administrator reuses everything from User and overrides the permission methods
public class Administrator extends User {

    public Administrator(String id, String name, String email) {
        super(id, name, email);
    }

    @Override
    public Role getRole() {
        return Role.ADMINISTRATOR;
    }

    @Override
    public String getMenuTitle() {
        return "Administrator Panel - " + getName();
    }

    @Override
    public boolean canManageEquipment() {
        return true;
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

    @Override
    public boolean canManageUsers() {
        return true;
    }
}
