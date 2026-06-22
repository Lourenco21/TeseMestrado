package pt.lourenco.optimization.jmetal.constraints.service;

import org.springframework.stereotype.Service;
import pt.lourenco.optimization.jmetal.constraints.model.SolutionContext;
import pt.lourenco.optimization.jmetal.partitioning.PreviousPartitionAssignmentsContext;
import pt.lourenco.optimization.jmetal.problems.model.ProblemInputData;

@Service
public class SolutionContextBuilderService {

    private final PreparedEvaluationDataBuilderService preparedEvaluationDataBuilderService;

    public SolutionContextBuilderService(
            PreparedEvaluationDataBuilderService preparedEvaluationDataBuilderService
    ) {
        this.preparedEvaluationDataBuilderService = preparedEvaluationDataBuilderService;
    }

    public SolutionContext buildFromProblemInput(ProblemInputData inputData) {
        SolutionContext context = new SolutionContext();

        context.setProblemId(inputData.getProblemId());
        context.setProblemName(inputData.getProblemName());
        context.setProblemType(inputData.getProblemType());
        context.setProblemSubtype(inputData.getProblemSubtype());

        context.setSelectedAlgorithm(inputData.getSelectedAlgorithm());
        context.setResolutionScope(inputData.getResolutionScope());
        context.setRepeatedInstanceStrategy(inputData.getRepeatedInstanceStrategy());

        context.setScheduleData(inputData.getScheduleData());
        context.setRoomsData(inputData.getRoomsData());
        context.setMappingData(inputData.getMappingData());
        context.setRoomsMappingData(inputData.getRoomsMappingData());
        context.setConstraintsSummary(inputData.getConstraintsSummary());
        context.setInstanceCharacteristics(inputData.getInstanceCharacteristics());

        context.setSelectedConstraints(
                inputData.getSelectedConstraints() == null
                        ? java.util.List.of()
                        : inputData.getSelectedConstraints().stream().map(item -> (Object) item).toList()
        );

        Object previousAssignmentsContext = inputData.getMetadata() == null
                ? null
                : inputData.getMetadata().get("previousPartitionAssignmentsContext");

        if (previousAssignmentsContext instanceof PreviousPartitionAssignmentsContext previousContext) {
            context.setPreviousPartitionAssignmentsContext(previousContext);
        }

        context.setPreparedEvaluationData(preparedEvaluationDataBuilderService.build(inputData));

        return context;
    }
}