package rbac.system;

import rbac.*;
import rbac.manager.AssignmentManager;
import rbac.manager.RoleManager;
import rbac.manager.UserManager;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class RBACSystem {

    private final UserManager userManager;
    private final RoleManager roleManager;
    private final AssignmentManager assignmentManager;
    private String currentUser;
    private boolean running;

    public RBACSystem() {
        this.userManager = new UserManager();
        this.roleManager = new RoleManager();
        this.assignmentManager = new AssignmentManager();
        this.roleManager.setAssignmentManager(this.assignmentManager);
        this.currentUser = "system";
        this.running = true;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public RoleManager getRoleManager() {
        return roleManager;
    }

    public AssignmentManager getAssignmentManager() {
        return assignmentManager;
    }

    public void setCurrentUser(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Current user cannot be empty");
        }
        this.currentUser = username;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() {
        running = false;
    }

    public void initialize() {
        if (!roleManager.findAll().isEmpty() || !userManager.findAll().isEmpty()) {
            return;
        }

        Permission readUsers = new Permission("READ", "users", "Read users");
        Permission writeUsers = new Permission("WRITE", "users", "Write users");
        Permission deleteUsers = new Permission("DELETE", "users", "Delete users");

        Permission readRoles = new Permission("READ", "roles", "Read roles");
        Permission writeRoles = new Permission("WRITE", "roles", "Write roles");
        Permission deleteRoles = new Permission("DELETE", "roles", "Delete roles");

        Permission readAssignments = new Permission("READ", "assignments", "Read assignments");
        Permission writeAssignments = new Permission("WRITE", "assignments", "Write assignments");

        Role adminRole = new Role("Admin", "Full access to RBAC system");
        adminRole.addPermission(readUsers);
        adminRole.addPermission(writeUsers);
        adminRole.addPermission(deleteUsers);
        adminRole.addPermission(readRoles);
        adminRole.addPermission(writeRoles);
        adminRole.addPermission(deleteRoles);
        adminRole.addPermission(readAssignments);
        adminRole.addPermission(writeAssignments);

        Role managerRole = new Role("Manager", "Manage users and assignments");
        managerRole.addPermission(readUsers);
        managerRole.addPermission(writeUsers);
        managerRole.addPermission(readAssignments);
        managerRole.addPermission(writeAssignments);
        managerRole.addPermission(readRoles);

        Role viewerRole = new Role("Viewer", "Read-only access");
        viewerRole.addPermission(readUsers);
        viewerRole.addPermission(readRoles);
        viewerRole.addPermission(readAssignments);

        roleManager.add(adminRole);
        roleManager.add(managerRole);
        roleManager.add(viewerRole);

        User adminUser = User.validate("admin", "System Administrator", "admin@company.com");
        userManager.add(adminUser);
        setCurrentUser(adminUser.username());

        AssignmentMetadata metadata = AssignmentMetadata.now(currentUser, "Initial setup");
        assignmentManager.add(new PermanentAssignment(adminUser, adminRole, metadata));
    }

    public String generateStatistics() {
        int users = userManager.count();
        int roles = roleManager.count();
        int totalAssignments = assignmentManager.count();
        int activeAssignments = assignmentManager.getActiveAssignments().size();
        int expiredAssignments = assignmentManager.getExpiredAssignments().size();

        double avgRolesPerUser = users == 0
                ? 0.0
                : (double) activeAssignments / users;

        Map<String, Long> roleUsage = assignmentManager.getActiveAssignments().stream()
                .collect(Collectors.groupingBy(a -> a.role().getName(), Collectors.counting()));

        List<Map.Entry<String, Long>> topRoles = roleUsage.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .toList();

        String topRolesText;
        if (topRoles.isEmpty()) {
            topRolesText = "No active role assignments";
        } else {
            topRolesText = topRoles.stream()
                    .map(e -> "%s (%d)".formatted(e.getKey(), e.getValue()))
                    .collect(Collectors.joining(", "));
        }

        return """
                RBAC Statistics
                ------------------------------
                Users: %d
                Roles: %d
                Assignments: %d total / %d active / %d expired
                Average active roles per user: %.2f
                Top 3 roles: %s
                Generated at: %s
                """.formatted(
                users,
                roles,
                totalAssignments,
                activeAssignments,
                expiredAssignments,
                avgRolesPerUser,
                topRolesText,
                LocalDateTime.now()
        );
    }
}
