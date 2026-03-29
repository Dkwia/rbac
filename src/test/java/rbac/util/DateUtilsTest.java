package test.java.rbac.util;

import org.junit.jupiter.api.Test;
import rbac.util.DateUtils;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilsTest {

    @Test
    void comparesDatesLexicographically() {
        assertTrue(DateUtils.isBefore("2024-01-01", "2024-02-01"));
        assertTrue(DateUtils.isAfter("2024-02-01", "2024-01-01"));
    }

    @Test
    void addsDaysWithSimpleCalendar() {
        assertEquals("2024-02-04", DateUtils.addDays("2024-01-30", 5));
        assertEquals("2023-12-31", DateUtils.addDays("2024-01-01", -1));
    }

    @Test
    void formatsRelativeTime() {
        String today = DateUtils.getCurrentDate();
        assertEquals("today", DateUtils.formatRelativeTime(today));
        String future = DateUtils.addDays(today, 2);
        assertEquals("in 2 days", DateUtils.formatRelativeTime(future));
    }
}
