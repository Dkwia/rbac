package rbac.sorters;

import rbac.Role;
import rbac.Permission;

import java.util.Comparator;

public class RoleSorters {

    public static Comparator<Role> byName() {
        return Comparator.comparing(Role::getName);
    }

    public static Comparator<Role> byPermissionCount() {
        return Comparator.comparingInt(r -> r.getPermissions().size());
    }
}
