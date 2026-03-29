package rbac.manager;

import rbac.*;
import rbac.model.*;
import rbac.repository.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class AssignmentManager implements Repository<RoleAssignment> {

    private final Map<String, RoleAssignment> assignments = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public void add(RoleAssignment assignment) {
        Objects.requireNonNull(assignment);
        lock.writeLock().lock();
        try {
            boolean duplicate = assignments.values().stream()
                    .anyMatch(a ->
                            a.user().equals(assignment.user()) &&
                                    a.role().equals(assignment.role()) &&
                                    a.isActive());
            if (duplicate) {
                throw new IllegalStateException(
                        "Role already assigned to user");
            }
            assignments.put(assignment.assignmentId(), assignment);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean remove(RoleAssignment assignment) {
        if (assignment == null) return false;
        lock.writeLock().lock();
        try {
            return assignments.remove(assignment.assignmentId()) != null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<RoleAssignment> findById(String id) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(assignments.get(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<RoleAssignment> findAll() {
        return snapshotAssignments();
    }

    @Override
    public int count() {
        lock.readLock().lock();
        try {
            return assignments.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            assignments.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<RoleAssignment> findByUser(User user) {
        return snapshotAssignments().stream()
                .filter(a -> a.user().equals(user))
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findByRole(Role role) {
        return snapshotAssignments().stream()
                .filter(a -> a.role().equals(role))
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findByFilter(AssignmentFilter filter) {
        return snapshotAssignments().stream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findByFilterParallel(AssignmentFilter filter) {
        return snapshotAssignments().parallelStream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findAll(AssignmentFilter filter,
                                        Comparator<RoleAssignment> sorter) {
        return snapshotAssignments().stream()
                .filter(filter::test)
                .sorted(sorter)
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> getActiveAssignments() {
        return snapshotAssignments().stream()
                .filter(RoleAssignment::isActive)
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> getExpiredAssignments() {
        return snapshotAssignments().stream()
                .filter(a -> !a.isActive())
                .collect(Collectors.toList());
    }

    public boolean userHasRole(User user, Role role) {
        return snapshotAssignments().stream()
                .anyMatch(a ->
                        a.user().equals(user) &&
                        a.role().equals(role) &&
                        a.isActive());
    }

    public boolean userHasPermission(User user,
                                     String permissionName,
                                     String resource) {

        return getUserPermissions(user).stream()
                .anyMatch(p ->
                        p.name().equalsIgnoreCase(permissionName) &&
                        p.resource().equalsIgnoreCase(resource));
    }

    public Set<Permission> getUserPermissions(User user) {
        return snapshotAssignments().stream()
                .filter(a -> a.user().equals(user))
                .filter(RoleAssignment::isActive)
                .flatMap(a -> a.role().getPermissions().stream())
                .collect(Collectors.toSet());
    }

    public void revokeAssignment(String assignmentId) {
        lock.writeLock().lock();
        try {
            RoleAssignment assignment = Optional.ofNullable(assignments.get(assignmentId))
                    .orElseThrow(() ->
                            new NoSuchElementException("Assignment not found"));
            if (assignment instanceof PermanentAssignment p) {
                p.revoke();
            } else if (assignment instanceof TemporaryAssignment t) {
                t.deactivate();
            } else {
                assignments.remove(assignmentId);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void extendTemporaryAssignment(String assignmentId,
                                          String newExpirationDate) {
        lock.writeLock().lock();
        try {
            RoleAssignment assignment = Optional.ofNullable(assignments.get(assignmentId))
                    .orElseThrow(() ->
                            new NoSuchElementException("Assignment not found"));
            if (!(assignment instanceof TemporaryAssignment t)) {
                throw new IllegalStateException(
                        "Not a temporary assignment");
            }
            t.extend(newExpirationDate);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean hasActiveAssignmentsForRole(Role role) {
        return snapshotAssignments().stream()
                .anyMatch(a ->
                        a.role().equals(role) &&
                        a.isActive());
    }

    public int deactivateExpiredAssignments() {
        LocalDateTime now = LocalDateTime.now();
        int updated = 0;
        for (RoleAssignment assignment : snapshotAssignments()) {
            if (assignment instanceof TemporaryAssignment temporaryAssignment
                    && temporaryAssignment.deactivateIfExpired(now)) {
                updated++;
            }
        }
        return updated;
    }

    private List<RoleAssignment> snapshotAssignments() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(assignments.values());
        } finally {
            lock.readLock().unlock();
        }
    }
}
