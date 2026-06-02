package pt.lourenco.optimization.jmetal.constraints.rules;

import pt.lourenco.optimization.jmetal.constraints.model.SolutionContext;

public interface ConstraintRule {
    String getId();
    double computeViolation(SolutionContext context);
}
