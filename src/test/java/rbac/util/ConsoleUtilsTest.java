package test.java.rbac.util;

import org.junit.jupiter.api.Test;
import rbac.util.ConsoleUtils;

import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleUtilsTest {

    @Test
    void promptStringHandlesRequired() {
        Scanner scanner = new Scanner("\nhello\n");
        String value = ConsoleUtils.promptString(scanner, "Value: ", true);
        assertEquals("hello", value);
    }

    @Test
    void promptIntHandlesInvalidInput() {
        Scanner scanner = new Scanner("abc\n5\n");
        int value = ConsoleUtils.promptInt(scanner, "Number: ", 1, 10);
        assertEquals(5, value);
    }

    @Test
    void promptYesNoHandlesYes() {
        Scanner scanner = new Scanner("yes\n");
        assertTrue(ConsoleUtils.promptYesNo(scanner, "Confirm: "));
    }

    @Test
    void promptChoiceSelectsOption() {
        Scanner scanner = new Scanner("2\n");
        String choice = ConsoleUtils.promptChoice(scanner, "Pick:", List.of("one", "two"));
        assertEquals("two", choice);
    }
}
