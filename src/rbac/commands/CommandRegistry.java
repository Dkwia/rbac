package rbac.commands;

import rbac.*;
import rbac.manager.AssignmentManager;
import rbac.manager.RoleManager;
import rbac.manager.UserManager;
import rbac.model.AssignmentFilter;
import rbac.model.AssignmentFilters;
import rbac.model.RoleFilter;
import rbac.model.RoleFilters;
import rbac.model.UserFilter;
import rbac.model.UserFilters;
import rbac.sorters.AssignmentSorters;
import rbac.sorters.RoleSorters;
import rbac.sorters.UserSorters;
import rbac.system.RBACSystem;
import rbac.util.ConsoleUtils;
import rbac.util.FormatUtils;
import rbac.util.ValidationUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class CommandRegistry {

    private CommandRegistry() {
    }

    public static void registerDefaultCommands(CommandParser parser) {
        registerUserCommands(parser);
        registerRoleCommands(parser);
        registerAssignmentCommands(parser);
        registerPermissionCommands(parser);
        registerServiceCommands(parser);
    }

    private static void registerUserCommands(CommandParser parser) {
        parser.registerCommand("user-list", "List users (supports optional substring argument)", (scanner, system) -> {
            List<User> users = system.getUserManager().findAll();
            String[] args = parser.getLastArgs();
            if (args.length > 0) {
                String needle = String.join(" ", args).toLowerCase(Locale.ROOT);
                users = users.stream()
                        .filter(u -> u.username().toLowerCase(Locale.ROOT).contains(needle)
                                || u.email().toLowerCase(Locale.ROOT).contains(needle)
                                || u.fullName().toLowerCase(Locale.ROOT).contains(needle))
                        .toList();
            }
            printUsers(users);
        });

        parser.registerCommand("user-create", "Create a user", (scanner, system) -> {
            try {
                String username = ConsoleUtils.promptString(scanner, "Username: ", true);
                String fullName = ConsoleUtils.promptString(scanner, "Full name: ", true);
                String email = ConsoleUtils.promptString(scanner, "Email: ", true);
                User user = User.validate(username, fullName, email);
                system.getUserManager().add(user);
                system.getAuditLog().log("USER_CREATE", system.getCurrentUser(), user.username(), user.email());
                System.out.println("User created: " + user.format());
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("user-view", "View user with roles and permissions", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Username: ", true);
            Optional<User> user = system.getUserManager().findByUsername(username);
            if (user.isEmpty()) {
                System.out.println("User not found");
                return;
            }

            System.out.println(user.get().format());
            List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(user.get());
            if (assignments.isEmpty()) {
                System.out.println("No assignments");
            } else {
                System.out.println("Assignments:");
                String[] headers = {"ASSIGNMENT ID", "ROLE", "STATUS"};
                List<String[]> rows = assignments.stream()
                        .map(a -> new String[]{a.assignmentId(), a.role().getName(), status(a)})
                        .toList();
                System.out.println(FormatUtils.formatTable(headers, rows));
            }

            Set<Permission> permissions = system.getAssignmentManager().getUserPermissions(user.get());
            if (permissions.isEmpty()) {
                System.out.println("No active permissions");
            } else {
                System.out.println("Permissions:");
                String[] headers = {"RESOURCE", "PERMISSION", "DESCRIPTION"};
                List<String[]> rows = permissions.stream()
                        .sorted(Comparator.comparing(Permission::resource).thenComparing(Permission::name))
                        .map(p -> new String[]{p.resource(), p.name(), p.description()})
                        .toList();
                System.out.println(FormatUtils.formatTable(headers, rows));
            }
        });

        parser.registerCommand("user-update", "Update user full name/email", (scanner, system) -> {
            try {
                String username = ConsoleUtils.promptString(scanner, "Username: ", true);
                String fullName = ConsoleUtils.promptString(scanner, "New full name: ", true);
                String email = ConsoleUtils.promptString(scanner, "New email: ", true);
                system.getUserManager().update(username, fullName, email);
                system.getAuditLog().log("USER_UPDATE", system.getCurrentUser(), username, "Updated full name/email");
                System.out.println("User updated");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("user-delete", "Delete a user and all their assignments", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Username: ", true);
            Optional<User> user = system.getUserManager().findByUsername(username);
            if (user.isEmpty()) {
                System.out.println("User not found");
                return;
            }
            boolean confirm = ConsoleUtils.promptYesNo(scanner, "Confirm deletion? (yes/no): ");
            if (!confirm) {
                System.out.println("Cancelled");
                return;
            }

            int removed = system.getAssignmentManager().removeAssignmentsForUser(user.get());
            system.getUserManager().remove(user.get());
            system.getAuditLog().log("USER_DELETE", system.getCurrentUser(), username, "Assignments removed: " + removed);
            System.out.printf("User deleted. Removed assignments: %d%n", removed);
        });

        parser.registerCommand("user-search", "Search users by filters", (scanner, system) -> {
            System.out.println("1) username contains");
            System.out.println("2) email contains");
            System.out.println("3) email domain");
            System.out.println("4) full name contains");
            int choice = ConsoleUtils.promptInt(scanner, "Choice: ", 1, 4);
            String value = ConsoleUtils.promptString(scanner, "Value: ", true);

            UserFilter filter;
            switch (choice) {
                case 1 -> filter = UserFilters.byUsernameContains(value);
                case 2 -> filter = u -> u.email().toLowerCase(Locale.ROOT).contains(value.toLowerCase(Locale.ROOT));
                case 3 -> filter = UserFilters.byEmailDomain(value);
                case 4 -> filter = UserFilters.byFullNameContains(value);
                default -> throw new IllegalStateException("Unexpected choice");
            }

            List<User> users = system.getUserManager().findAll(filter, UserSorters.byUsername());
            printUsers(users);
        });
    }

    private static void registerRoleCommands(CommandParser parser) {
        parser.registerCommand("role-list", "List all roles", (scanner, system) -> {
            List<Role> roles = system.getRoleManager().findAll(RoleFilterAlways.TRUE, RoleSorters.byName());
            if (roles.isEmpty()) {
                System.out.println("No roles");
                return;
            }
            String[] headers = {"ROLE", "PERMISSIONS", "ID"};
            List<String[]> rows = roles.stream()
                    .map(r -> new String[]{
                            r.getName(),
                            String.valueOf(r.getPermissions().size()),
                            r.getId()
                    })
                    .toList();
            System.out.println(FormatUtils.formatTable(headers, rows));
        });

        parser.registerCommand("role-create", "Create role and optionally add permissions", (scanner, system) -> {
            try {
                String name = ConsoleUtils.promptString(scanner, "Role name: ", true);
                String description = ConsoleUtils.promptString(scanner, "Description: ", true);
                Role role = new Role(name, description);
                system.getRoleManager().add(role);

                while (true) {
                    boolean add = ConsoleUtils.promptYesNo(scanner, "Add permission? (yes/no): ");
                    if (!add) {
                        break;
                    }
                    Permission permission = readPermission(scanner);
                    role.addPermission(permission);
                }

                system.getAuditLog().log("ROLE_CREATE", system.getCurrentUser(), role.getName(), role.getDescription());
                System.out.println("Role created: " + role.getName());
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("role-view", "View role details", (scanner, system) -> {
            String name = ConsoleUtils.promptString(scanner, "Role name: ", true);
            Optional<Role> role = system.getRoleManager().findByName(name);
            if (role.isEmpty()) {
                System.out.println("Role not found");
                return;
            }
            System.out.println(FormatUtils.formatBox(role.get().format()));
        });

        parser.registerCommand("role-update", "Update role name/description", (scanner, system) -> {
            try {
                String currentName = ConsoleUtils.promptString(scanner, "Current role name: ", true);
                String newName = ConsoleUtils.promptString(scanner, "New role name: ", true);
                String newDescription = ConsoleUtils.promptString(scanner, "New description: ", true);
                system.getRoleManager().update(currentName, newName, newDescription);
                system.getAuditLog().log("ROLE_UPDATE", system.getCurrentUser(), currentName, "Renamed to " + newName);
                System.out.println("Role updated");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("role-delete", "Delete role if no active assignments", (scanner, system) -> {
            String name = ConsoleUtils.promptString(scanner, "Role name: ", true);
            Optional<Role> roleOpt = system.getRoleManager().findByName(name);
            if (roleOpt.isEmpty()) {
                System.out.println("Role not found");
                return;
            }
            Role role = roleOpt.get();
            List<RoleAssignment> active = system.getAssignmentManager().findByRole(role).stream()
                    .filter(RoleAssignment::isActive)
                    .toList();
            if (!active.isEmpty()) {
                System.out.println("Role has active assignments:");
                String[] headers = {"USERNAME"};
                List<String[]> rows = active.stream()
                        .map(a -> new String[]{a.user().username()})
                        .toList();
                System.out.println(FormatUtils.formatTable(headers, rows));
            }
            boolean confirm = ConsoleUtils.promptYesNo(scanner, "Confirm deletion? (yes/no): ");
            if (!confirm) {
                System.out.println("Cancelled");
                return;
            }
            try {
                system.getRoleManager().remove(role);
                system.getAuditLog().log("ROLE_DELETE", system.getCurrentUser(), role.getName(), "Role deleted");
                System.out.println("Role deleted");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("role-add-permission", "Add permission to role", (scanner, system) -> {
            try {
                String roleName = ConsoleUtils.promptString(scanner, "Role name: ", true);
                Permission permission = readPermission(scanner);
                system.getRoleManager().addPermissionToRole(roleName, permission);
                System.out.println("Permission added");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("role-remove-permission", "Remove role permission by index", (scanner, system) -> {
            String roleName = ConsoleUtils.promptString(scanner, "Role name: ", true);
            Optional<Role> roleOpt = system.getRoleManager().findByName(roleName);
            if (roleOpt.isEmpty()) {
                System.out.println("Role not found");
                return;
            }

            Role role = roleOpt.get();
            List<Permission> permissions = new ArrayList<>(role.getPermissions());
            permissions.sort(Comparator.comparing(Permission::resource).thenComparing(Permission::name));
            if (permissions.isEmpty()) {
                System.out.println("Role has no permissions");
                return;
            }
            for (int i = 0; i < permissions.size(); i++) {
                System.out.printf("%d) %s%n", i + 1, permissions.get(i).format());
            }

            int idx = ConsoleUtils.promptInt(scanner, "Number to remove: ", 1, permissions.size()) - 1;

            system.getRoleManager().removePermissionFromRole(roleName, permissions.get(idx));
            System.out.println("Permission removed");
        });

        parser.registerCommand("role-search", "Search roles", (scanner, system) -> {
            System.out.println("1) name contains");
            System.out.println("2) has specific permission");
            System.out.println("3) min permissions count");
            int choice = ConsoleUtils.promptInt(scanner, "Choice: ", 1, 3);

            RoleFilter filter;
            switch (choice) {
                case 1 -> {
                    String value = ConsoleUtils.promptString(scanner, "Substring: ", true);
                    filter = RoleFilters.byNameContains(value);
                }
                case 2 -> {
                    String name = ConsoleUtils.promptString(scanner, "Permission name: ", true);
                    String resource = ConsoleUtils.promptString(scanner, "Resource: ", true);
                    filter = RoleFilters.hasPermission(name, resource);
                }
                case 3 -> {
                    int n = ConsoleUtils.promptInt(scanner, "Minimum permissions: ", 0, Integer.MAX_VALUE);
                    filter = RoleFilters.hasAtLeastNPermissions(n);
                }
                default -> {
                    System.out.println("Unknown option");
                    return;
                }
            }

            List<Role> roles = system.getRoleManager().findAll(filter, RoleSorters.byName());
            if (roles.isEmpty()) {
                System.out.println("No roles found");
                return;
            }
            String[] headers = {"ROLE", "PERMISSIONS"};
            List<String[]> rows = roles.stream()
                    .map(r -> new String[]{r.getName(), String.valueOf(r.getPermissions().size())})
                    .toList();
            System.out.println(FormatUtils.formatTable(headers, rows));
        });
    }

    private static void registerAssignmentCommands(CommandParser parser) {
        parser.registerCommand("assign-role", "Assign role to user", (scanner, system) -> {
            try {
                String username = ConsoleUtils.promptString(scanner, "Username: ", true);
                User user = system.getUserManager().findByUsername(username)
                        .orElseThrow(() -> new NoSuchElementException("User not found"));

                List<Role> roles = system.getRoleManager().findAll(RoleFilterAlways.TRUE, RoleSorters.byName());
                if (roles.isEmpty()) {
                    System.out.println("No roles available");
                    return;
                }

                List<String> roleNames = roles.stream()
                        .map(Role::getName)
                        .toList();
                String selectedRole = ConsoleUtils.promptChoice(scanner, "Select role:", roleNames);
                Role role = system.getRoleManager().findByName(selectedRole)
                        .orElseThrow(() -> new NoSuchElementException("Role not found"));

                String type = ConsoleUtils.promptChoice(
                        scanner,
                        "Assignment type:",
                        List.of("permanent", "temporary")
                ).toLowerCase(Locale.ROOT);
                String reason = ConsoleUtils.promptString(scanner, "Reason: ", false);
                AssignmentMetadata metadata = AssignmentMetadata.now(system.getCurrentUser(), reason);

                if ("temporary".equals(type)) {
                    String expiresAt = ConsoleUtils.promptString(scanner, "Expires at (YYYY-MM-DD): ", true);
                    while (!ValidationUtils.isValidDate(expiresAt)) {
                        System.out.println("Invalid date format.");
                        expiresAt = ConsoleUtils.promptString(scanner, "Expires at (YYYY-MM-DD): ", true);
                    }
                    boolean autoRenew = ConsoleUtils.promptYesNo(scanner, "Auto renew? (yes/no): ");
                    TemporaryAssignment assignment = new TemporaryAssignment(user, role, metadata, expiresAt, autoRenew);
                    system.getAssignmentManager().add(assignment);
                    system.getAuditLog().log("ROLE_ASSIGN", system.getCurrentUser(),
                            user.username(), "Temporary role " + role.getName() + " until " + expiresAt);
                    System.out.println("Temporary role assigned");
                } else if ("permanent".equals(type)) {
                    system.getAssignmentManager().add(new PermanentAssignment(user, role, metadata));
                    system.getAuditLog().log("ROLE_ASSIGN", system.getCurrentUser(),
                            user.username(), "Permanent role " + role.getName());
                    System.out.println("Permanent role assigned");
                } else {
                    System.out.println("Unknown type");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("revoke-role", "Revoke assignment from user", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Username: ", true);
            Optional<User> userOpt = system.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("User not found");
                return;
            }

            List<RoleAssignment> active = system.getAssignmentManager().findByUser(userOpt.get()).stream()
                    .filter(RoleAssignment::isActive)
                    .toList();

            if (active.isEmpty()) {
                System.out.println("No active assignments");
                return;
            }

            for (int i = 0; i < active.size(); i++) {
                RoleAssignment a = active.get(i);
                System.out.printf("%d) %s | %s | %s%n", i + 1, a.assignmentId(), a.role().getName(), a.assignmentType());
            }

            try {
                int index = ConsoleUtils.promptInt(scanner, "Choose assignment: ", 1, active.size()) - 1;
                RoleAssignment target = active.get(index);
                system.getAssignmentManager().revokeAssignment(target.assignmentId());
                system.getAuditLog().log("ROLE_REVOKE", system.getCurrentUser(),
                        username, "Revoked " + target.role().getName());
                System.out.println("Assignment revoked");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("assignment-list", "List all assignments", (scanner, system) ->
                printAssignments(system.getAssignmentManager().findAll(AssignmentFilterAlways.TRUE, AssignmentSorters.byAssignmentDate())));

        parser.registerCommand("assignment-list-user", "List assignments for username", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Username: ", true);
            List<RoleAssignment> assignments = system.getAssignmentManager()
                    .findByFilter(AssignmentFilters.byUsername(username));
            printAssignments(assignments);
        });

        parser.registerCommand("assignment-list-role", "List users assigned to role", (scanner, system) -> {
            String roleName = ConsoleUtils.promptString(scanner, "Role name: ", true);
            List<RoleAssignment> assignments = system.getAssignmentManager()
                    .findByFilter(AssignmentFilters.byRoleName(roleName));
            if (assignments.isEmpty()) {
                System.out.println("No assignments found");
                return;
            }
            String[] headers = {"USERNAME", "STATUS"};
            List<String[]> rows = assignments.stream()
                    .map(a -> new String[]{a.user().username(), status(a)})
                    .toList();
            System.out.println(FormatUtils.formatTable(headers, rows));
        });

        parser.registerCommand("assignment-active", "List active assignments", (scanner, system) ->
                printAssignments(system.getAssignmentManager().getActiveAssignments()));

        parser.registerCommand("assignment-expired", "List expired temporary assignments", (scanner, system) ->
                printAssignments(system.getAssignmentManager().getExpiredAssignments()));

        parser.registerCommand("assignment-extend", "Extend temporary assignment", (scanner, system) -> {
            String id = ConsoleUtils.promptString(scanner, "Assignment ID (leave empty to search by username+role): ", false);

            RoleAssignment target;
            if (id == null || id.isBlank()) {
                String username = ConsoleUtils.promptString(scanner, "Username: ", true);
                String roleName = ConsoleUtils.promptString(scanner, "Role name: ", true);
                target = system.getAssignmentManager().findAll().stream()
                        .filter(a -> a.user().username().equals(username))
                        .filter(a -> a.role().getName().equals(roleName))
                        .filter(a -> a instanceof TemporaryAssignment)
                        .findFirst()
                        .orElse(null);
            } else {
                target = system.getAssignmentManager().findById(id).orElse(null);
            }

            if (!(target instanceof TemporaryAssignment)) {
                System.out.println("Temporary assignment not found");
                return;
            }

            String newDate = ConsoleUtils.promptString(scanner, "New expires at (YYYY-MM-DD): ", true);
            while (!ValidationUtils.isValidDate(newDate)) {
                System.out.println("Invalid date format.");
                newDate = ConsoleUtils.promptString(scanner, "New expires at (YYYY-MM-DD): ", true);
            }
            try {
                system.getAssignmentManager().extendTemporaryAssignment(target.assignmentId(), newDate);
                System.out.println("Assignment extended");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("assignment-search", "Search assignments by filters", (scanner, system) -> {
            System.out.println("1) by user");
            System.out.println("2) by role");
            System.out.println("3) by type");
            System.out.println("4) by status");
            System.out.println("5) assigned after date");
            System.out.println("6) expiring before date");
            int choice = ConsoleUtils.promptInt(scanner, "Choice: ", 1, 6);

            AssignmentFilter filter;
            switch (choice) {
                case 1 -> filter = AssignmentFilters.byUsername(ConsoleUtils.promptString(scanner, "Username: ", true));
                case 2 -> filter = AssignmentFilters.byRoleName(ConsoleUtils.promptString(scanner, "Role name: ", true));
                case 3 -> filter = AssignmentFilters.byType(ConsoleUtils.promptString(scanner, "Type (permanent/temporary): ", true));
                case 4 -> {
                    String status = ConsoleUtils.promptString(scanner, "Status (active/inactive): ", true);
                    filter = "active".equalsIgnoreCase(status)
                            ? AssignmentFilters.activeOnly()
                            : AssignmentFilters.inactiveOnly();
                }
                case 5 -> {
                    String date = ConsoleUtils.promptString(scanner, "Date (YYYY-MM-DD): ", true);
                    while (!ValidationUtils.isValidDate(date)) {
                        System.out.println("Invalid date format.");
                        date = ConsoleUtils.promptString(scanner, "Date (YYYY-MM-DD): ", true);
                    }
                    filter = AssignmentFilters.assignedAfter(date);
                }
                case 6 -> {
                    String date = ConsoleUtils.promptString(scanner, "Date (YYYY-MM-DD): ", true);
                    while (!ValidationUtils.isValidDate(date)) {
                        System.out.println("Invalid date format.");
                        date = ConsoleUtils.promptString(scanner, "Date (YYYY-MM-DD): ", true);
                    }
                    filter = AssignmentFilters.expiringBefore(date);
                }
                default -> {
                    System.out.println("Unknown option");
                    return;
                }
            }

            List<RoleAssignment> assignments = system.getAssignmentManager().findByFilter(filter);
            printAssignments(assignments);
        });
    }

    private static void registerPermissionCommands(CommandParser parser) {
        parser.registerCommand("permissions-user", "Show user permissions grouped by resource", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Username: ", true);
            Optional<User> user = system.getUserManager().findByUsername(username);
            if (user.isEmpty()) {
                System.out.println("User not found");
                return;
            }

            Map<String, List<Permission>> grouped = system.getAssignmentManager().getUserPermissions(user.get()).stream()
                    .collect(Collectors.groupingBy(Permission::resource));

            if (grouped.isEmpty()) {
                System.out.println("No active permissions");
                return;
            }
            String[] headers = {"RESOURCE", "PERMISSIONS"};
            List<String[]> rows = grouped.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> new String[]{
                            entry.getKey(),
                            entry.getValue().stream()
                                    .sorted(Comparator.comparing(Permission::name))
                                    .map(Permission::name)
                                    .collect(Collectors.joining(", "))
                    })
                    .toList();
            System.out.println(FormatUtils.formatTable(headers, rows));
        });

        parser.registerCommand("permissions-check", "Check user permission and role source", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Username: ", true);
            String permissionName = ConsoleUtils.promptString(scanner, "Permission name: ", true);
            String resource = ConsoleUtils.promptString(scanner, "Resource: ", true);

            Optional<User> user = system.getUserManager().findByUsername(username);
            if (user.isEmpty()) {
                System.out.println("User not found");
                return;
            }

            boolean allowed = system.getAssignmentManager()
                    .userHasPermission(user.get(), permissionName, resource);

            if (!allowed) {
                System.out.println("Result: DENIED");
                return;
            }

            List<String> roles = system.getAssignmentManager().findByUser(user.get()).stream()
                    .filter(RoleAssignment::isActive)
                    .filter(a -> a.role().hasPermission(permissionName, resource))
                    .map(a -> a.role().getName())
                    .distinct()
                    .toList();

            System.out.println("Result: ALLOWED");
            System.out.println("Provided by roles: " + String.join(", ", roles));
        });
    }

    private static void registerServiceCommands(CommandParser parser) {
        parser.registerCommand("help", "Show command help", (scanner, system) -> parser.printHelp());

        parser.registerCommand("stats", "Show system statistics", (scanner, system) ->
                System.out.println(system.generateStatistics()));

        parser.registerCommand("audit-log", "Show audit log", (scanner, system) ->
                system.getAuditLog().printLog());

        parser.registerCommand("report-users", "Generate report by users", (scanner, system) -> {
            String report = system.getReportGenerator()
                    .generateUserReport(system.getUserManager(), system.getAssignmentManager());
            System.out.println(report);
            if (ConsoleUtils.promptYesNo(scanner, "Save report to file? (yes/no): ")) {
                String filename = ConsoleUtils.promptString(scanner, "Filename: ", true);
                system.getReportGenerator().exportToFile(report, filename);
            }
        });

        parser.registerCommand("report-roles", "Generate report by roles", (scanner, system) -> {
            String report = system.getReportGenerator()
                    .generateRoleReport(system.getRoleManager(), system.getAssignmentManager());
            System.out.println(report);
            if (ConsoleUtils.promptYesNo(scanner, "Save report to file? (yes/no): ")) {
                String filename = ConsoleUtils.promptString(scanner, "Filename: ", true);
                system.getReportGenerator().exportToFile(report, filename);
            }
        });

        parser.registerCommand("report-matrix", "Generate permission matrix report", (scanner, system) -> {
            String report = system.getReportGenerator()
                    .generatePermissionMatrix(system.getUserManager(), system.getAssignmentManager());
            System.out.println(report);
            if (ConsoleUtils.promptYesNo(scanner, "Save report to file? (yes/no): ")) {
                String filename = ConsoleUtils.promptString(scanner, "Filename: ", true);
                system.getReportGenerator().exportToFile(report, filename);
            }
        });

        parser.registerCommand("clear", "Clear console output", (scanner, system) -> {
            for (int i = 0; i < 40; i++) {
                System.out.println();
            }
        });

        parser.registerCommand("exit", "Exit application", (scanner, system) -> {
            boolean confirm = ConsoleUtils.promptYesNo(scanner, "Exit application? (yes/no): ");
            if (!confirm) {
                System.out.println("Cancelled");
                return;
            }

            boolean save = ConsoleUtils.promptYesNo(scanner, "Save data before exit? (yes/no): ");
            if (save) {
                save(scanner, system);
            }
            system.stop();
        });

        parser.registerCommand("save", "Save data to file", CommandRegistry::save);
        parser.registerCommand("load", "Load data from file", CommandRegistry::load);
    }

    private static void save(Scanner scanner, RBACSystem system) {
        String pathText = ConsoleUtils.promptString(scanner, "File path (default rbac-data.txt): ", false);
        Path path = pathText == null || pathText.isBlank() ? Path.of("rbac-data.txt") : Path.of(pathText.trim());

        List<String> lines = new ArrayList<>();
        lines.add("USERS");
        for (User user : system.getUserManager().findAll()) {
            lines.add("U\t%s\t%s\t%s".formatted(esc(user.username()), esc(user.fullName()), esc(user.email())));
        }

        lines.add("ROLES");
        for (Role role : system.getRoleManager().findAll()) {
            lines.add("R\t%s\t%s".formatted(esc(role.getName()), esc(role.getDescription())));
            for (Permission p : role.getPermissions()) {
                lines.add("P\t%s\t%s\t%s\t%s".formatted(
                        esc(role.getName()),
                        esc(p.name()),
                        esc(p.resource()),
                        esc(p.description())));
            }
        }

        lines.add("ASSIGNMENTS");
        for (RoleAssignment assignment : system.getAssignmentManager().findAll()) {
            String expiresAt = assignment instanceof TemporaryAssignment t ? t.getExpiresAt() : "";
            String autoRenew = assignment instanceof TemporaryAssignment t ? String.valueOf(t.isAutoRenew()) : "false";
            lines.add("A\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s".formatted(
                    esc(assignment.assignmentType()),
                    esc(assignment.user().username()),
                    esc(assignment.role().getName()),
                    esc(assignment.metadata().assignedBy()),
                    esc(assignment.metadata().assignedAt()),
                    esc(Objects.toString(assignment.metadata().reason(), "")),
                    esc(expiresAt),
                    esc(autoRenew),
                    esc(String.valueOf(assignment.isActive()))));
        }

        try {
            Files.write(path, lines);
            System.out.println("Saved to: " + path.toAbsolutePath());
        } catch (IOException e) {
            System.out.println("Save error: " + e.getMessage());
        }
    }

    private static void load(Scanner scanner, RBACSystem system) {
        String pathText = ConsoleUtils.promptString(scanner, "File path (default rbac-data.txt): ", false);
        Path path = pathText == null || pathText.isBlank() ? Path.of("rbac-data.txt") : Path.of(pathText.trim());

        List<String> lines;
        try {
            lines = Files.readAllLines(path);
        } catch (IOException e) {
            System.out.println("Load error: " + e.getMessage());
            return;
        }

        UserManager userManager = system.getUserManager();
        RoleManager roleManager = system.getRoleManager();
        AssignmentManager assignmentManager = system.getAssignmentManager();
        userManager.clear();
        roleManager.clear();
        assignmentManager.clear();
        roleManager.setAssignmentManager(assignmentManager);

        for (String line : lines) {
            if (line.isBlank() || line.equals("USERS") || line.equals("ROLES") || line.equals("ASSIGNMENTS")) {
                continue;
            }
            String[] parts = line.split("\t", -1);
            try {
                switch (parts[0]) {
                    case "U" -> userManager.add(User.validate(unesc(parts[1]), unesc(parts[2]), unesc(parts[3])));
                    case "R" -> roleManager.add(new Role(unesc(parts[1]), unesc(parts[2])));
                    case "P" -> roleManager.addPermissionToRole(
                            unesc(parts[1]),
                            new Permission(unesc(parts[2]), unesc(parts[3]), unesc(parts[4]))
                    );
                    case "A" -> loadAssignment(parts, system);
                    default -> {
                    }
                }
            } catch (Exception e) {
                System.out.println("Skipping invalid line: " + line);
            }
        }

        System.out.println("Loaded from: " + path.toAbsolutePath());
    }

    private static void loadAssignment(String[] parts, RBACSystem system) {
        String type = unesc(parts[1]);
        String username = unesc(parts[2]);
        String roleName = unesc(parts[3]);
        String assignedBy = unesc(parts[4]);
        String assignedAt = unesc(parts[5]);
        String reason = unesc(parts[6]);
        String expiresAt = unesc(parts[7]);
        boolean autoRenew = Boolean.parseBoolean(unesc(parts[8]));
        boolean active = Boolean.parseBoolean(unesc(parts[9]));

        User user = system.getUserManager().findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("Unknown user"));
        Role role = system.getRoleManager().findByName(roleName)
                .orElseThrow(() -> new NoSuchElementException("Unknown role"));

        AssignmentMetadata metadata = new AssignmentMetadata(assignedBy, assignedAt, reason);
        if ("TEMPORARY".equalsIgnoreCase(type)) {
            TemporaryAssignment assignment = new TemporaryAssignment(user, role, metadata, expiresAt, autoRenew);
            if (!active) {
                assignment.revoke();
            }
            system.getAssignmentManager().add(assignment);
        } else {
            PermanentAssignment assignment = new PermanentAssignment(user, role, metadata);
            if (!active) {
                assignment.revoke();
            }
            system.getAssignmentManager().add(assignment);
        }
    }

    private static Permission readPermission(Scanner scanner) {
        String name = ConsoleUtils.promptString(scanner, "Permission name: ", true);
        String resource = ConsoleUtils.promptString(scanner, "Resource: ", true);
        String description = ConsoleUtils.promptString(scanner, "Description: ", true);
        return new Permission(name, resource, description);
    }

    private static void printUsers(List<User> users) {
        if (users.isEmpty()) {
            System.out.println("No users found");
            return;
        }
        List<User> sorted = users.stream().sorted(UserSorters.byUsername()).toList();
        String[] headers = {"USERNAME", "FULL NAME", "EMAIL"};
        List<String[]> rows = sorted.stream()
                .map(u -> new String[]{u.username(), u.fullName(), u.email()})
                .toList();
        System.out.println(FormatUtils.formatTable(headers, rows));
    }

    private static void printAssignments(List<RoleAssignment> assignments) {
        if (assignments.isEmpty()) {
            System.out.println("No assignments found");
            return;
        }
        String[] headers = {"USERNAME", "ROLE", "TYPE", "STATUS", "ASSIGNED AT", "ASSIGNMENT ID"};
        List<String[]> rows = assignments.stream()
                .sorted(AssignmentSorters.byAssignmentDate())
                .map(a -> new String[]{
                        a.user().username(),
                        a.role().getName(),
                        a.assignmentType(),
                        status(a),
                        a.metadata().assignedAt(),
                        a.assignmentId()
                })
                .toList();
        System.out.println(FormatUtils.formatTable(headers, rows));
    }

    private static String status(RoleAssignment assignment) {
        return assignment.isActive() ? "ACTIVE" : "INACTIVE";
    }


    private static String esc(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\t", "\\t")
                .replace("\n", "\\n");
    }

    private static String unesc(String value) {
        StringBuilder out = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!escaped) {
                if (c == '\\') {
                    escaped = true;
                } else {
                    out.append(c);
                }
            } else {
                if (c == 't') out.append('\t');
                else if (c == 'n') out.append('\n');
                else out.append(c);
                escaped = false;
            }
        }
        if (escaped) {
            out.append('\\');
        }
        return out.toString();
    }

    private interface RoleFilterAlways {
        RoleFilter TRUE = role -> true;
    }

    private interface AssignmentFilterAlways {
        AssignmentFilter TRUE = assignment -> true;
    }
}

