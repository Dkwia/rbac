package rbac.persistence;

import java.io.Serializable;
import java.util.List;

public record RBACSnapshot(List<UserRecord> users,
                           List<RoleRecord> roles,
                           List<AssignmentRecord> assignments) implements Serializable {

    public record UserRecord(String username,
                             String fullName,
                             String email) implements Serializable { }

    public record PermissionRecord(String name,
                                   String resource,
                                   String description) implements Serializable { }

    public record RoleRecord(String id,
                             String name,
                             String description,
                             List<PermissionRecord> permissions) implements Serializable { }

    public record AssignmentRecord(String assignmentId,
                                   String userUsername,
                                   String roleId,
                                   String assignedBy,
                                   String assignedAt,
                                   String reason,
                                   String type,
                                   boolean active,
                                   String expiresAt,
                                   boolean autoRenew) implements Serializable { }
}
