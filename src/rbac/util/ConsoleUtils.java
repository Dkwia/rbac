package rbac.util;

import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public final class ConsoleUtils {

    private ConsoleUtils() {
    }

    public static String promptString(Scanner scanner, String message, boolean required) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine();
            String normalized = ValidationUtils.normalizeString(input);
            if (!required || (normalized != null && !normalized.isBlank())) {
                return normalized == null ? "" : normalized;
            }
            System.out.println("Value cannot be empty.");
        }
    }

    public static int promptInt(Scanner scanner, String message, int min, int max) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value < min || value > max) {
                    System.out.printf("Enter a number between %d and %d.%n", min, max);
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Invalid number.");
            }
        }
    }

    public static boolean promptYesNo(Scanner scanner, String message) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
            if (input.equals("y") || input.equals("yes") || input.equals("da")) {
                return true;
            }
            if (input.equals("n") || input.equals("no") || input.equals("net")) {
                return false;
            }
            System.out.println("Please enter yes or no.");
        }
    }

    public static <T> T promptChoice(Scanner scanner, String message, List<T> options) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("Options cannot be empty");
        }
        System.out.println(message);
        for (int i = 0; i < options.size(); i++) {
            System.out.printf("%d) %s%n", i + 1, options.get(i));
        }
        int index = promptInt(scanner, "Choose number: ", 1, options.size()) - 1;
        return options.get(index);
    }
}
