package com.iwfc.pattern;

import com.iwfc.model.Administrator;
import com.iwfc.model.Instructor;
import com.iwfc.model.Member;
import com.iwfc.model.Role;
import com.iwfc.model.User;

public final class UserFactory {

    private UserFactory() {
    }

    public static User create(Role role, String id, String name, String email, String extra) {
        if (role == null) {
            throw new IllegalArgumentException("Role is required");
        }
        switch (role) {
            case ADMINISTRATOR:
                return new Administrator(id, name, email);
            case INSTRUCTOR:
                return new Instructor(id, name, email, extra);
            case MEMBER:
                return new Member(id, name, email, extra);
            default:
                throw new IllegalArgumentException("Unsupported role " + role);
        }
    }

    public static User create(Role role, String id, String name, String email) {
        return create(role, id, name, email, null);
    }
}
