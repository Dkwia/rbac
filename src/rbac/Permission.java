package rbac;

import rbac.util.ValidationUtils;

public record Permission(String name, String resource, String description) {

    public Permission {
        name = ValidationUtils.normalizeString(name);
        ValidationUtils.requireNonEmpty(name, "Permission name");

        if (name.contains(" "))
            throw new IllegalArgumentException("Permission name cannot contain spaces");

        name = name.toUpperCase();

        resource = ValidationUtils.normalizeString(resource);
        ValidationUtils.requireNonEmpty(resource, "Resource");
        resource = resource.toLowerCase();

        description = ValidationUtils.normalizeString(description);
        ValidationUtils.requireNonEmpty(description, "Description");
    }

    public String format() {
        return "%s on %s: %s".formatted(name, resource, description);
    }

    public boolean matches(String namePattern, String resourcePattern) {
        return name.contains(namePattern.toUpperCase())
                && resource.contains(resourcePattern.toLowerCase());
    }
}
