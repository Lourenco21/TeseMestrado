package pt.lourenco.optimization.jmetal.problems.service;

import org.springframework.stereotype.Service;
import pt.lourenco.optimization.django.DjangoProblemDataClient;
import pt.lourenco.optimization.django.dto.DjangoProblemDataResponse;
import pt.lourenco.optimization.jmetal.constraints.dto.UserConstraintSelection;
import pt.lourenco.optimization.jmetal.constraints.service.ConstraintSelectionMapper;
import pt.lourenco.optimization.jmetal.problems.model.ProblemInputData;
import pt.lourenco.optimization.utils.JSONGetters;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProblemDataBuilderService {

    private final DjangoProblemDataClient djangoProblemDataClient;
    private final ConstraintSelectionMapper constraintSelectionMapper;

    public ProblemDataBuilderService(DjangoProblemDataClient djangoProblemDataClient, ConstraintSelectionMapper constraintSelectionMapper) {
        this.djangoProblemDataClient = djangoProblemDataClient;
        this.constraintSelectionMapper = constraintSelectionMapper;
    }

    public String buildProblemData(JSONGetters request) {
        StringBuilder sb = new StringBuilder();

        sb.append("name: ").append(valueOrNull(request.getName())).append("\n");
        sb.append("problem_type: ").append(valueOrNull(request.getProblem_type())).append("\n");
        sb.append("problem_subtype: ").append(valueOrNull(request.getProblem_subtype())).append("\n");
        sb.append("resolution_scope: ").append(valueOrNull(request.getResolution_scope())).append("\n");
        sb.append("repeated_instance_strategy: ").append(valueOrNull(request.getRepeated_instance_strategy())).append("\n");
        sb.append("\n");

        sb.append("constraints_summary:\n");
        sb.append(valueOrNull(request.getConstraints_summary())).append("\n\n");

        sb.append("instance_characteristics:\n");
        sb.append(valueOrNull(request.getInstance_characteristics())).append("\n");

        return sb.toString();
    }

    public String buildExecutionProblemData(JSONGetters request) {
        StringBuilder sb = new StringBuilder();

        sb.append("name: ").append(valueOrNull(request.getName())).append("\n");
        sb.append("problem_type: ").append(valueOrNull(request.getProblem_type())).append("\n");
        sb.append("problem_subtype: ").append(valueOrNull(request.getProblem_subtype())).append("\n");
        sb.append("resolution_scope: ").append(valueOrNull(request.getResolution_scope())).append("\n");
        sb.append("repeated_instance_strategy: ").append(valueOrNull(request.getRepeated_instance_strategy())).append("\n");
        sb.append("selected_algorithm: ").append(valueOrNull(request.getSelected_algorithm())).append("\n");
        sb.append("\n");

        sb.append("constraints_summary:\n");
        sb.append(valueOrNull(request.getConstraints_summary())).append("\n\n");

        sb.append("selected_constraints:\n");
        sb.append(valueOrNull(request.getSelected_constraints())).append("\n\n");

        sb.append("instance_characteristics:\n");
        sb.append(valueOrNull(request.getInstance_characteristics())).append("\n\n");

        return sb.toString();
    }

    public ProblemInputData buildProblemInputData(JSONGetters request) {
        log.info("Building ProblemInputData");
        log.debug("request.schedule_data present: {}", request.getSchedule_data() != null);
        log.debug("request.rooms_data present: {}", request.getRooms_data() != null);
        log.debug("request.mapping_data present: {}", request.getMapping_data() != null);
        log.debug("request.rooms_mapping_data present: {}", request.getRooms_mapping_data() != null);

        if (request.getSchedule_data() != null) {
            log.debug("request.schedule_data keys: {}", request.getSchedule_data().keySet());
            Object classesObject = request.getSchedule_data().get("classes");
            log.debug("request.schedule_data.classes type: {}", classesObject == null ? null : classesObject.getClass().getName());
            log.debug("request.schedule_data.classes size: {}", classesObject instanceof java.util.List<?> list ? list.size() : null);
        }

        if (request.getRooms_data() != null) {
            log.debug("request.rooms_data keys: {}", request.getRooms_data().keySet());
            Object roomsObject = request.getRooms_data().get("rooms");
            log.debug("request.rooms_data.rooms type: {}", roomsObject == null ? null : roomsObject.getClass().getName());
            log.debug("request.rooms_data.rooms size: {}", roomsObject instanceof java.util.List<?> list ? list.size() : null);
        }

        if (request.getMapping_data() != null) {
            log.debug("request.mapping_data keys: {}", request.getMapping_data().keySet());
            Object nestedMapping = request.getMapping_data().get("mapping");
            log.debug("request.mapping_data.mapping type: {}", nestedMapping == null ? null : nestedMapping.getClass().getName());
            log.debug("request.mapping_data.mapping content: {}", nestedMapping);
        }

        List<UserConstraintSelection> selectedConstraints =
                constraintSelectionMapper.mapFromRequest(request);

        DjangoProblemDataResponse djangoData =
                djangoProblemDataClient.fetchProblemData(request.getProblem_id());

        ProblemInputData inputData = new ProblemInputData();
        inputData.setProblemId(request.getProblem_id());
        inputData.setProblemName(request.getName());
        inputData.setProblemType(request.getProblem_type());
        inputData.setProblemSubtype(request.getProblem_subtype());
        inputData.setSelectedAlgorithm(request.getSelected_algorithm());
        inputData.setResolutionScope(request.getResolution_scope());
        inputData.setRepeatedInstanceStrategy(request.getRepeated_instance_strategy());
        inputData.setSelectedConstraints(selectedConstraints);
        inputData.setScheduleData(djangoData.getSchedule_data());
        inputData.setRoomsData(djangoData.getRooms_data());
        inputData.setMetadata(request.getMetadata());
        inputData.setMappingData(request.getMapping_data());
        inputData.setRoomsMappingData(request.getRooms_mapping_data());
        inputData.setRoomFeatureResolution(request.getRoom_feature_resolution());
        inputData.setResolvedRequestedRoomFeatures(request.getResolved_requested_room_features());
        inputData.setConstraintsSummary(request.getConstraints_summary());
        inputData.setInstanceCharacteristics(request.getInstance_characteristics());

        log.debug("inputData.scheduleData present: {}", djangoData.getScheduleData() != null);
        log.debug("inputData.roomsData present: {}", djangoData.getRoomsData() != null);
        log.debug("inputData.mappingData present: {}", inputData.getMappingData() != null);

        return inputData;
    }

    private String valueOrNull(Object value) {
        return value == null ? "null" : value.toString();
    }
}