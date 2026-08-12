package pt.lourenco.optimization.jmetal.problems.service;

import org.springframework.stereotype.Service;
import pt.lourenco.optimization.django.DjangoProblemDataClient;
import pt.lourenco.optimization.django.dto.DjangoProblemDataResponse;
import pt.lourenco.optimization.jmetal.constraints.dto.UserConstraintSelection;
import pt.lourenco.optimization.jmetal.constraints.service.ConstraintSelectionMapper;
import pt.lourenco.optimization.jmetal.problems.model.ProblemInputData;
import pt.lourenco.optimization.utils.JSONGetters;

import java.util.List;
import java.util.Map;

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

        Map<String, Object> constraintsSummary = request.getConstraints_summary();
        Map<String, Object> instanceCharacteristics = request.getInstance_characteristics();

        String problemType = normalize(request.getProblem_type());
        String problemSubtype = normalize(request.getProblem_subtype());

        sb.append("This is a single-objective optimization problem");
        if (problemType != null) {
            sb.append(" of type ").append(problemType);
        }
        if (problemSubtype != null) {
            sb.append(", more specifically ").append(problemSubtype);
        }
        sb.append(".\n\n");

        sb.append("Soft constraints enter the objective function weighted by their importance, ")
                .append("while hard constraints are used only to ensure solution feasibility ")
                .append("(applied during solution creation and repair), and do not contribute directly ")
                .append("to the objective function value, however they go into the constraint vector.\n\n");

        sb.append("Before running the selected algorithm, an initial feasible solution is generated " +
                "using a heuristic construction method (createSolution), which the algorithm then " +
                "uses as its starting point or as part of its initial population.\n\n");

        int total = asInt(getValue(constraintsSummary, "total"));
        int hard = asInt(getValue(constraintsSummary, "hard"));
        int soft = asInt(getValue(constraintsSummary, "soft"));

        sb.append("The problem has a total of ").append(total)
                .append(" constraints: ").append(hard).append(" hard and ")
                .append(soft).append(" soft.\n\n");

        sb.append("The problem has 1 objective and ").append(hard).append(" constraints in the constraint vector.\n\n");

        sb.append("The problem is solved by partitions: each partition corresponds to a group of ")
                .append("classes starting at the same time, in 30 minute intervals. Each partition is solved ")
                .append("in an independent execution of the chosen algorithm, however, constraints related to ")
                .append("overlaps or to consecutive classes may depend on the solution of adjacent time ")
                .append("partitions.\n\n");

        Object totalRooms = request.getTotal_rooms();
        Object totalClasses = getValue(instanceCharacteristics, "total_classes");

        sb.append("Instance size:\n");
        sb.append("- Number of rooms: ").append(valueOrNull(totalRooms)).append("\n");
        sb.append("- Number of classes: ").append(valueOrNull(totalClasses)).append("\n\n");

        Object partitionStatsObj = getValue(instanceCharacteristics, "selected_partition_statistics");

        sb.append("Partition statistics:\n");
        if (partitionStatsObj instanceof Map<?, ?> partitionStats) {
            sb.append("- Total number of partitions: ")
                    .append(valueOrNull(partitionStats.get("partition_count"))).append("\n");
            sb.append("- Average classes per partition: ")
                    .append(valueOrNull(partitionStats.get("average_classes_per_partition"))).append("\n");
            sb.append("- Minimum classes per partition: ")
                    .append(valueOrNull(partitionStats.get("min_classes_per_partition"))).append("\n");
            sb.append("- Maximum classes per partition: ")
                    .append(valueOrNull(partitionStats.get("max_classes_per_partition"))).append("\n");
        }

        return sb.toString();
    }

    private Object getValue(Map<String, Object> map, String key) {
        return map == null ? null : map.get(key);
    }

    private int asInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        return value.replace("_", " ").toLowerCase();
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