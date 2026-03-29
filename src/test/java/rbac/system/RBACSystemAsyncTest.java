package test.java.rbac.system;

import org.junit.jupiter.api.Test;
import rbac.AssignmentMetadata;
import rbac.Permission;
import rbac.Role;
import rbac.TemporaryAssignment;
import rbac.User;
import rbac.system.RBACSystem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RBACSystemAsyncTest {

    @Test
    void supportsAsyncSaveAndScheduledExpiration() throws Exception {
        Path file = Files.createTempFile("rbac-", ".bin");

        try (RBACSystem system = new RBACSystem()) {
            system.userManager().add(User.validate("tmp_user", "Temp User", "tmp@mail.com"));
            Role role = new Role("TempRole", "Temporary");
            role.addPermission(new Permission("READ", "users", "read"));
            system.roleManager().add(role);
            system.assignmentManager().add(new TemporaryAssignment(
                    system.userManager().findByUsername("tmp_user").orElseThrow(),
                    role,
                    AssignmentMetadata.now("test", "temporary"),
                    LocalDateTime.now().plusSeconds(1).toString(),
                    false
            ));

            system.saveAsync(file).get(5, TimeUnit.SECONDS);
            assertTrue(Files.size(file) > 0);

            system.startMaintenanceTasks(1);
            TimeUnit.SECONDS.sleep(2);

            assertEquals(0, system.assignmentManager().getActiveAssignments().size());
            assertTrue(system.auditLog().getEntries().stream()
                    .anyMatch(entry -> entry.contains("Expired assignments deactivated")));
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
