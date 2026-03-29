package rbac;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

public class TemporaryAssignment extends AbstractRoleAssignment {

    private volatile LocalDateTime expiresAt;
    private final boolean autoRenew;
    private final AtomicBoolean active = new AtomicBoolean(true);

    public TemporaryAssignment(User user,
                               Role role,
                               AssignmentMetadata metadata,
                               String expiresAt,
                               boolean autoRenew) {
        super(user, role, metadata);
        this.expiresAt = LocalDateTime.parse(expiresAt);
        this.autoRenew = autoRenew;
    }

    public TemporaryAssignment(String assignmentId,
                               User user,
                               Role role,
                               AssignmentMetadata metadata,
                               String expiresAt,
                               boolean autoRenew,
                               boolean active) {
        super(assignmentId, user, role, metadata);
        this.expiresAt = LocalDateTime.parse(expiresAt);
        this.autoRenew = autoRenew;
        this.active.set(active);
    }

    @Override
    public boolean isActive() {
        if (active.get() && isExpired()) {
            active.compareAndSet(true, false);
        }
        return active.get();
    }

    @Override
    public String assignmentType() {
        return "TEMPORARY";
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void extend(String newExpirationDate) {
        this.expiresAt = LocalDateTime.parse(newExpirationDate);
        active.set(true);
    }

    public boolean deactivateIfExpired(LocalDateTime now) {
        if (active.get() && now.isAfter(expiresAt)) {
            return active.compareAndSet(true, false);
        }
        return false;
    }

    public void deactivate() {
        active.set(false);
    }

    public String getTimeRemaining() {
        return "Expires at: " + expiresAt;
    }

    public String getExpiresAt() {
        return expiresAt.toString();
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }

    @Override
    public String summary() {
        return super.summary() + "\nExpires at: " + expiresAt;
    }
}
