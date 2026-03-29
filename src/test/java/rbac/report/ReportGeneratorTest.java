package test.java.rbac.report;

import org.junit.jupiter.api.Test;
import rbac.AssignmentMetadata;
import rbac.Permission;
import rbac.PermanentAssignment;
import rbac.Role;
import rbac.User;
import rbac.manager.AssignmentManager;
import rbac.manager.RoleManager;
import rbac.manager.UserManager;
import rbac.report.ReportGenerator;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportGeneratorTest {

    @Test
    void generatesParallelReports() {
        UserManager userManager = new UserManager();
        RoleManager roleManager = new RoleManager();
        AssignmentManager assignmentManager = new AssignmentManager();
        roleManager.setAssignmentManager(assignmentManager);

        User alice = User.validate("alice", "Alice Doe", "alice@mail.com");
        userManager.add(alice);
        Role admin = new Role("Admin", "Admin role");
        admin.addPermission(new Permission("READ", "users", "read users"));
        admin.addPermission(new Permission("WRITE", "users", "write users"));
        roleManager.add(admin);
        assignmentManager.add(new PermanentAssignment(alice, admin, AssignmentMetadata.now("test", "seed")));

        ReportGenerator reportGenerator = new ReportGenerator();

        String usersReport = reportGenerator.buildUsersReport(userManager, assignmentManager);
        String matrix = reportGenerator.buildPermissionMatrix(userManager, roleManager, assignmentManager);

        assertTrue(usersReport.contains("alice"));
        assertTrue(usersReport.contains("Admin"));
        assertTrue(matrix.contains("READ:users"));
        assertTrue(matrix.contains("alice;Y;Y"));
    }
}
