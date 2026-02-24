package test.java.rbac.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rbac.manager.*;
import rbac.model.*;
import rbac.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RBACIntegrationTest {

    private UserManager userManager;
    private RoleManager roleManager;
    private AssignmentManager assignmentManager;

    private User admin;
    private User manager;

    private Role adminRole;
    private Role viewerRole;

    @BeforeEach
    void setup() {
        userManager = new UserManager();
        roleManager = new RoleManager();
        assignmentManager = new AssignmentManager();

        roleManager.setAssignmentManager(assignmentManager);

        admin = User.validate("admin_1", "Admin User", "admin@company.com");
        manager = User.validate("manager_1", "Manager User", "manager@company.com");

        userManager.add(admin);
        userManager.add(manager);

        adminRole = new Role("Administrator", "Full access");
        viewerRole = new Role("Viewer", "Read only");

        adminRole.addPermission(new Permission("READ", "users", "Read users"));
        adminRole.addPermission(new Permission("WRITE", "users", "Write users"));
        adminRole.addPermission(new Permission("DELETE", "users", "Delete users"));

        viewerRole.addPermission(new Permission("READ", "users", "Read users"));

        roleManager.add(adminRole);
        roleManager.add(viewerRole);
    }

    @Test
    void fullSystemWorkflowTest() {

        AssignmentMetadata metadata =
                AssignmentMetadata.now("system", "Initial setup");

        PermanentAssignment adminAssignment =
                new PermanentAssignment(admin, adminRole, metadata);

        assignmentManager.add(adminAssignment);

        TemporaryAssignment tempAssignment =
                new TemporaryAssignment(
                        manager,
                        viewerRole,
                        metadata,
                        LocalDateTime.now().plusDays(1).toString(),
                        false
                );

        assignmentManager.add(tempAssignment);

        assertTrue(assignmentManager.userHasRole(admin, adminRole));
        assertTrue(assignmentManager.userHasRole(manager, viewerRole));

        assertTrue(assignmentManager.userHasPermission(admin, "DELETE", "users"));
        assertFalse(assignmentManager.userHasPermission(manager, "DELETE", "users"));

        Set<Permission> managerPermissions =
                assignmentManager.getUserPermissions(manager);

        assertEquals(1, managerPermissions.size());

        assertThrows(IllegalStateException.class, () ->
                assignmentManager.add(
                        new PermanentAssignment(admin, adminRole, metadata)
                ));

        assignmentManager.revokeAssignment(adminAssignment.assignmentId());

        assertFalse(adminAssignment.isActive());

        assertThrows(IllegalStateException.class, () ->
                roleManager.remove(viewerRole));

        String newDate = LocalDateTime.now().plusDays(3).toString();
        assignmentManager.extendTemporaryAssignment(
                tempAssignment.assignmentId(),
                newDate
        );

        assertTrue(tempAssignment.isActive());

        List<RoleAssignment> active =
                assignmentManager.getActiveAssignments();

        assertEquals(1, active.size());

        assertEquals(2, userManager.count());
        assertEquals(2, roleManager.count());
    }
}
