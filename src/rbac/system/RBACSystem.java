package rbac.system;

import rbac.AssignmentMetadata;
import rbac.Permission;
import rbac.PermanentAssignment;
import rbac.Role;
import rbac.RoleAssignment;
import rbac.TemporaryAssignment;
import rbac.User;
import rbac.audit.AuditLog;
import rbac.commands.CommandParser;
import rbac.commands.CommandRegistry;
import rbac.manager.AssignmentManager;
import rbac.manager.RoleManager;
import rbac.manager.UserManager;
import rbac.model.AssignmentFilters;
import rbac.model.RoleFilters;
import rbac.model.UserFilters;
import rbac.persistence.RBACPersistence;
import rbac.report.ReportGenerator;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class RBACSystem implements AutoCloseable {

    private final UserManager userManager = new UserManager();
    private final RoleManager roleManager = new RoleManager();
    private final AssignmentManager assignmentManager = new AssignmentManager();
    private final ReportGenerator reportGenerator = new ReportGenerator();
    private final RBACPersistence persistence = new RBACPersistence();
    private final AuditLog auditLog = new AuditLog();
    private final BackgroundExecutor backgroundExecutor = new BackgroundExecutor(4);
    private final CommandRegistry commandRegistry = new CommandRegistry();
    private final CommandParser commandParser = new CommandParser();

    public RBACSystem() {
        roleManager.setAssignmentManager(assignmentManager);
        registerCommands();
    }

    public UserManager userManager() {
        return userManager;
    }

    public RoleManager roleManager() {
        return roleManager;
    }

    public AssignmentManager assignmentManager() {
        return assignmentManager;
    }

    public AuditLog auditLog() {
        return auditLog;
    }

    public void startMaintenanceTasks(long periodSeconds) {
        backgroundExecutor.scheduleAtFixedRate(() -> {
            int expired = assignmentManager.deactivateExpiredAssignments();
            if (expired > 0) {
                auditLog.log("Expired assignments deactivated: " + expired);
            }
            auditLog.log("Maintenance stats: " +
                    reportGenerator.buildStatistics(userManager, roleManager, assignmentManager)
                            .replace(System.lineSeparator(), ", "));
        }, periodSeconds, periodSeconds, TimeUnit.SECONDS);
    }

    public void runInteractive(Scanner scanner) {
        System.out.println("RBAC console started. Type 'help' for commands.");
        while (true) {
            System.out.print("> ");
            if (!scanner.hasNextLine()) {
                break;
            }
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            CommandParser.ParsedCommand parsedCommand = commandParser.parse(line);
            if ("exit".equalsIgnoreCase(parsedCommand.name())) {
                System.out.println("Bye.");
                break;
            }
            try {
                System.out.println(commandRegistry.execute(parsedCommand.name(), parsedCommand.args()));
            } catch (Exception exception) {
                System.out.println("ERROR: " + exception.getMessage());
            }
        }
    }

    public Future<?> generateUsersReportAsync() {
        return backgroundExecutor.submit(() ->
                System.out.println(reportGenerator.buildUsersReport(userManager, assignmentManager)));
    }

    public Future<?> saveAsync(Path path) {
        return backgroundExecutor.submit(() -> {
            try {
                persistence.save(path, userManager, roleManager, assignmentManager);
                auditLog.log("Snapshot saved to " + path);
            } catch (IOException ioException) {
                throw new IllegalStateException(ioException);
            }
        });
    }

    private void registerCommands() {
        commandRegistry.register("help", "show available commands", args -> commandRegistry.helpText());
        commandRegistry.register("create-user", "create-user <username> <fullName> <email>", args -> {
            requireArgs(args, 3);
            userManager.add(User.validate(args[0], args[1], args[2]));
            auditLog.log("User created: " + args[0]);
            return "User created";
        });
        commandRegistry.register("update-user", "update-user <username> <fullName> <email>", args -> {
            requireArgs(args, 3);
            userManager.update(args[0], args[1], args[2]);
            auditLog.log("User updated: " + args[0]);
            return "User updated";
        });
        commandRegistry.register("delete-user", "delete-user <username>", args -> {
            requireArgs(args, 1);
            User user = userManager.findByUsername(args[0]).orElseThrow(() -> new NoSuchElementException("User not found"));
            userManager.remove(user);
            auditLog.log("User deleted: " + args[0]);
            return "User deleted";
        });
        commandRegistry.register("list-users", "list-users", args -> formatUsers(userManager.findAll()));
        commandRegistry.register("find-users-parallel", "find-users-parallel username|emailDomain|fullName <value>", args -> {
            requireArgs(args, 2);
            List<User> users = switch (args[0]) {
                case "username" -> userManager.findByFilterParallel(UserFilters.byUsernameContains(args[1]));
                case "emailDomain" -> userManager.findByFilterParallel(UserFilters.byEmailDomain(args[1]));
                case "fullName" -> userManager.findByFilterParallel(UserFilters.byFullNameContains(args[1]));
                default -> throw new IllegalArgumentException("Unsupported filter");
            };
            return formatUsers(users);
        });
        commandRegistry.register("create-role", "create-role <name> <description>", args -> {
            requireArgs(args, 2);
            roleManager.add(new Role(args[0], args[1]));
            auditLog.log("Role created: " + args[0]);
            return "Role created";
        });
        commandRegistry.register("delete-role", "delete-role <name>", args -> {
            requireArgs(args, 1);
            Role role = roleManager.findByName(args[0]).orElseThrow(() -> new NoSuchElementException("Role not found"));
            roleManager.remove(role);
            auditLog.log("Role deleted: " + args[0]);
            return "Role deleted";
        });
        commandRegistry.register("list-roles", "list-roles", args -> formatRoles(roleManager.findAll()));
        commandRegistry.register("find-roles-parallel", "find-roles-parallel name|permission <value> [resource]", args -> {
            requireArgs(args, 2);
            List<Role> roles = switch (args[0]) {
                case "name" -> roleManager.findByFilterParallel(RoleFilters.byNameContains(args[1]));
                case "permission" -> {
                    requireArgs(args, 3);
                    yield roleManager.findByFilterParallel(RoleFilters.hasPermission(args[1], args[2]));
                }
                default -> throw new IllegalArgumentException("Unsupported filter");
            };
            return formatRoles(roles);
        });
        commandRegistry.register("add-permission", "add-permission <roleName> <permName> <resource> <description>", args -> {
            requireArgs(args, 4);
            roleManager.addPermissionToRole(args[0], new Permission(args[1], args[2], args[3]));
            auditLog.log("Permission added to role " + args[0]);
            return "Permission added";
        });
        commandRegistry.register("remove-permission", "remove-permission <roleName> <permName> <resource>", args -> {
            requireArgs(args, 3);
            roleManager.removePermissionFromRole(args[0], args[1], args[2]);
            auditLog.log("Permission removed from role " + args[0]);
            return "Permission removed";
        });
        commandRegistry.register("assign-role", "assign-role <username> <roleName> permanent|temporary [expiresAt] [autoRenew]", args -> {
            requireArgs(args, 3);
            User user = userManager.findByUsername(args[0]).orElseThrow(() -> new NoSuchElementException("User not found"));
            Role role = roleManager.findByName(args[1]).orElseThrow(() -> new NoSuchElementException("Role not found"));
            AssignmentMetadata metadata = AssignmentMetadata.now("console", "manual assignment");
            RoleAssignment assignment = switch (args[2].toLowerCase()) {
                case "permanent" -> new PermanentAssignment(user, role, metadata);
                case "temporary" -> {
                    requireArgs(args, 4);
                    boolean autoRenew = args.length >= 5 && Boolean.parseBoolean(args[4]);
                    yield new TemporaryAssignment(user, role, metadata, args[3], autoRenew);
                }
                default -> throw new IllegalArgumentException("Unknown assignment type");
            };
            assignmentManager.add(assignment);
            auditLog.log("Role assigned: %s -> %s".formatted(args[1], args[0]));
            return assignment.assignmentId();
        });
        commandRegistry.register("revoke-assignment", "revoke-assignment <assignmentId>", args -> {
            requireArgs(args, 1);
            assignmentManager.revokeAssignment(args[0]);
            auditLog.log("Assignment revoked: " + args[0]);
            return "Assignment revoked";
        });
        commandRegistry.register("list-assignments", "list-assignments", args -> formatAssignments(assignmentManager.findAll()));
        commandRegistry.register("find-assignments-parallel", "find-assignments-parallel username|role|active <value>", args -> {
            requireArgs(args, 2);
            List<RoleAssignment> assignments = switch (args[0]) {
                case "username" -> assignmentManager.findByFilterParallel(AssignmentFilters.byUsername(args[1]));
                case "role" -> assignmentManager.findByFilterParallel(AssignmentFilters.byRoleName(args[1]));
                case "active" -> Boolean.parseBoolean(args[1])
                        ? assignmentManager.findByFilterParallel(AssignmentFilters.activeOnly())
                        : assignmentManager.findByFilterParallel(AssignmentFilters.inactiveOnly());
                default -> throw new IllegalArgumentException("Unsupported filter");
            };
            return formatAssignments(assignments);
        });
        commandRegistry.register("report-users", "build user report using parallelStream", args ->
                reportGenerator.buildUsersReport(userManager, assignmentManager));
        commandRegistry.register("report-users-async", "build user report in background", args -> {
            generateUsersReportAsync();
            return "Report generation submitted";
        });
        commandRegistry.register("report-matrix", "build permission matrix using parallelStream", args ->
                reportGenerator.buildPermissionMatrix(userManager, roleManager, assignmentManager));
        commandRegistry.register("stats", "show statistics", args ->
                reportGenerator.buildStatistics(userManager, roleManager, assignmentManager));
        commandRegistry.register("save", "save <file>", args -> {
            requireArgs(args, 1);
            try {
                persistence.save(Path.of(args[0]), userManager, roleManager, assignmentManager);
                auditLog.log("Snapshot saved to " + args[0]);
                return "Saved";
            } catch (IOException ioException) {
                throw new IllegalStateException(ioException);
            }
        });
        commandRegistry.register("save-async", "save-async <file>", args -> {
            requireArgs(args, 1);
            saveAsync(Path.of(args[0]));
            return "Save submitted";
        });
        commandRegistry.register("load", "load <file>", args -> {
            requireArgs(args, 1);
            try {
                persistence.load(Path.of(args[0]), userManager, roleManager, assignmentManager);
                auditLog.log("Snapshot loaded from " + args[0]);
                return "Loaded";
            } catch (IOException | ClassNotFoundException exception) {
                throw new IllegalStateException(exception);
            }
        });
        commandRegistry.register("show-audit", "show-audit", args -> String.join(System.lineSeparator(), auditLog.getEntries()));
        commandRegistry.register("demo-data", "load a small in-memory demo", args -> {
            bootstrapDemoData();
            return "Demo data loaded";
        });
    }

    private void bootstrapDemoData() {
        if (userManager.count() > 0 || roleManager.count() > 0) {
            throw new IllegalStateException("System is not empty");
        }
        userManager.add(User.validate("admin", "System Admin", "admin@example.com"));
        userManager.add(User.validate("analyst", "Data Analyst", "analyst@example.com"));
        Role admin = new Role("Administrator", "Full access");
        admin.addPermission(new Permission("READ", "users", "Read users"));
        admin.addPermission(new Permission("WRITE", "users", "Write users"));
        Role viewer = new Role("Viewer", "Read only");
        viewer.addPermission(new Permission("READ", "users", "Read users"));
        roleManager.add(admin);
        roleManager.add(viewer);
        assignmentManager.add(new PermanentAssignment(
                userManager.findByUsername("admin").orElseThrow(),
                admin,
                AssignmentMetadata.now("bootstrap", "demo")
        ));
        assignmentManager.add(new TemporaryAssignment(
                userManager.findByUsername("analyst").orElseThrow(),
                viewer,
                AssignmentMetadata.now("bootstrap", "demo"),
                LocalDateTime.now().plusMinutes(30).toString(),
                false
        ));
    }

    private void requireArgs(String[] args, int expected) {
        if (args.length < expected) {
            throw new IllegalArgumentException("Expected at least " + expected + " arguments");
        }
    }

    private String formatUsers(List<User> users) {
        return users.stream()
                .sorted(Comparator.comparing(User::username))
                .map(User::format)
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("No users");
    }

    private String formatRoles(List<Role> roles) {
        return roles.stream()
                .sorted(Comparator.comparing(Role::getName))
                .map(role -> "%s | permissions=%d".formatted(role.getName(), role.getPermissions().size()))
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("No roles");
    }

    private String formatAssignments(List<RoleAssignment> assignments) {
        return assignments.stream()
                .sorted(Comparator.comparing(RoleAssignment::assignmentId))
                .map(assignment -> "%s | user=%s | role=%s | active=%s".formatted(
                        assignment.assignmentId(),
                        assignment.user().username(),
                        assignment.role().getName(),
                        assignment.isActive()))
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("No assignments");
    }

    @Override
    public void close() {
        backgroundExecutor.close();
        auditLog.close();
    }
}
