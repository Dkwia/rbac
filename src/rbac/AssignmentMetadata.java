package rbac;

import rbac.util.DateUtils;

public record AssignmentMetadata(String assignedBy,
                                 String assignedAt,
                                 String reason) {

    public static AssignmentMetadata now(String assignedBy, String reason) {
        return new AssignmentMetadata(
                assignedBy,
                DateUtils.getCurrentDate(),
                reason
        );
    }

    public String format() {
        return "Assigned by: %s at %s\nReason: %s"
                .formatted(assignedBy, assignedAt,
                        reason == null ? "N/A" : reason);
    }
}
