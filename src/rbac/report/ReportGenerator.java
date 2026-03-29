package rbac.report;

import rbac.Permission;
import rbac.RoleAssignment;
import rbac.User;
import rbac.manager.AssignmentManager;
import rbac.manager.RoleManager;
import rbac.manager.UserManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class ReportGenerator {

    public String buildUsersReport(UserManager userManager,
                                   AssignmentManager assignmentManager) {
        return userManager.findAll().parallelStream()
                .sorted(Comparator.comparing(User::username))
                .map(user -> {
                    List<String> roles = assignmentManager.findByUser(user).stream()
                            .filter(RoleAssignment::isActive)
                            .map(assignment -> assignment.role().getName())
                            .sorted()
                            .toList();
                    return "%s | %s | roles=%s".formatted(
                            user.username(),
                            user.email(),
                            roles.isEmpty() ? "-" : String.join(",", roles));
                })
                .collect(Collectors.joining(System.lineSeparator()));
    }

    public String buildPermissionMatrix(UserManager userManager,
                                        RoleManager roleManager,
                                        AssignmentManager assignmentManager) {
        Set<String> headers = roleManager.findAll().parallelStream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.name() + ":" + permission.resource())
                .collect(Collectors.toCollection(TreeSet::new));

        List<String> lines = new ArrayList<>();
        lines.add("user;" + String.join(";", headers));
        lines.addAll(userManager.findAll().parallelStream()
                .sorted(Comparator.comparing(User::username))
                .map(user -> {
                    Set<String> permissions = assignmentManager.getUserPermissions(user).stream()
                            .map(permission -> permission.name() + ":" + permission.resource())
                            .collect(Collectors.toSet());
                    List<String> cells = headers.stream()
                            .map(header -> permissions.contains(header) ? "Y" : "-")
                            .toList();
                    return user.username() + ";" + String.join(";", cells);
                })
                .toList());
        return String.join(System.lineSeparator(), lines);
    }

    public String buildStatistics(UserManager userManager,
                                  RoleManager roleManager,
                                  AssignmentManager assignmentManager) {
        long activeAssignments = assignmentManager.getActiveAssignments().size();
        long inactiveAssignments = assignmentManager.getExpiredAssignments().size();
        long uniquePermissions = roleManager.findAll().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::format)
                .distinct()
                .count();
        return """
                users=%d
                roles=%d
                activeAssignments=%d
                inactiveAssignments=%d
                uniquePermissions=%d
                """.formatted(
                userManager.count(),
                roleManager.count(),
                activeAssignments,
                inactiveAssignments,
                uniquePermissions
        ).trim();
    }
}
