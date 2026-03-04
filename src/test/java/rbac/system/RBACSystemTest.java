package rbac.system;

import org.junit.jupiter.api.Test;
import rbac.Role;
import rbac.User;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RBACSystemTest {

    @Test
    void initializeCreatesDefaultData() {
        RBACSystem system = new RBACSystem();
        system.initialize();

        assertTrue(system.getUserManager().count() >= 1);
        assertTrue(system.getRoleManager().count() >= 3);
        assertTrue(system.getAssignmentManager().count() >= 1);

        Optional<User> admin = system.getUserManager().findByUsername("admin");
        assertTrue(admin.isPresent());

        Optional<Role> adminRole = system.getRoleManager().findByName("Admin");
        assertTrue(adminRole.isPresent());
        assertTrue(system.getAssignmentManager().userHasRole(admin.get(), adminRole.get()));
    }

    @Test
    void generateStatisticsContainsCoreMetrics() {
        RBACSystem system = new RBACSystem();
        system.initialize();

        String stats = system.generateStatistics();

        assertTrue(stats.contains("Users:"));
        assertTrue(stats.contains("Roles:"));
        assertTrue(stats.contains("Assignments:"));
        assertTrue(stats.contains("Top 3 roles:"));
    }
}
