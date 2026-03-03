package rbac;

import rbac.commands.CommandParser;
import rbac.commands.CommandRegistry;
import rbac.system.RBACSystem;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        RBACSystem system = new RBACSystem();
        system.initialize();

        CommandParser parser = new CommandParser();
        CommandRegistry.registerDefaultCommands(parser);

        System.out.println("RBAC console started. Type 'help' for commands.");

        try (Scanner scanner = new Scanner(System.in)) {
            while (system.isRunning()) {
                System.out.print("rbac> ");
                if (!scanner.hasNextLine()) {
                    break;
                }
                String input = scanner.nextLine();
                parser.parseAndExecute(input, scanner, system);
            }
        }
    }
}
