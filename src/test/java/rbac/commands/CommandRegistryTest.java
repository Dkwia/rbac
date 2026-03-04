package test.java.rbac.commands;

import org.junit.jupiter.api.Test;
import rbac.TemporaryAssignment;
import rbac.User;
import rbac.commands.CommandParser;
import rbac.commands.CommandRegistry;
import rbac.system.RBACSystem;

import java.time.LocalDateTime;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class CommandRegistryTest {

    @Test
    void userCreateCommandAddsUser() {
        RBACSystem system = new RBACSystem();
        system.initialize();
        CommandParser parser = new CommandParser();
        CommandRegistry.registerDefaultCommands(parser);

        Scanner scanner = new Scanner("new_user\nNew User\nnew.user@company.com\n");
        parser.parseAndExecute("user-create", scanner, system);

        assertTrue(system.getUserManager().findByUsername("new_user").isPresent());
    }

    @Test
    void roleUpdateCommandRenamesRole() {
        RBACSystem system = new RBACSystem();
        system.initialize();
        CommandParser parser = new CommandParser();
        CommandRegistry.registerDefaultCommands(parser);

        Scanner scanner = new Scanner("Viewer\nObserver\nRead only observer\n");
        parser.parseAndExecute("role-update", scanner, system);

        assertTrue(system.getRoleManager().findByName("Observer").isPresent());
        assertTrue(system.getRoleManager().findByName("Viewer").isEmpty());
    }

    @Test
    void assignRoleCreatesTemporaryAssignment() {
        RBACSystem system = new RBACSystem();
        system.initialize();
        system.getUserManager().add(User.validate("john_1", "John Smith", "john@company.com"));

        CommandParser parser = new CommandParser();
        CommandRegistry.registerDefaultCommands(parser);

        String expiresAt = LocalDateTime.now().plusDays(2).withNano(0).toString();
        Scanner scanner = new Scanner("john_1\n2\ntemporary\nproject work\n" + expiresAt + "\nn\n");
        parser.parseAndExecute("assign-role", scanner, system);

        assertTrue(system.getAssignmentManager().findByUser(User.validate("john_1", "John Smith", "john@company.com"))
                .stream()
                .anyMatch(a -> a instanceof TemporaryAssignment));
    }

    @Test
    void exitCommandStopsSystem() {
        RBACSystem system = new RBACSystem();
        system.initialize();
        CommandParser parser = new CommandParser();
        CommandRegistry.registerDefaultCommands(parser);

        Scanner scanner = new Scanner("da\nn\n");
        parser.parseAndExecute("exit", scanner, system);

        assertFalse(system.isRunning());
    }
}
