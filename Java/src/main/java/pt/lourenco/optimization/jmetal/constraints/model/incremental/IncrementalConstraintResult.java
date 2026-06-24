package pt.lourenco.optimization.jmetal.constraints.model.incremental;

public record IncrementalConstraintResult(
        double rawViolation
) {
    public static IncrementalConstraintResult zero() {
        return new IncrementalConstraintResult(0.0);
    }
}