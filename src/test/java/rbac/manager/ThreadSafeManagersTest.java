package test.java.rbac.manager;

import org.junit.jupiter.api.Test;
import rbac.AssignmentMetadata;
import rbac.Permission;
import rbac.PermanentAssignment;
import rbac.Role;
import rbac.User;
import rbac.manager.AssignmentManager;
import rbac.manager.RoleManager;
import rbac.manager.UserManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThreadSafeManagersTest {

    @Test
    void managersHandleConcurrentMutationsWithoutDuplicates() throws Exception {
        UserManager userManager = new UserManager();
        RoleManager roleManager = new RoleManager();
        AssignmentManager assignmentManager = new AssignmentManager();
        roleManager.setAssignmentManager(assignmentManager);

        Role role = new Role("Engineer", "Engineering role");
        role.addPermission(new Permission("READ", "users", "read"));
        roleManager.add(role);

        int threads = 8;
        int perThread = 25;
        ExecutorService executorService = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int threadIndex = 0; threadIndex < threads; threadIndex++) {
            final int start = threadIndex * perThread;
            executorService.submit(() -> {
                try {
                    for (int i = 0; i < perThread; i++) {
                        int id = start + i;
                        User user = User.validate("user_" + id, "User " + id, "user" + id + "@mail.com");
                        userManager.add(user);
                        userManager.update(user.username(), "Updated " + id, user.email());
                        assignmentManager.add(new PermanentAssignment(
                                userManager.findByUsername(user.username()).orElseThrow(),
                                role,
                                AssignmentMetadata.now("test", "load")
                        ));
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdownNow();

        assertEquals(threads * perThread, userManager.count());
        assertEquals(threads * perThread, assignmentManager.count());
    }
}
