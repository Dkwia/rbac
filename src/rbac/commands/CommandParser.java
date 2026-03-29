package rbac.commands;

import java.util.ArrayList;
import java.util.List;

public class CommandParser {

    public ParsedCommand parse(String line) {
        List<String> tokens = tokenize(line);
        if (tokens.isEmpty()) {
            return new ParsedCommand("", new String[0]);
        }
        String name = tokens.get(0);
        String[] args = tokens.stream().skip(1).toArray(String[]::new);
        return new ParsedCommand(name, args);
    }

    private List<String> tokenize(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {
            char symbol = line.charAt(i);
            if (symbol == '"') {
                quoted = !quoted;
                continue;
            }
            if (Character.isWhitespace(symbol) && !quoted) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            current.append(symbol);
        }

        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    public record ParsedCommand(String name, String[] args) { }
}
