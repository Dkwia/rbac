package rbac.commands;

import rbac.system.RBACSystem;

import java.util.Scanner;

@FunctionalInterface
public interface Command {
    void execute(Scanner scanner, RBACSystem system);
}
