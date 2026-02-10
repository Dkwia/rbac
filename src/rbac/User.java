package rbac;

import java.util.regex.Pattern;

public record User(String username, String fullName, String email) {

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_]{3,20}$");

    public User {
        validateFields(username, fullName, email);
    }

    public static User validate(String username, String fullName, String email) {
        validateFields(username, fullName, email);
        return new User(username, fullName, email);
    }

    private static void validateFields(String username, String fullName, String email) {

        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Username cannot be empty");

        if (!USERNAME_PATTERN.matcher(username).matches())
            throw new IllegalArgumentException("Username must be 3-20 characters, latin letters, digits or underscore");

        if (fullName == null || fullName.isBlank())
            throw new IllegalArgumentException("Full name cannot be empty");

        if (email == null || email.isBlank())
            throw new IllegalArgumentException("Email cannot be empty");

        if (!email.contains("@") || !email.substring(email.indexOf("@")).contains("."))
            throw new IllegalArgumentException("Invalid email format");
    }

    public String format() {
        return "%s (%s) <%s>".formatted(username, fullName, email);
    }
}