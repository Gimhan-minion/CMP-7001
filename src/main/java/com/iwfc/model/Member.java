package com.iwfc.model;

public class Member extends User {

    private String membershipPlan;

    public Member(String id, String name, String email, String membershipPlan) {
        super(id, name, email);
        this.membershipPlan = membershipPlan == null ? "Standard" : membershipPlan;
    }

    @Override
    public Role getRole() {
        return Role.MEMBER;
    }

    @Override
    public String getMenuTitle() {
        return "Member Area - " + getName() + " [" + membershipPlan + "]";
    }

    @Override
    public boolean canManageEquipment() {
        return false;
    }

    @Override
    public boolean canScheduleSessions() {
        return false;
    }

    @Override
    public boolean canBookSessions() {
        return true;
    }

    @Override
    public boolean canReportFaults() {
        return false;
    }

    public String getMembershipPlan() {
        return membershipPlan;
    }

    public void setMembershipPlan(String membershipPlan) {
        this.membershipPlan = membershipPlan;
    }
}
