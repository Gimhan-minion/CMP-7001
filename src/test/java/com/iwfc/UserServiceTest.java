package com.iwfc;

import com.iwfc.exception.DuplicateDataException;
import com.iwfc.exception.EntityNotFoundException;
import com.iwfc.model.Instructor;
import com.iwfc.model.Role;
import com.iwfc.model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserServiceTest extends ServiceTestBase {

    @Test
    void registersAndFindsUser() {
        userService.register(Role.INSTRUCTOR, "I5", "Nuwan Silva", "nuwan@iwfc.lk", "Strength");

        User found = userService.getById("i5");
        assertInstanceOf(Instructor.class, found);
        assertEquals("Nuwan Silva", found.getName());
    }

    @Test
    void rejectsDuplicateUserId() {
        assertThrows(DuplicateDataException.class,
                () -> userService.register(Role.MEMBER, "M1", "Copy", "copy@gmail.com", "Standard"));
    }

    @Test
    void listsUsersByRole() {
        assertEquals(6, userService.getAll().size());
        assertEquals(3, userService.getByRole(Role.MEMBER).size());
        assertEquals(2, userService.getByRole(Role.INSTRUCTOR).size());
    }

    @Test
    void unknownUserCannotLogIn() {
        assertThrows(EntityNotFoundException.class, () -> userService.login("Z9"));
    }

    @Test
    void reactivatedUserCanLogInAgain() {
        userService.deactivate("M2");
        userService.reactivate("M2");

        assertEquals("M2", userService.login("M2").getId());
    }

    @Test
    void rejectsInvalidEmail() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.register(Role.MEMBER, "M7", "Bad Email", "not-an-email", "Standard"));
    }
}
