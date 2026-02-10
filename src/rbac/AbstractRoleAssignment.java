package rbac;

import java.util.Objects;
import java.util.UUID;

public abstract class AbstractRoleAssignment implements RoleAssignment {

    private final String assignmentId;
    private final User user;
    private final Role role;
    private final AssignmentMetadata metadata;

    protected AbstractRoleAssignment(User user,
                                     Role role,
                                     AssignmentMetadata metadata) {

        this.assignmentId = UUID.randomUUID().toString();
        this.user = Objects.requireNonNull(user);
        this.role = Objects.requireNonNull(role);
        this.metadata = Objects.requireNonNull(metadata);
    }

    @Override
    public String assignmentId() {
        return assignmentId;
    }

    @Override
    public User user() {
        return user;
    }

    @Override
    public Role role() {
        return role;
    }

    @Override
    public AssignmentMetadata metadata() {
        return metadata;
    }

    @Override
    public abstract boolean isActive();

    @Override
    public abstract String assignmentType();

    public String summary() {
        return "[%s] %s assigned to %s by %s at %s\nReason: %s\nStatus: %s"
                .formatted(
                        assignmentType(),
                        role.getName(),
                        user.username(),
                        metadata.assignedBy(),
                        metadata.assignedAt(),
                        metadata.reason(),
                        isActive() ? "ACTIVE" : "INACTIVE"
                );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractRoleAssignment that)) return false;
        return assignmentId.equals(that.assignmentId);
    }

    @Override
    public int hashCode() {
        return assignmentId.hashCode();
    }
}