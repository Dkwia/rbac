package rbac;

import java.io.Serializable;
import java.time.LocalDateTime;

public record AssignmentMetadata(String assignedBy,
                                 String assignedAt,
                                 String reason) implements Serializable {

    public static AssignmentMetadata now(String assignedBy, String reason) {
        return new AssignmentMetadata(
                assignedBy,
                LocalDateTime.now().toString(),
                reason
        );
    }

    public String format() {
        return "Assigned by: %s at %s\nReason: %s"
                .formatted(assignedBy, assignedAt,
                        reason == null ? "N/A" : reason);
    }
}
