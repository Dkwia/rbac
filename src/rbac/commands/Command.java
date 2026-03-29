package rbac.commands;

@FunctionalInterface
public interface Command {
    String execute(String[] args);
}
