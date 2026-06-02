package pt.lourenco.optimization.jmetal.constraints.model;

public enum ConstraintGoal {
    HARD,
    SOFT;

    public static ConstraintGoal fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Constraint goal is required.");
        }

        return switch (value.trim().toLowerCase()) {
            case "hard" -> HARD;
            case "soft" -> SOFT;
            default -> throw new IllegalArgumentException("Unsupported constraint goal: " + value);
        };
    }
}
