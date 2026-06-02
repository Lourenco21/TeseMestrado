package pt.lourenco.optimization.jmetal.constraints.registry;

import org.springframework.stereotype.Component;
import pt.lourenco.optimization.jmetal.constraints.rules.ConstraintRule;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ConstraintRegistry {

    private final Map<String, ConstraintRule> rulesById;

    public ConstraintRegistry(List<ConstraintRule> rules) {
        this.rulesById = rules.stream()
                .collect(Collectors.toMap(
                        ConstraintRule::getId,
                        Function.identity()
                ));
    }

    public ConstraintRule getById(String id) {
        ConstraintRule rule = rulesById.get(id);

        if (rule == null) {
            throw new IllegalArgumentException("Unsupported constraint: " + id);
        }

        return rule;
    }
}
