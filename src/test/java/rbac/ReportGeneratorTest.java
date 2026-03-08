package test.java.rbac;

import org.junit.jupiter.api.Test;
import rbac.*;
import rbac.manager.AssignmentManager;
import rbac.manager.RoleManager;
import rbac.manager.UserManager;

import static org.junit.jupiter.api.Assertions.*;

class ReportGeneratorTest {

    @Test
    void generatesReports() {
        UserManager userManager = new UserManager();
        RoleManager roleManager = new RoleManager();
        AssignmentManager assignmentManager = new AssignmentManager();

        User user = User.validate("john_doe", "John Doe", "john@company.com");
        userManager.add(user);

        Role role = new Role("Manager", "Manage users");
        role.addPermission(new Permission("READ", "users", "Read users"));
        roleManager.add(role);

        AssignmentMetadata metadata = AssignmentMetadata.now("admin", "init");
        assignmentManager.add(new PermanentAssignment(user, role, metadata));

        ReportGenerator generator = new ReportGenerator();
        String userReport = generator.generateUserReport(userManager, assignmentManager);
        String roleReport = generator.generateRoleReport(roleManager, assignmentManager);
        String matrix = generator.generatePermissionMatrix(userManager, assignmentManager);

        assertTrue(userReport.contains("john_doe"));
        assertTrue(roleReport.contains("Manager"));
        assertTrue(matrix.contains("users"));
    }
}
