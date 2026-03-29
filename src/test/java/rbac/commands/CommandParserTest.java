package test.java.rbac.commands;

import org.junit.jupiter.api.Test;
import rbac.commands.CommandParser;
import rbac.system.RBACSystem;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class CommandParserTest {

    @Test
    void parseAndExecuteSplitsArguments() {
        CommandParser parser = new CommandParser();
        RBACSystem system = new RBACSystem();
        AtomicBoolean executed = new AtomicBoolean(false);

        parser.registerCommand("sample", "sample command", (scanner, s) -> executed.set(true));
        parser.parseAndExecute("sample one two", new Scanner(""), system);

        assertTrue(executed.get());
        assertArrayEquals(new String[]{"one", "two"}, parser.getLastArgs());
    }

    @Test
    void executeUnknownCommandPrintsHint() {
        CommandParser parser = new CommandParser();
        RBACSystem system = new RBACSystem();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        System.setOut(new PrintStream(output));
        try {
            parser.parseAndExecute("missing", new Scanner(""), system);
        } finally {
            System.setOut(oldOut);
        }

        String printed = output.toString();
        assertTrue(printed.contains("Unknown command"));
        assertTrue(printed.contains("help"));
    }
}
