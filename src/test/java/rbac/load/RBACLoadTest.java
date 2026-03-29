package test.java.rbac.load;

import org.junit.jupiter.api.Test;
import rbac.AssignmentMetadata;
import rbac.Permission;
import rbac.PermanentAssignment;
import rbac.Role;
import rbac.User;
import rbac.manager.AssignmentManager;
import rbac.manager.RoleManager;
import rbac.manager.UserManager;
import rbac.model.UserFilters;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RBACLoadTest {

    @Test
    void stressScenarioDoesNotProduceStrangeStates() throws Exception {
        UserManager userManager = new UserManager();
        RoleManager roleManager = new RoleManager();
        AssignmentManager assignmentManager = new AssignmentManager();
        roleManager.setAssignmentManager(assignmentManager);

        Role operator = new Role("Operator", "Operator");
        operator.addPermission(new Permission("READ", "users", "read"));
        roleManager.add(operator);

        int threads = 6;
        int operations = 40;
        ExecutorService executorService = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            final int threadId = t;
            executorService.submit(() -> {
                try {
                    for (int i = 0; i < operations; i++) {
                        String username = "worker_" + threadId + "_" + i;
                        User user = User.validate(username, "Worker " + i, username + "@mail.com");
                        userManager.add(user);
                        userManager.update(username, "Worker Updated " + i, username + "@mail.com");
                        assignmentManager.add(new PermanentAssignment(
                                userManager.findByUsername(username).orElseThrow(),
                                operator,
                                AssignmentMetadata.now("load", "parallel")
                        ));
                        assertTrue(userManager.findByFilterParallel(UserFilters.byUsernameContains("worker_" + threadId)).size() >= 1);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executorService.shutdownNow();

        assertEquals(threads * operations, userManager.count());
        assertEquals(threads * operations, assignmentManager.count());
        assertEquals(threads * operations, assignmentManager.getActiveAssignments().size());
    }
}
