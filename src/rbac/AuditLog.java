package rbac;

import rbac.util.DateUtils;
import rbac.util.FormatUtils;
import rbac.util.ValidationUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AuditLog {

    private final List<AuditEntry> entries = new ArrayList<>();

    public record AuditEntry(
            String timestamp,
            String action,
            String performer,
            String target,
            String details
    ) {
    }

    public void log(String action, String performer, String target, String details) {
        ValidationUtils.requireNonEmpty(action, "Action");
        ValidationUtils.requireNonEmpty(performer, "Performer");
        ValidationUtils.requireNonEmpty(target, "Target");
        String entryDetails = details == null ? "" : details;
        entries.add(new AuditEntry(
                DateUtils.getCurrentDateTime(),
                action,
                performer,
                target,
                entryDetails
        ));
    }

    public List<AuditEntry> getAll() {
        return List.copyOf(entries);
    }

    public List<AuditEntry> getByPerformer(String performer) {
        return entries.stream()
                .filter(e -> e.performer().equals(performer))
                .collect(Collectors.toList());
    }

    public List<AuditEntry> getByAction(String action) {
        return entries.stream()
                .filter(e -> e.action().equalsIgnoreCase(action))
                .collect(Collectors.toList());
    }

    public void printLog() {
        if (entries.isEmpty()) {
            System.out.println("No audit entries");
            return;
        }
        String[] headers = {"TIMESTAMP", "ACTION", "PERFORMER", "TARGET", "DETAILS"};
        List<String[]> rows = entries.stream()
                .map(e -> new String[]{e.timestamp(), e.action(), e.performer(), e.target(), e.details()})
                .collect(Collectors.toList());
        System.out.println(FormatUtils.formatTable(headers, rows));
    }

    public void saveToFile(String filename) {
        ValidationUtils.requireNonEmpty(filename, "Filename");
        Path path = Path.of(filename);
        List<String> lines = new ArrayList<>();
        lines.add("AUDIT LOG");
        for (AuditEntry entry : entries) {
            lines.add("%s | %s | %s | %s | %s".formatted(
                    entry.timestamp(),
                    entry.action(),
                    entry.performer(),
                    entry.target(),
                    entry.details()));
        }
        try {
            Files.write(path, lines);
        } catch (IOException e) {
            System.out.println("Audit log save error: " + e.getMessage());
        }
    }
}
