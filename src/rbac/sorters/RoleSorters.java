public class RoleSorters {

    public static Comparator<Role> byName() {
        return Comparator.comparing(Role::getName);
    }

    public static Comparator<Role> byPermissionCount() {
        return Comparator.comparingInt(r -> r.getPermissions().size());
    }
}
