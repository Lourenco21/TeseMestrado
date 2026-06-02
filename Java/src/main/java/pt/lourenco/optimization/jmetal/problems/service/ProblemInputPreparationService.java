package pt.lourenco.optimization.jmetal.problems.service;


import org.springframework.stereotype.Service;
import pt.lourenco.optimization.jmetal.constraints.dto.UserConstraintSelection;
import pt.lourenco.optimization.jmetal.constraints.service.ConstraintSelectionMapper;
import pt.lourenco.optimization.django.DjangoProblemDataClient;
import pt.lourenco.optimization.django.dto.DjangoProblemDataResponse;
import pt.lourenco.optimization.jmetal.problems.model.ProblemInputData;
import pt.lourenco.optimization.utils.JSONGetters;

import java.util.List;

@Service
public class ProblemInputPreparationService {

    private final DjangoProblemDataClient djangoProblemDataClient;
    private final ConstraintSelectionMapper constraintSelectionMapper;

    public ProblemInputPreparationService(
            DjangoProblemDataClient djangoProblemDataClient,
            ConstraintSelectionMapper constraintSelectionMapper
    ) {
        this.djangoProblemDataClient = djangoProblemDataClient;
        this.constraintSelectionMapper = constraintSelectionMapper;
    }

    public ProblemInputData prepare(JSONGetters request) {
        validateRequest(request);

        List<UserConstraintSelection> selectedConstraints =
                constraintSelectionMapper.mapFromRequest(request);

        DjangoProblemDataResponse djangoData =
                djangoProblemDataClient.fetchProblemData(request.getProblem_draft_id());

        ProblemInputData inputData = new ProblemInputData();
        inputData.setProblemId(request.getProblem_id());
        inputData.setProblemName(request.getName());
        inputData.setProblemType(request.getProblem_type());
        inputData.setProblemSubtype(request.getProblem_subtype());

        inputData.setSelectedAlgorithm(request.getSelected_algorithm());
        inputData.setResolutionScope(request.getResolution_scope());
        inputData.setRepeatedInstanceStrategy(request.getRepeated_instance_strategy());

        inputData.setSelectedConstraints(selectedConstraints);

        inputData.setScheduleData(djangoData.getScheduleData());
        inputData.setRoomsData(djangoData.getRoomsData());

        inputData.setMappingData(request.getMapping_data());
        inputData.setRoomsMappingData(request.getRooms_mapping_data());
        inputData.setConstraintsSummary(request.getConstraints_summary());
        inputData.setInstanceCharacteristics(request.getInstance_characteristics());

        return inputData;
    }

    private void validateRequest(JSONGetters request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null.");
        }

        if (request.getProblem_id() == null) {
            throw new IllegalArgumentException("Problem id is required.");
        }

        if (request.getProblem_draft_id() == null) {
            throw new IllegalArgumentException("Problem draft id is required.");
        }
    }
}