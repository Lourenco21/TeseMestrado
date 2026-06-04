package pt.lourenco.optimization.jmetal.constraints.service;

import org.springframework.stereotype.Component;
import pt.lourenco.optimization.jmetal.constraints.rules.ConstraintRule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ConstraintRuleRegistry {

    private final Map<String, ConstraintRule> rulesById = new HashMap<>();

    public ConstraintRuleRegistry(List<ConstraintRule> rules) {
        for (ConstraintRule rule : rules) {
            rulesById.put(rule.getConstraintId(), rule);
        }
    }

    public ConstraintRule getById(String constraintId) {
        return rulesById.get(constraintId);
    }
}