package rbac.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class ValidationUtils {

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern DATE_PATTERN =
            Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    private ValidationUtils() {
    }

    public static boolean isValidUsername(String username) {
        if (username == null) return false;
        return USERNAME_PATTERN.matcher(username).matches();
    }

    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        return EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidDate(String date) {
        if (date == null || !DATE_PATTERN.matcher(date).matches()) {
            return false;
        }
        String[] parts = date.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        int day = Integer.parseInt(parts[2]);
        if (year < 0) return false;
        if (month < 1 || month > 12) return false;
        return day >= 1 && day <= 31;
    }

    public static String normalizeString(String input) {
        if (input == null) return null;
        String trimmed = input.trim();
        if (trimmed.isEmpty()) return "";
        return trimmed.replaceAll("\\s+", " ");
    }

    public static void requireNonEmpty(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            String name = fieldName == null ? "Value" : fieldName;
            throw new IllegalArgumentException(name + " cannot be empty");
        }
    }

    public static String toLowerCase(String value) {
        if (value == null) return null;
        return value.toLowerCase(Locale.ROOT);
    }
}
