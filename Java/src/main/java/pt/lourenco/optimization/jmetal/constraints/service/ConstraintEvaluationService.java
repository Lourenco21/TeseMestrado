package pt.lourenco.optimization.jmetal.constraints.service;

import org.springframework.stereotype.Service;
import pt.lourenco.optimization.jmetal.constraints.dto.UserConstraintSelection;
import pt.lourenco.optimization.jmetal.constraints.model.ConstraintEvaluationItem;
import pt.lourenco.optimization.jmetal.constraints.model.ConstraintEvaluationResult;
import pt.lourenco.optimization.jmetal.constraints.model.ConstraintGoal;
import pt.lourenco.optimization.jmetal.constraints.model.SolutionContext;
import pt.lourenco.optimization.jmetal.constraints.registry.ConstraintRegistry;
import pt.lourenco.optimization.jmetal.constraints.rules.ConstraintRule;

import java.util.ArrayList;
import java.util.List;

@Service
public class ConstraintEvaluationService {

    private final ConstraintRegistry constraintRegistry;

    public ConstraintEvaluationService(ConstraintRegistry constraintRegistry) {
        this.constraintRegistry = constraintRegistry;
    }

    public ConstraintEvaluationResult evaluate(
            SolutionContext context,
            List<UserConstraintSelection> selectedConstraints
    ) {
        double totalHardViolation = 0.0;
        double totalSoftViolation = 0.0;
        int hardViolatedCount = 0;
        int softViolatedCount = 0;
        List<ConstraintEvaluationItem> items = new ArrayList<>();

        if (selectedConstraints == null || selectedConstraints.isEmpty()) {
            return new ConstraintEvaluationResult(
                    0.0,
                    0.0,
                    0,
                    0,
                    items
            );
        }

        for (UserConstraintSelection selection : selectedConstraints) {
            ConstraintRule rule = constraintRegistry.getById(selection.getId());

            double rawViolation = rule.computeViolation(context);
            boolean violated = rawViolation > 0.0;
            double weightedViolation = violated
                    ? rawViolation * selection.getImportance().getWeight()
                    : 0.0;

            if (violated) {
                if (selection.getGoal() == ConstraintGoal.HARD) {
                    totalHardViolation += weightedViolation;
                    hardViolatedCount++;
                } else {
                    totalSoftViolation += weightedViolation;
                    softViolatedCount++;
                }
            }

            items.add(new ConstraintEvaluationItem(
                    selection.getId(),
                    selection.getGoal(),
                    selection.getImportance(),
                    rawViolation,
                    weightedViolation,
                    violated
            ));
        }

        return new ConstraintEvaluationResult(
                totalHardViolation,
                totalSoftViolation,
                hardViolatedCount,
                softViolatedCount,
                items
        );
    }
}
