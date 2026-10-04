package com.iwfc;

import com.iwfc.exception.UnauthorizedAccessException;
import com.iwfc.model.FitnessSession;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("failing")
class IntentionalFailureTest extends ServiceTestBase {

    @Test
    void doubleBookingShouldThrowUnauthorizedAccess() {
        FitnessSession hiit = schedulingService.createSession(session("HIIT Blast", "Studio A", 18, 19));
        schedulingService.bookSession("M1", hiit.getId());

        assertThrows(UnauthorizedAccessException.class, () -> schedulingService.bookSession("M1", hiit.getId()));
    }
}
