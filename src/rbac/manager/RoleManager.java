package rbac.manager;

import rbac.Permission;
import rbac.model.*;
import rbac.repository.Repository;
import rbac.util.ValidationUtils;

import java.util.*;
import java.util.stream.Collectors;
import rbac.Role;


public class RoleManager implements Repository<Role> {

    private final Map<String, Role> rolesById = new HashMap<>();
    private final Map<String, Role> rolesByName = new HashMap<>();

    private AssignmentManager assignmentManager;

    public void setAssignmentManager(AssignmentManager assignmentManager) {
        this.assignmentManager = assignmentManager;
    }

    @Override
    public void add(Role role) {
        Objects.requireNonNull(role);

        if (rolesByName.containsKey(role.getName())) {
            throw new IllegalArgumentException(
                    "Role '" + role.getName() + "' already exists");
        }

        rolesById.put(role.getId(), role);
        rolesByName.put(role.getName(), role);
    }

    @Override
    public boolean remove(Role role) {
        if (role == null) return false;

        if (assignmentManager != null &&
                assignmentManager.hasActiveAssignmentsForRole(role)) {

            throw new IllegalStateException(
                    "Cannot remove role assigned to users");
        }

        rolesByName.remove(role.getName());
        return rolesById.remove(role.getId()) != null;
    }

    @Override
    public Optional<Role> findById(String id) {
        return Optional.ofNullable(rolesById.get(id));
    }

    @Override
    public List<Role> findAll() {
        return new ArrayList<>(rolesById.values());
    }

    @Override
    public int count() {
        return rolesById.size();
    }

    @Override
    public void clear() {
        rolesById.clear();
        rolesByName.clear();
    }

    public Optional<Role> findByName(String name) {
        return Optional.ofNullable(rolesByName.get(name));
    }

    public boolean exists(String name) {
        return rolesByName.containsKey(name);
    }

    public List<Role> findByFilter(RoleFilter filter) {
        return rolesById.values().stream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<Role> findAll(RoleFilter filter, Comparator<Role> sorter) {
        return rolesById.values().stream()
                .filter(filter::test)
                .sorted(sorter)
                .collect(Collectors.toList());
    }

    public void addPermissionToRole(String roleName, Permission permission) {
        Role role = findByName(roleName)
                .orElseThrow(() ->
                        new NoSuchElementException("Role not found"));

        role.addPermission(permission);
    }

    public void removePermissionFromRole(String roleName, Permission permission) {
        Role role = findByName(roleName)
                .orElseThrow(() ->
                        new NoSuchElementException("Role not found"));

        role.removePermission(permission);
    }

    public List<Role> findRolesWithPermission(String name, String resource) {
        return rolesById.values().stream()
                .filter(r -> r.hasPermission(name, resource))
                .collect(Collectors.toList());
    }

    public void update(String currentName, String newName, String newDescription) {
        Role role = findByName(currentName)
                .orElseThrow(() -> new NoSuchElementException("Role not found"));

        ValidationUtils.requireNonEmpty(newName, "Role name");
        ValidationUtils.requireNonEmpty(newDescription, "Description");
        String targetName = ValidationUtils.normalizeString(newName);

        if (!currentName.equals(targetName) && rolesByName.containsKey(targetName)) {
            throw new IllegalArgumentException("Role '" + targetName + "' already exists");
        }

        rolesByName.remove(currentName);
        role.setName(targetName);
        role.setDescription(newDescription);
        rolesByName.put(targetName, role);
    }
}
