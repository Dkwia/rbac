package rbac;

import java.time.LocalDateTime;

public class TemporaryAssignment extends AbstractRoleAssignment {

    private String expiresAt;
    private boolean autoRenew;

    public TemporaryAssignment(User user,
                               Role role,
                               AssignmentMetadata metadata,
                               String expiresAt,
                               boolean autoRenew) {
        super(user, role, metadata);
        this.expiresAt = expiresAt;
        this.autoRenew = autoRenew;
    }

    @Override
    public boolean isActive() {
        return !isExpired();
    }

    @Override
    public String assignmentType() {
        return "TEMPORARY";
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(LocalDateTime.parse(expiresAt));
    }

    public void extend(String newExpirationDate) {
        this.expiresAt = newExpirationDate;
    }

    public String getTimeRemaining() {
        return "Expires at: " + expiresAt;
    }

    @Override
    public String summary() {
        return super.summary() + "\nExpires at: " + expiresAt;
    }
}