package test.java.rbac.util;

import org.junit.jupiter.api.Test;
import rbac.util.ValidationUtils;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilsTest {

    @Test
    void validatesUsernameAndEmail() {
        assertTrue(ValidationUtils.isValidUsername("user_1"));
        assertFalse(ValidationUtils.isValidUsername("ab"));

        assertTrue(ValidationUtils.isValidEmail("admin@company.com"));
        assertFalse(ValidationUtils.isValidEmail("invalid@"));
    }

    @Test
    void validatesDateFormat() {
        assertTrue(ValidationUtils.isValidDate("2024-01-15"));
        assertFalse(ValidationUtils.isValidDate("2024/01/15"));
    }

    @Test
    void normalizesAndRequiresNonEmpty() {
        assertEquals("John Doe", ValidationUtils.normalizeString("  John   Doe  "));
        assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.requireNonEmpty(" ", "Field"));
    }
}
