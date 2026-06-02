package pt.lourenco.optimization.jmetal.constraints.service;

import org.springframework.stereotype.Service;
import pt.lourenco.optimization.jmetal.constraints.dto.UserConstraintSelection;
import pt.lourenco.optimization.jmetal.constraints.model.ConstraintEvaluationResult;
import pt.lourenco.optimization.jmetal.constraints.model.SolutionContext;
import pt.lourenco.optimization.utils.JSONGetters;

import java.util.List;

@Service
public class ConstraintOrchestratorService {

    private final ConstraintSelectionMapper constraintSelectionMapper;
    private final SolutionContextBuilderService solutionContextBuilderService;
    private final ConstraintEvaluationService constraintEvaluationService;

    public ConstraintOrchestratorService(
            ConstraintSelectionMapper constraintSelectionMapper,
            SolutionContextBuilderService solutionContextBuilderService,
            ConstraintEvaluationService constraintEvaluationService
    ) {
        this.constraintSelectionMapper = constraintSelectionMapper;
        this.solutionContextBuilderService = solutionContextBuilderService;
        this.constraintEvaluationService = constraintEvaluationService;
    }

    public ConstraintEvaluationResult evaluateFromRequest(JSONGetters request) {
        SolutionContext context = solutionContextBuilderService.buildFromRequest(request);
        List<UserConstraintSelection> selectedConstraints =
                constraintSelectionMapper.mapFromRequest(request);

        return constraintEvaluationService.evaluate(context, selectedConstraints);
    }
}
