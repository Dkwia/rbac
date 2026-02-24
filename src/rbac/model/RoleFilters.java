package rbac.model;

import rbac.Permission;
import rbac.model.RoleFilter;

public class RoleFilters {

    public static RoleFilter byName(String name) {
        return role -> role.getName().equals(name);
    }

    public static RoleFilter byNameContains(String substring) {
        return role -> role.getName()
                .toLowerCase()
                .contains(substring.toLowerCase());
    }

    public static RoleFilter hasPermission(Permission permission) {
        return role -> role.hasPermission(permission);
    }

    public static RoleFilter hasPermission(String name, String resource) {
        return role -> role.hasPermission(name, resource);
    }

    public static RoleFilter hasAtLeastNPermissions(int n) {
        return role -> role.getPermissions().size() >= n;
    }
}
