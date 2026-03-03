package rbac.commands;

import rbac.system.RBACSystem;

import java.util.*;

public class CommandParser {

    private final Map<String, Command> commands = new LinkedHashMap<>();
    private final Map<String, String> commandDescriptions = new LinkedHashMap<>();
    private String[] lastArgs = new String[0];

    public void registerCommand(String name, String description, Command command) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Command name cannot be empty");
        }
        Objects.requireNonNull(description);
        Objects.requireNonNull(command);

        String normalized = name.trim().toLowerCase(Locale.ROOT);
        commands.put(normalized, command);
        commandDescriptions.put(normalized, description);
    }

    public void executeCommand(String commandName, Scanner scanner, RBACSystem system) {
        if (commandName == null || commandName.isBlank()) {
            return;
        }

        Command command = commands.get(commandName.trim().toLowerCase(Locale.ROOT));
        if (command == null) {
            System.out.println("Unknown command: " + commandName);
            System.out.println("Type 'help' to list available commands.");
            return;
        }

        command.execute(scanner, system);
    }

    public void printHelp() {
        System.out.println("Available commands:");
        commandDescriptions.forEach((name, description) ->
                System.out.printf("  %-24s %s%n", name, description));
    }

    public void parseAndExecute(String input, Scanner scanner, RBACSystem system) {
        if (input == null || input.isBlank()) {
            return;
        }

        String trimmed = input.trim();
        String[] parts = trimmed.split("\\s+");
        String commandName = parts[0];
        this.lastArgs = Arrays.copyOfRange(parts, 1, parts.length);

        executeCommand(commandName, scanner, system);
    }

    public String[] getLastArgs() {
        return Arrays.copyOf(lastArgs, lastArgs.length);
    }
}
