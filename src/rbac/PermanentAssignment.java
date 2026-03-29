package rbac;

import java.util.concurrent.atomic.AtomicBoolean;

public class PermanentAssignment extends AbstractRoleAssignment {

    private final AtomicBoolean revoked = new AtomicBoolean(false);

    public PermanentAssignment(User user,
                               Role role,
                               AssignmentMetadata metadata) {
        super(user, role, metadata);
    }

    public PermanentAssignment(String assignmentId,
                               User user,
                               Role role,
                               AssignmentMetadata metadata,
                               boolean revoked) {
        super(assignmentId, user, role, metadata);
        this.revoked.set(revoked);
    }

    @Override
    public boolean isActive() {
        return !revoked.get();
    }

    @Override
    public String assignmentType() {
        return "PERMANENT";
    }

    public void revoke() {
        revoked.set(true);
    }

    public boolean isRevoked() {
        return revoked.get();
    }
}
