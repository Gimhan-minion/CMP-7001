package com.iwfc.model;

public enum RequestStatus {
    PENDING,
    ASSIGNED,
    COMPLETED;

    // Workflow rule: PENDING -> ASSIGNED -> COMPLETED, any other move is rejected
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
