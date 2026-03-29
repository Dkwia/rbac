package rbac.manager;

import rbac.Permission;
import rbac.model.*;
import rbac.repository.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import rbac.Role;


public class RoleManager implements Repository<Role> {

    private final Map<String, Role> rolesById = new ConcurrentHashMap<>();
    private final Map<String, Role> rolesByName = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private volatile AssignmentManager assignmentManager;

    public void setAssignmentManager(AssignmentManager assignmentManager) {
        this.assignmentManager = assignmentManager;
    }

    @Override
    public void add(Role role) {
        Objects.requireNonNull(role);
        lock.writeLock().lock();
        try {
            if (rolesByName.containsKey(role.getName())) {
                throw new IllegalArgumentException(
                        "Role '" + role.getName() + "' already exists");
            }
            rolesById.put(role.getId(), role);
            rolesByName.put(role.getName(), role);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean remove(Role role) {
        if (role == null) return false;
        lock.writeLock().lock();
        try {
            if (assignmentManager != null &&
                    assignmentManager.hasActiveAssignmentsForRole(role)) {
                throw new IllegalStateException(
                        "Cannot remove role assigned to users");
            }
            rolesByName.remove(role.getName());
            return rolesById.remove(role.getId()) != null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<Role> findById(String id) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(rolesById.get(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Role> findAll() {
        return snapshotRoles();
    }

    @Override
    public int count() {
        lock.readLock().lock();
        try {
            return rolesById.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            rolesById.clear();
            rolesByName.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Optional<Role> findByName(String name) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(rolesByName.get(name));
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean exists(String name) {
        lock.readLock().lock();
        try {
            return rolesByName.containsKey(name);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Role> findByFilter(RoleFilter filter) {
        return snapshotRoles().stream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<Role> findByFilterParallel(RoleFilter filter) {
        return snapshotRoles().parallelStream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<Role> findAll(RoleFilter filter, Comparator<Role> sorter) {
        return snapshotRoles().stream()
                .filter(filter::test)
                .sorted(sorter)
                .collect(Collectors.toList());
    }

    public void addPermissionToRole(String roleName, Permission permission) {
        lock.writeLock().lock();
        try {
            Role role = Optional.ofNullable(rolesByName.get(roleName))
                    .orElseThrow(() ->
                            new NoSuchElementException("Role not found"));
            role.addPermission(permission);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removePermissionFromRole(String roleName, Permission permission) {
        removePermissionFromRole(roleName, permission.name(), permission.resource());
    }

    public void removePermissionFromRole(String roleName, String permissionName, String resource) {
        lock.writeLock().lock();
        try {
            Role role = Optional.ofNullable(rolesByName.get(roleName))
                    .orElseThrow(() ->
                            new NoSuchElementException("Role not found"));
            if (!role.removePermission(permissionName, resource)) {
                throw new NoSuchElementException("Permission not found in role");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<Role> findRolesWithPermission(String name, String resource) {
        return snapshotRoles().stream()
                .filter(r -> r.hasPermission(name, resource))
                .collect(Collectors.toList());
    }

    private List<Role> snapshotRoles() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(rolesById.values());
        } finally {
            lock.readLock().unlock();
        }
    }
}
