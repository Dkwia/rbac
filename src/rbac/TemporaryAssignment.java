package rbac;

import rbac.util.DateUtils;
import rbac.util.ValidationUtils;

public class TemporaryAssignment extends AbstractRoleAssignment {

    private String expiresAt;
    private boolean autoRenew;
    private boolean revoked;

    public TemporaryAssignment(User user,
                               Role role,
                               AssignmentMetadata metadata,
                               String expiresAt,
                               boolean autoRenew) {
        super(user, role, metadata);
        String normalized = normalizeDate(expiresAt);
        if (!ValidationUtils.isValidDate(normalized)) {
            throw new IllegalArgumentException("Invalid expiration date format (YYYY-MM-DD)");
        }
        this.expiresAt = normalized;
        this.autoRenew = autoRenew;
    }

    @Override
    public boolean isActive() {
        return !revoked && !isExpired();
    }

    @Override
    public String assignmentType() {
        return "TEMPORARY";
    }

    public boolean isExpired() {
        return DateUtils.isAfter(DateUtils.getCurrentDate(), expiresAt);
    }

    public void extend(String newExpirationDate) {
        String normalized = normalizeDate(newExpirationDate);
        if (!ValidationUtils.isValidDate(normalized)) {
            throw new IllegalArgumentException("Invalid expiration date format (YYYY-MM-DD)");
        }
        this.expiresAt = normalized;
    }

    public String getTimeRemaining() {
        return "Expires at: " + expiresAt + " (" + DateUtils.formatRelativeTime(expiresAt) + ")";
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }

    public void revoke() {
        revoked = true;
    }

    @Override
    public String summary() {
        return super.summary() + "\nExpires at: " + expiresAt;
    }

    private static String normalizeDate(String value) {
        if (value == null) return null;
        if (value.length() >= 10 && (value.contains("T") || value.contains(" "))) {
            return value.substring(0, 10);
        }
        return value;
    }
}
