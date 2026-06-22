package pt.lourenco.optimization.jmetal.partitioning;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import pt.lourenco.optimization.jmetal.problems.mapping.ScheduleMappingUtils;
import pt.lourenco.optimization.jmetal.problems.model.ProblemInputData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PartitionSignatureService {

    private final SchedulePartitionService schedulePartitionService;
    private final ObjectMapper objectMapper;

    public PartitionSignatureService(
            SchedulePartitionService schedulePartitionService,
            ObjectMapper objectMapper
    ) {
        this.schedulePartitionService = schedulePartitionService;
        this.objectMapper = objectMapper;
    }

    public String buildSignature(PartitionedProblemInputData partition) {
        ProblemInputData inputData = partition.getInputData();


        List<Map<String, Object>> normalized = new ArrayList<>();

        for (Map<String, Object> classData : partition.getClassesInPartition()) {
            Map<String, Object> item = new LinkedHashMap<>();
            Map<String, Object> mapping = inputData.getMappingData();
            item.put("dia", normalize(ScheduleMappingUtils.getDay(classData, mapping)));
            item.put("hora_inicio", normalize(ScheduleMappingUtils.getStartTime(classData, mapping)));
            item.put("hora_fim", normalize(ScheduleMappingUtils.getEndTime(classData, mapping)));
            item.put("unidade_curricular", normalize(ScheduleMappingUtils.getCourse(classData, mapping)));
            item.put("caracteristicas_pedidas_para_sala", normalize(ScheduleMappingUtils.getRequestedRoomCharacteristics(classData, mapping)));
            item.put("numero_estudantes", normalize(ScheduleMappingUtils.getStudents(classData, mapping)));
            item.put("turma", normalize(ScheduleMappingUtils.getClassGroup(classData, mapping)));
            item.put("tipo_aula", normalize(ScheduleMappingUtils.getClassType(classData, mapping)));
            normalized.add(item);
        }

        normalized.sort(Comparator.comparing(Object::toString));

        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build partition signature.", e);
        }
    }

    private String normalize(Object value) {
        return value == null ? "" : value.toString().trim().toLowerCase();
    }
}