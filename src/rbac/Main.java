package rbac;

import rbac.system.RBACSystem;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        try (RBACSystem system = new RBACSystem();
             Scanner scanner = new Scanner(System.in)) {
            system.startMaintenanceTasks(5);
            system.runInteractive(scanner);
        }
    }
}
