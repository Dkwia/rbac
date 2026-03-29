package rbac.persistence;

import rbac.AssignmentMetadata;
import rbac.Permission;
import rbac.PermanentAssignment;
import rbac.Role;
import rbac.RoleAssignment;
import rbac.TemporaryAssignment;
import rbac.User;
import rbac.manager.AssignmentManager;
import rbac.manager.RoleManager;
import rbac.manager.UserManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RBACPersistence {

    public void save(Path path,
                     UserManager userManager,
                     RoleManager roleManager,
                     AssignmentManager assignmentManager) throws IOException {
        RBACSnapshot snapshot = new RBACSnapshot(
                userManager.findAll().stream()
                        .map(user -> new RBACSnapshot.UserRecord(
                                user.username(),
                                user.fullName(),
                                user.email()))
                        .toList(),
                roleManager.findAll().stream()
                        .map(role -> new RBACSnapshot.RoleRecord(
                                role.getId(),
                                role.getName(),
                                role.getDescription(),
                                role.getPermissions().stream()
                                        .map(permission -> new RBACSnapshot.PermissionRecord(
                                                permission.name(),
                                                permission.resource(),
                                                permission.description()))
                                        .toList()))
                        .toList(),
                assignmentManager.findAll().stream()
                        .map(this::toRecord)
                        .toList()
        );

        try (ObjectOutputStream objectOutputStream =
                     new ObjectOutputStream(Files.newOutputStream(path))) {
            objectOutputStream.writeObject(snapshot);
        }
    }

    public void load(Path path,
                     UserManager userManager,
                     RoleManager roleManager,
                     AssignmentManager assignmentManager)
            throws IOException, ClassNotFoundException {
        RBACSnapshot snapshot;
        try (ObjectInputStream objectInputStream =
                     new ObjectInputStream(Files.newInputStream(path))) {
            snapshot = (RBACSnapshot) objectInputStream.readObject();
        }

        userManager.clear();
        roleManager.clear();
        assignmentManager.clear();

        for (RBACSnapshot.UserRecord userRecord : snapshot.users()) {
            userManager.add(new User(userRecord.username(), userRecord.fullName(), userRecord.email()));
        }

        for (RBACSnapshot.RoleRecord roleRecord : snapshot.roles()) {
            Role role = new Role(roleRecord.id(), roleRecord.name(), roleRecord.description());
            for (RBACSnapshot.PermissionRecord permissionRecord : roleRecord.permissions()) {
                role.addPermission(new Permission(
                        permissionRecord.name(),
                        permissionRecord.resource(),
                        permissionRecord.description()
                ));
            }
            roleManager.add(role);
        }

        Map<String, User> usersByUsername = userManager.findAll().stream()
                .collect(Collectors.toMap(User::username, Function.identity()));
        Map<String, Role> rolesById = roleManager.findAll().stream()
                .collect(Collectors.toMap(Role::getId, Function.identity()));

        for (RBACSnapshot.AssignmentRecord assignmentRecord : snapshot.assignments()) {
            User user = usersByUsername.get(assignmentRecord.userUsername());
            Role role = rolesById.get(assignmentRecord.roleId());
            if (user == null || role == null) {
                throw new NoSuchElementException("Broken snapshot references");
            }

            AssignmentMetadata metadata = new AssignmentMetadata(
                    assignmentRecord.assignedBy(),
                    assignmentRecord.assignedAt(),
                    assignmentRecord.reason()
            );

            RoleAssignment assignment = switch (assignmentRecord.type()) {
                case "PERMANENT" -> new PermanentAssignment(
                        assignmentRecord.assignmentId(),
                        user,
                        role,
                        metadata,
                        !assignmentRecord.active()
                );
                case "TEMPORARY" -> new TemporaryAssignment(
                        assignmentRecord.assignmentId(),
                        user,
                        role,
                        metadata,
                        assignmentRecord.expiresAt(),
                        assignmentRecord.autoRenew(),
                        assignmentRecord.active()
                );
                default -> throw new IllegalStateException("Unknown assignment type");
            };
            assignmentManager.add(assignment);
        }
    }

    private RBACSnapshot.AssignmentRecord toRecord(RoleAssignment assignment) {
        if (assignment instanceof TemporaryAssignment temporaryAssignment) {
            return new RBACSnapshot.AssignmentRecord(
                    assignment.assignmentId(),
                    assignment.user().username(),
                    assignment.role().getId(),
                    assignment.metadata().assignedBy(),
                    assignment.metadata().assignedAt(),
                    assignment.metadata().reason(),
                    assignment.assignmentType(),
                    assignment.isActive(),
                    temporaryAssignment.getExpiresAt(),
                    temporaryAssignment.isAutoRenew()
            );
        }

        return new RBACSnapshot.AssignmentRecord(
                assignment.assignmentId(),
                assignment.user().username(),
                assignment.role().getId(),
                assignment.metadata().assignedBy(),
                assignment.metadata().assignedAt(),
                assignment.metadata().reason(),
                assignment.assignmentType(),
                assignment.isActive(),
                null,
                false
        );
    }
}
