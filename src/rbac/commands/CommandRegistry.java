package rbac.commands;

import java.util.LinkedHashMap;
import java.util.Map;

public class CommandRegistry {

    private final Map<String, Command> commands = new LinkedHashMap<>();
    private final Map<String, String> help = new LinkedHashMap<>();

    public void register(String name, String helpText, Command command) {
        commands.put(name, command);
        help.put(name, helpText);
    }

    public String execute(String name, String[] args) {
        Command command = commands.get(name);
        if (command == null) {
            return "Unknown command: " + name;
        }
        return command.execute(args);
    }

    public String helpText() {
        return help.entrySet().stream()
                .map(entry -> entry.getKey() + " - " + entry.getValue())
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("No commands registered");
    }
}
