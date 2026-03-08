package rbac;

import rbac.util.ValidationUtils;

public record User(String username, String fullName, String email) {

    public User {
        username = ValidationUtils.normalizeString(username);
        fullName = ValidationUtils.normalizeString(fullName);
        email = ValidationUtils.normalizeString(email);
        validateFields(username, fullName, email);
        email = ValidationUtils.toLowerCase(email);
    }

    public static User validate(String username, String fullName, String email) {
        return new User(username, fullName, email);
    }

    private static void validateFields(String username, String fullName, String email) {

        ValidationUtils.requireNonEmpty(username, "Username");
        ValidationUtils.requireNonEmpty(fullName, "Full name");
        ValidationUtils.requireNonEmpty(email, "Email");

        if (!ValidationUtils.isValidUsername(username))
            throw new IllegalArgumentException("Username must be 3-20 characters, latin letters, digits or underscore");
        if (!ValidationUtils.isValidEmail(ValidationUtils.toLowerCase(email)))
            throw new IllegalArgumentException("Invalid email format");
    }

    public String format() {
        return "%s (%s) <%s>".formatted(username, fullName, email);
    }
}
