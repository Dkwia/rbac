package rbac;

public record Permission(String name, String resource, String description) {

    public Permission {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Permission name cannot be empty");

        if (name.contains(" "))
            throw new IllegalArgumentException("Permission name cannot contain spaces");

        name = name.toUpperCase();

        if (resource == null || resource.isBlank())
            throw new IllegalArgumentException("Resource cannot be empty");

        resource = resource.toLowerCase();

        if (description == null || description.isBlank())
            throw new IllegalArgumentException("Description cannot be empty");
    }

    public String format() {
        return "%s on %s: %s".formatted(name, resource, description);
    }

    public boolean matches(String namePattern, String resourcePattern) {
        return name.contains(namePattern.toUpperCase())
                && resource.contains(resourcePattern.toLowerCase());
    }
}