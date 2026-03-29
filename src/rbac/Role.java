package rbac;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Role implements Serializable {

    private final String id;
    private String name;
    private String description;
    private final Set<Permission> permissions = ConcurrentHashMap.newKeySet();

    public Role(String name, String description) {
        this(UUID.randomUUID().toString(), name, description);
    }

    public Role(String id, String name, String description) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNull(description);
    }

    public void addPermission(Permission permission) {
        permissions.add(permission);
    }

    public void removePermission(Permission permission) {
        permissions.remove(permission);
    }

    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }

    public boolean hasPermission(String permissionName, String resource) {
        return permissions.stream()
                .anyMatch(p -> p.name().equalsIgnoreCase(permissionName)
                        && p.resource().equalsIgnoreCase(resource));
    }

    public Set<Permission> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }

    public String getId() { return id; }

    public String getName() { return name; }

    public String getDescription() { return description; }

    public boolean removePermission(String permissionName, String resource) {
        return permissions.removeIf(permission ->
                permission.name().equalsIgnoreCase(permissionName)
                        && permission.resource().equalsIgnoreCase(resource));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role role)) return false;
        return id.equals(role.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Role{name='%s', id='%s'}".formatted(name, id);
    }

    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append("Role: ").append(name)
                .append(" [ID: ").append(id).append("]\n")
                .append("Description: ").append(description).append("\n")
                .append("Permissions (").append(permissions.size()).append("):\n");

        for (Permission p : permissions) {
            sb.append("- ").append(p.format()).append("\n");
        }

        return sb.toString();
    }
}
