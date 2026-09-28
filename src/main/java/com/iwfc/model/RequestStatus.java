package com.iwfc.model;

public enum RequestStatus {
    PENDING,
    ASSIGNED,
    COMPLETED;

    public boolean canMoveTo(RequestStatus next) {
        switch (this) {
            case PENDING:
                return next == ASSIGNED;
            case ASSIGNED:
                return next == COMPLETED;
            default:
                return false;
        }
    }
}
