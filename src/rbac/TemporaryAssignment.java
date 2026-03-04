package rbac;

import java.time.LocalDateTime;

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
        LocalDateTime.parse(expiresAt);
        this.expiresAt = expiresAt;
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
        return LocalDateTime.now().isAfter(LocalDateTime.parse(expiresAt));
    }

    public void extend(String newExpirationDate) {
        LocalDateTime.parse(newExpirationDate);
        this.expiresAt = newExpirationDate;
    }

    public String getTimeRemaining() {
        return "Expires at: " + expiresAt;
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
}
