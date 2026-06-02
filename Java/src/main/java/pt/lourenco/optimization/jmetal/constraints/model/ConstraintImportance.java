package pt.lourenco.optimization.jmetal.constraints.model;

public enum ConstraintImportance {
    LOW(1.0),
    MEDIUM(3.0),
    HIGH(6.0);

    private final double weight;

    ConstraintImportance(double weight) {
        this.weight = weight;
    }

    public double getWeight() {
        return weight;
    }

    public static ConstraintImportance fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Constraint importance is required.");
        }

        return switch (value.trim().toLowerCase()) {
            case "low" -> LOW;
            case "medium" -> MEDIUM;
            case "high" -> HIGH;
            default -> throw new IllegalArgumentException("Unsupported constraint importance: " + value);
        };
    }
}
