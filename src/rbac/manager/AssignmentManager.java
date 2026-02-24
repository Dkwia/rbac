package rbac.manager;

import rbac.model.*;
import rbac.filters.AssignmentFilter;
import rbac.repository.Repository;

import java.util.*;
import java.util.stream.Collectors;

public class AssignmentManager implements Repository<RoleAssignment> {

    private final Map<String, RoleAssignment> assignments = new HashMap<>();

    @Override
    public void add(RoleAssignment assignment) {
        Objects.requireNonNull(assignment);

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
    }

    @Override
    public boolean remove(RoleAssignment assignment) {
        if (assignment == null) return false;
        return assignments.remove(assignment.assignmentId()) != null;
    }

    @Override
    public Optional<RoleAssignment> findById(String id) {
        return Optional.ofNullable(assignments.get(id));
    }

    @Override
    public List<RoleAssignment> findAll() {
        return new ArrayList<>(assignments.values());
    }

    @Override
    public int count() {
        return assignments.size();
    }

    @Override
    public void clear() {
        assignments.clear();
    }

    public List<RoleAssignment> findByUser(User user) {
        return assignments.values().stream()
                .filter(a -> a.user().equals(user))
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findByRole(Role role) {
        return assignments.values().stream()
                .filter(a -> a.role().equals(role))
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findByFilter(AssignmentFilter filter) {
        return assignments.values().stream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findAll(AssignmentFilter filter,
                                        Comparator<RoleAssignment> sorter) {
        return assignments.values().stream()
                .filter(filter::test)
                .sorted(sorter)
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> getActiveAssignments() {
        return assignments.values().stream()
                .filter(RoleAssignment::isActive)
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> getExpiredAssignments() {
        return assignments.values().stream()
                .filter(a -> !a.isActive())
                .collect(Collectors.toList());
    }

    public boolean userHasRole(User user, Role role) {
        return assignments.values().stream()
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
        return assignments.values().stream()
                .filter(a -> a.user().equals(user))
                .filter(RoleAssignment::isActive)
                .flatMap(a -> a.role().getPermissions().stream())
                .collect(Collectors.toSet());
    }

    public void revokeAssignment(String assignmentId) {
        RoleAssignment assignment = findById(assignmentId)
                .orElseThrow(() ->
                        new NoSuchElementException("Assignment not found"));

        if (assignment instanceof PermanentAssignment p) {
            p.revoke();
        } else {
            assignments.remove(assignmentId);
        }
    }

    public void extendTemporaryAssignment(String assignmentId,
                                          String newExpirationDate) {

        RoleAssignment assignment = findById(assignmentId)
                .orElseThrow(() ->
                        new NoSuchElementException("Assignment not found"));

        if (!(assignment instanceof TemporaryAssignment t)) {
            throw new IllegalStateException(
                    "Not a temporary assignment");
        }

        t.extend(newExpirationDate);
    }

    public boolean hasActiveAssignmentsForRole(Role role) {
        return assignments.values().stream()
                .anyMatch(a ->
                        a.role().equals(role) &&
                        a.isActive());
    }
}
