package rbac;

public class Main {

    public static void main(String[] args) {

        // Test User validation
        try {
            User user = User.validate("admin_01",
                    "System Administrator",
                    "admin@example.com");

            System.out.println(user.format());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        try {
            User user = User.validate("admin_02",
                    "System Administrator2",
                    "admin2@example.com");

            System.out.println(user.format());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }


            Permission readUsers =
                new Permission("read", "USERS", "Can view user list");

        Permission writeUsers =
                new Permission("write", "USERS", "Can write in user list");
        Permission deleteUsers =
                new Permission("delete", "USERS", "Can delete from user list");
        Role adminRole =
                new Role("Administrator", "Full system access");

        adminRole.addPermission(readUsers);
        adminRole.addPermission(writeUsers);
        adminRole.addPermission(deleteUsers);

        System.out.println(adminRole.format());

        AssignmentMetadata metadata =
                AssignmentMetadata.now("system", "Initial setup");

        PermanentAssignment assignment =
                new PermanentAssignment(
                        User.validate("admin01","Admin User","admin@test.com"),
                        adminRole,
                        metadata
                );

        System.out.println(assignment.summary());
    }
}