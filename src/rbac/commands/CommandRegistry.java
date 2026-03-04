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
                String username = ask(scanner, "Username: ");
                String fullName = ask(scanner, "Full name: ");
                String email = ask(scanner, "Email: ");
                User user = User.validate(username, fullName, email);
                system.getUserManager().add(user);
                System.out.println("User created: " + user.format());
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("user-view", "View user with roles and permissions", (scanner, system) -> {
            String username = ask(scanner, "Username: ");
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
                assignments.forEach(a ->
                        System.out.printf("  %s | %s | %s%n", a.assignmentId(), a.role().getName(), status(a)));
            }

            Set<Permission> permissions = system.getAssignmentManager().getUserPermissions(user.get());
            if (permissions.isEmpty()) {
                System.out.println("No active permissions");
            } else {
                System.out.println("Permissions:");
                permissions.stream()
                        .sorted(Comparator.comparing(Permission::resource).thenComparing(Permission::name))
                        .forEach(p -> System.out.println("  - " + p.format()));
            }
        });

        parser.registerCommand("user-update", "Update user full name/email", (scanner, system) -> {
            try {
                String username = ask(scanner, "Username: ");
                String fullName = ask(scanner, "New full name: ");
                String email = ask(scanner, "New email: ");
                system.getUserManager().update(username, fullName, email);
                System.out.println("User updated");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("user-delete", "Delete a user and all their assignments", (scanner, system) -> {
            String username = ask(scanner, "Username: ");
            Optional<User> user = system.getUserManager().findByUsername(username);
            if (user.isEmpty()) {
                System.out.println("User not found");
                return;
            }
            String confirm = ask(scanner, "Type 'da' to confirm deletion: ");
            if (!"da".equalsIgnoreCase(confirm.trim())) {
                System.out.println("Cancelled");
                return;
            }

            int removed = system.getAssignmentManager().removeAssignmentsForUser(user.get());
            system.getUserManager().remove(user.get());
            System.out.printf("User deleted. Removed assignments: %d%n", removed);
        });

        parser.registerCommand("user-search", "Search users by filters", (scanner, system) -> {
            System.out.println("1) username contains");
            System.out.println("2) email contains");
            System.out.println("3) email domain");
            System.out.println("4) full name contains");
            String choice = ask(scanner, "Choice: ");
            String value = ask(scanner, "Value: ");

            UserFilter filter;
            switch (choice) {
                case "1" -> filter = UserFilters.byUsernameContains(value);
                case "2" -> filter = u -> u.email().toLowerCase(Locale.ROOT).contains(value.toLowerCase(Locale.ROOT));
                case "3" -> filter = UserFilters.byEmailDomain(value);
                case "4" -> filter = UserFilters.byFullNameContains(value);
                default -> {
                    System.out.println("Unknown option");
                    return;
                }
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
            System.out.printf("%-20s %-12s %-36s%n", "ROLE", "PERMISSIONS", "ID");
            roles.forEach(r -> System.out.printf("%-20s %-12d %-36s%n",
                    r.getName(),
                    r.getPermissions().size(),
                    r.getId()));
        });

        parser.registerCommand("role-create", "Create role and optionally add permissions", (scanner, system) -> {
            try {
                String name = ask(scanner, "Role name: ");
                String description = ask(scanner, "Description: ");
                Role role = new Role(name, description);
                system.getRoleManager().add(role);

                while (true) {
                    String add = ask(scanner, "Add permission? (y/n): ");
                    if (!"y".equalsIgnoreCase(add)) {
                        break;
                    }
                    Permission permission = readPermission(scanner);
                    role.addPermission(permission);
                }

                System.out.println("Role created: " + role.getName());
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("role-view", "View role details", (scanner, system) -> {
            String name = ask(scanner, "Role name: ");
            Optional<Role> role = system.getRoleManager().findByName(name);
            if (role.isEmpty()) {
                System.out.println("Role not found");
                return;
            }
            System.out.println(role.get().format());
        });

        parser.registerCommand("role-update", "Update role name/description", (scanner, system) -> {
            try {
                String currentName = ask(scanner, "Current role name: ");
                String newName = ask(scanner, "New role name: ");
                String newDescription = ask(scanner, "New description: ");
                system.getRoleManager().update(currentName, newName, newDescription);
                System.out.println("Role updated");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("role-delete", "Delete role if no active assignments", (scanner, system) -> {
            String name = ask(scanner, "Role name: ");
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
                active.forEach(a -> System.out.println("  - " + a.user().username()));
            }
            String confirm = ask(scanner, "Type 'da' to confirm deletion: ");
            if (!"da".equalsIgnoreCase(confirm.trim())) {
                System.out.println("Cancelled");
                return;
            }
            try {
                system.getRoleManager().remove(role);
                System.out.println("Role deleted");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("role-add-permission", "Add permission to role", (scanner, system) -> {
            try {
                String roleName = ask(scanner, "Role name: ");
                Permission permission = readPermission(scanner);
                system.getRoleManager().addPermissionToRole(roleName, permission);
                System.out.println("Permission added");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("role-remove-permission", "Remove role permission by index", (scanner, system) -> {
            String roleName = ask(scanner, "Role name: ");
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

            String idxText = ask(scanner, "Number to remove: ");
            int idx;
            try {
                idx = Integer.parseInt(idxText) - 1;
            } catch (NumberFormatException e) {
                System.out.println("Invalid number");
                return;
            }
            if (idx < 0 || idx >= permissions.size()) {
                System.out.println("Out of range");
                return;
            }

            system.getRoleManager().removePermissionFromRole(roleName, permissions.get(idx));
            System.out.println("Permission removed");
        });

        parser.registerCommand("role-search", "Search roles", (scanner, system) -> {
            System.out.println("1) name contains");
            System.out.println("2) has specific permission");
            System.out.println("3) min permissions count");
            String choice = ask(scanner, "Choice: ");

            RoleFilter filter;
            switch (choice) {
                case "1" -> {
                    String value = ask(scanner, "Substring: ");
                    filter = RoleFilters.byNameContains(value);
                }
                case "2" -> {
                    String name = ask(scanner, "Permission name: ");
                    String resource = ask(scanner, "Resource: ");
                    filter = RoleFilters.hasPermission(name, resource);
                }
                case "3" -> {
                    String min = ask(scanner, "Minimum permissions: ");
                    int n;
                    try {
                        n = Integer.parseInt(min);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid number");
                        return;
                    }
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
            roles.forEach(r -> System.out.printf("- %s (%d perms)%n", r.getName(), r.getPermissions().size()));
        });
    }

    private static void registerAssignmentCommands(CommandParser parser) {
        parser.registerCommand("assign-role", "Assign role to user", (scanner, system) -> {
            try {
                String username = ask(scanner, "Username: ");
                User user = system.getUserManager().findByUsername(username)
                        .orElseThrow(() -> new NoSuchElementException("User not found"));

                List<Role> roles = system.getRoleManager().findAll(RoleFilterAlways.TRUE, RoleSorters.byName());
                if (roles.isEmpty()) {
                    System.out.println("No roles available");
                    return;
                }

                for (int i = 0; i < roles.size(); i++) {
                    System.out.printf("%d) %s%n", i + 1, roles.get(i).getName());
                }
                int roleIndex = parseIndex(ask(scanner, "Role number: "), roles.size());
                Role role = roles.get(roleIndex);

                String type = ask(scanner, "Type (permanent/temporary): ").trim().toLowerCase(Locale.ROOT);
                String reason = ask(scanner, "Reason: ");
                AssignmentMetadata metadata = AssignmentMetadata.now(system.getCurrentUser(), reason);

                if ("temporary".equals(type)) {
                    String expiresAt = ask(scanner, "Expires at (yyyy-MM-ddTHH:mm:ss): ");
                    boolean autoRenew = "y".equalsIgnoreCase(ask(scanner, "Auto renew? (y/n): "));
                    TemporaryAssignment assignment = new TemporaryAssignment(user, role, metadata, expiresAt, autoRenew);
                    system.getAssignmentManager().add(assignment);
                    System.out.println("Temporary role assigned");
                } else if ("permanent".equals(type)) {
                    system.getAssignmentManager().add(new PermanentAssignment(user, role, metadata));
                    System.out.println("Permanent role assigned");
                } else {
                    System.out.println("Unknown type");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("revoke-role", "Revoke assignment from user", (scanner, system) -> {
            String username = ask(scanner, "Username: ");
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
                int index = parseIndex(ask(scanner, "Choose assignment: "), active.size());
                system.getAssignmentManager().revokeAssignment(active.get(index).assignmentId());
                System.out.println("Assignment revoked");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("assignment-list", "List all assignments", (scanner, system) ->
                printAssignments(system.getAssignmentManager().findAll(AssignmentFilterAlways.TRUE, AssignmentSorters.byAssignmentDate())));

        parser.registerCommand("assignment-list-user", "List assignments for username", (scanner, system) -> {
            String username = ask(scanner, "Username: ");
            List<RoleAssignment> assignments = system.getAssignmentManager()
                    .findByFilter(AssignmentFilters.byUsername(username));
            printAssignments(assignments);
        });

        parser.registerCommand("assignment-list-role", "List users assigned to role", (scanner, system) -> {
            String roleName = ask(scanner, "Role name: ");
            List<RoleAssignment> assignments = system.getAssignmentManager()
                    .findByFilter(AssignmentFilters.byRoleName(roleName));
            if (assignments.isEmpty()) {
                System.out.println("No assignments found");
                return;
            }
            assignments.forEach(a -> System.out.printf("- %s (%s)%n", a.user().username(), status(a)));
        });

        parser.registerCommand("assignment-active", "List active assignments", (scanner, system) ->
                printAssignments(system.getAssignmentManager().getActiveAssignments()));

        parser.registerCommand("assignment-expired", "List expired temporary assignments", (scanner, system) ->
                printAssignments(system.getAssignmentManager().getExpiredAssignments()));

        parser.registerCommand("assignment-extend", "Extend temporary assignment", (scanner, system) -> {
            String id = ask(scanner, "Assignment ID (leave empty to search by username+role): ");

            RoleAssignment target;
            if (id == null || id.isBlank()) {
                String username = ask(scanner, "Username: ");
                String roleName = ask(scanner, "Role name: ");
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

            String newDate = ask(scanner, "New expires at (yyyy-MM-ddTHH:mm:ss): ");
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
            String choice = ask(scanner, "Choice: ");

            AssignmentFilter filter;
            switch (choice) {
                case "1" -> filter = AssignmentFilters.byUsername(ask(scanner, "Username: "));
                case "2" -> filter = AssignmentFilters.byRoleName(ask(scanner, "Role name: "));
                case "3" -> filter = AssignmentFilters.byType(ask(scanner, "Type (permanent/temporary): "));
                case "4" -> {
                    String status = ask(scanner, "Status (active/inactive): ");
                    filter = "active".equalsIgnoreCase(status)
                            ? AssignmentFilters.activeOnly()
                            : AssignmentFilters.inactiveOnly();
                }
                case "5" -> filter = AssignmentFilters.assignedAfter(ask(scanner, "Date (yyyy-MM-ddTHH:mm:ss): "));
                case "6" -> filter = AssignmentFilters.expiringBefore(ask(scanner, "Date (yyyy-MM-ddTHH:mm:ss): "));
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
            String username = ask(scanner, "Username: ");
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
            grouped.forEach((resource, permissions) -> {
                System.out.println(resource + ":");
                permissions.stream()
                        .sorted(Comparator.comparing(Permission::name))
                        .forEach(p -> System.out.println("  - " + p.name()));
            });
        });

        parser.registerCommand("permissions-check", "Check user permission and role source", (scanner, system) -> {
            String username = ask(scanner, "Username: ");
            String permissionName = ask(scanner, "Permission name: ");
            String resource = ask(scanner, "Resource: ");

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

        parser.registerCommand("clear", "Clear console output", (scanner, system) -> {
            for (int i = 0; i < 40; i++) {
                System.out.println();
            }
        });

        parser.registerCommand("exit", "Exit application", (scanner, system) -> {
            String confirm = ask(scanner, "Type 'da' to exit: ");
            if (!"da".equalsIgnoreCase(confirm.trim())) {
                System.out.println("Cancelled");
                return;
            }

            String save = ask(scanner, "Save data before exit? (y/n): ");
            if ("y".equalsIgnoreCase(save.trim())) {
                save(scanner, system);
            }
            system.stop();
        });

        parser.registerCommand("save", "Save data to file", CommandRegistry::save);
        parser.registerCommand("load", "Load data from file", CommandRegistry::load);
    }

    private static void save(Scanner scanner, RBACSystem system) {
        String pathText = ask(scanner, "File path (default rbac-data.txt): ");
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
        String pathText = ask(scanner, "File path (default rbac-data.txt): ");
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
        String name = ask(scanner, "Permission name: ");
        String resource = ask(scanner, "Resource: ");
        String description = ask(scanner, "Description: ");
        return new Permission(name, resource, description);
    }

    private static String ask(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static void printUsers(List<User> users) {
        if (users.isEmpty()) {
            System.out.println("No users found");
            return;
        }
        List<User> sorted = users.stream().sorted(UserSorters.byUsername()).toList();
        System.out.printf("%-20s %-25s %-30s%n", "USERNAME", "FULL NAME", "EMAIL");
        sorted.forEach(u -> System.out.printf("%-20s %-25s %-30s%n", u.username(), u.fullName(), u.email()));
    }

    private static void printAssignments(List<RoleAssignment> assignments) {
        if (assignments.isEmpty()) {
            System.out.println("No assignments found");
            return;
        }
        System.out.printf("%-15s %-15s %-11s %-9s %-20s %-36s%n",
                "USERNAME",
                "ROLE",
                "TYPE",
                "STATUS",
                "ASSIGNED AT",
                "ASSIGNMENT ID");
        assignments.stream()
                .sorted(AssignmentSorters.byAssignmentDate())
                .forEach(a -> System.out.printf("%-15s %-15s %-11s %-9s %-20s %-36s%n",
                        a.user().username(),
                        a.role().getName(),
                        a.assignmentType(),
                        status(a),
                        a.metadata().assignedAt(),
                        a.assignmentId()));
    }

    private static String status(RoleAssignment assignment) {
        return assignment.isActive() ? "ACTIVE" : "INACTIVE";
    }

    private static int parseIndex(String value, int size) {
        int index = Integer.parseInt(value) - 1;
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException("Index out of range");
        }
        return index;
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

