package rbac.model;

import rbac.RoleAssignment;
import rbac.TemporaryAssignment;
import rbac.User;
import rbac.Role;

import java.time.LocalDateTime;

public class AssignmentFilters {

    public static AssignmentFilter byUser(User user) {
        return a -> a.user().equals(user);
    }

    public static AssignmentFilter byUsername(String username) {
        return a -> a.user().username().equals(username);
    }

    public static AssignmentFilter byRole(Role role) {
        return a -> a.role().equals(role);
    }

    public static AssignmentFilter byRoleName(String roleName) {
        return a -> a.role().getName().equals(roleName);
    }

    public static AssignmentFilter activeOnly() {
        return RoleAssignment::isActive;
    }

    public static AssignmentFilter inactiveOnly() {
        return a -> !a.isActive();
    }

    public static AssignmentFilter byType(String type) {
        return a -> a.assignmentType().equalsIgnoreCase(type);
    }

    public static AssignmentFilter assignedBy(String username) {
        return a -> a.metadata().assignedBy().equals(username);
    }

    public static AssignmentFilter assignedAfter(String date) {
        return a -> LocalDateTime.parse(a.metadata().assignedAt())
                .isAfter(LocalDateTime.parse(date));
    }

    public static AssignmentFilter expiringBefore(String date) {
        return a -> {
            if (a instanceof TemporaryAssignment t) {
                return LocalDateTime.parse(t.getTimeRemaining()
                        .replace("Expires at: ", ""))
                        .isBefore(LocalDateTime.parse(date));
            }
            return false;
        };
    }
}
