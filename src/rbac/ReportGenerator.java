package rbac;

import rbac.manager.AssignmentManager;
import rbac.manager.RoleManager;
import rbac.manager.UserManager;
import rbac.util.FormatUtils;
import rbac.util.ValidationUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ReportGenerator {

    public String generateUserReport(UserManager userManager, AssignmentManager assignmentManager) {
        Objects.requireNonNull(userManager, "UserManager");
        Objects.requireNonNull(assignmentManager, "AssignmentManager");

        List<User> users = userManager.findAll().stream()
                .sorted(Comparator.comparing(User::username))
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("User Report").append("\n");
        sb.append("Total users: ").append(users.size()).append("\n\n");

        for (User user : users) {
            sb.append(String.format("User: %s%n", user.format()));
            List<RoleAssignment> assignments = assignmentManager.findByUser(user).stream()
                    .filter(RoleAssignment::isActive)
                    .toList();
            if (assignments.isEmpty()) {
                sb.append("  Roles: none\n");
            } else {
                String roles = assignments.stream()
                        .map(a -> a.role().getName())
                        .distinct()
                        .collect(Collectors.joining(", "));
                sb.append(String.format("  Roles: %s%n", roles));
            }
            sb.append("\n");
        }

        return sb.toString().trim();
    }

    public String generateRoleReport(RoleManager roleManager, AssignmentManager assignmentManager) {
        Objects.requireNonNull(roleManager, "RoleManager");
        Objects.requireNonNull(assignmentManager, "AssignmentManager");

        List<Role> roles = roleManager.findAll().stream()
                .sorted(Comparator.comparing(Role::getName))
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("Role Report").append("\n");
        sb.append("Total roles: ").append(roles.size()).append("\n\n");

        for (Role role : roles) {
            long count = assignmentManager.findByRole(role).stream()
                    .filter(RoleAssignment::isActive)
                    .map(RoleAssignment::user)
                    .distinct()
                    .count();
            sb.append(String.format("- %s: %d users%n", role.getName(), count));
        }

        return sb.toString().trim();
    }

    public String generatePermissionMatrix(UserManager userManager, AssignmentManager assignmentManager) {
        Objects.requireNonNull(userManager, "UserManager");
        Objects.requireNonNull(assignmentManager, "AssignmentManager");

        List<User> users = userManager.findAll().stream()
                .sorted(Comparator.comparing(User::username))
                .toList();

        List<String> resources = assignmentManager.getActiveAssignments().stream()
                .flatMap(a -> a.role().getPermissions().stream())
                .map(Permission::resource)
                .distinct()
                .sorted()
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("Permission Matrix").append("\n");
        sb.append("Users: ").append(users.size()).append(", Resources: ").append(resources.size()).append("\n\n");

        String[] headers = new String[resources.size() + 1];
        headers[0] = "USER";
        for (int i = 0; i < resources.size(); i++) {
            headers[i + 1] = resources.get(i);
        }

        List<String[]> rows = new ArrayList<>();
        for (User user : users) {
            String[] row = new String[headers.length];
            row[0] = user.username();
            Set<Permission> permissions = assignmentManager.getUserPermissions(user);
            for (int i = 0; i < resources.size(); i++) {
                String resource = resources.get(i);
                String cell = permissions.stream()
                        .filter(p -> p.resource().equals(resource))
                        .map(Permission::name)
                        .distinct()
                        .sorted()
                        .collect(Collectors.joining(", "));
                row[i + 1] = cell.isBlank() ? "-" : cell;
            }
            rows.add(row);
        }

        sb.append(FormatUtils.formatTable(headers, rows));
        return sb.toString();
    }

    public void exportToFile(String report, String filename) {
        ValidationUtils.requireNonEmpty(report, "Report");
        ValidationUtils.requireNonEmpty(filename, "Filename");
        try {
            Files.writeString(Path.of(filename), report);
        } catch (IOException e) {
            System.out.println("Report save error: " + e.getMessage());
        }
    }
}
