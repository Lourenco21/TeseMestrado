package pt.lourenco.optimization.jmetal.constraints.rules;

import org.springframework.stereotype.Component;
import pt.lourenco.optimization.jmetal.constraints.model.SolutionContext;

@Component
public class CapacityWasteRule implements ConstraintRule {

    @Override
    public String getId() {
        return "capacity_waste";
    }

    @Override
    public double computeViolation(SolutionContext context) {
        return 0.0;
    }
}
