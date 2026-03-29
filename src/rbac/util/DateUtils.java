package rbac.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateUtils {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateUtils() {
    }

    public static String getCurrentDate() {
        return LocalDate.now().format(DATE_FORMAT);
    }

    public static String getCurrentDateTime() {
        return LocalDateTime.now().format(DATETIME_FORMAT);
    }

    public static boolean isBefore(String date1, String date2) {
        if (date1 == null || date2 == null) return false;
        return date1.compareTo(date2) < 0;
    }

    public static boolean isAfter(String date1, String date2) {
        if (date1 == null || date2 == null) return false;
        return date1.compareTo(date2) > 0;
    }

    public static String addDays(String date, int days) {
        ValidationUtils.requireNonEmpty(date, "Date");
        if (!ValidationUtils.isValidDate(date)) {
            throw new IllegalArgumentException("Invalid date format");
        }
        String[] parts = date.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        int day = Integer.parseInt(parts[2]) + days;

        while (day > 31) {
            day -= 31;
            month += 1;
            if (month > 12) {
                month = 1;
                year += 1;
            }
        }
        while (day <= 0) {
            day += 31;
            month -= 1;
            if (month <= 0) {
                month = 12;
                year -= 1;
            }
        }

        return "%04d-%02d-%02d".formatted(year, month, day);
    }

    public static String formatRelativeTime(String date) {
        ValidationUtils.requireNonEmpty(date, "Date");
        if (!ValidationUtils.isValidDate(date)) {
            throw new IllegalArgumentException("Invalid date format");
        }
        int diff = toDays(date) - toDays(getCurrentDate());
        if (diff == 0) {
            return "today";
        }
        if (diff > 0) {
            return "in %d days".formatted(diff);
        }
        return "%d days ago".formatted(Math.abs(diff));
    }

    private static int toDays(String date) {
        String[] parts = date.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        int day = Integer.parseInt(parts[2]);
        return (year * 365) + (month * 31) + day;
    }
}
