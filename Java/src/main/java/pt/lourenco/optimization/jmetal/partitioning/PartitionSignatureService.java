package pt.lourenco.optimization.jmetal.partitioning;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
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
        Map<String, Object> mapping = inputData.getMappingData();

        String dayCol = schedulePartitionService.getMappedColumn(mapping, "dia");
        String startCol = schedulePartitionService.getMappedColumn(mapping, "hora_inicio");
        String endCol = schedulePartitionService.getMappedColumn(mapping, "hora_fim");
        String ucCol = schedulePartitionService.getMappedColumn(mapping, "unidade_curricular");
        String roomFeaturesCol = schedulePartitionService.getMappedColumn(mapping, "caracteristicas_pedidas_para_sala");
        String studentsCol = schedulePartitionService.getMappedColumn(mapping, "numero_estudantes");
        String turmaCol = schedulePartitionService.getMappedColumn(mapping, "turma");
        String typeCol = schedulePartitionService.getMappedColumn(mapping, "tipo_aula");

        List<Map<String, Object>> normalized = new ArrayList<>();

        for (Map<String, Object> classData : partition.getClassesInPartition()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("dia", normalize(schedulePartitionService.getValue(classData, dayCol)));
            item.put("hora_inicio", normalize(schedulePartitionService.getValue(classData, startCol)));
            item.put("hora_fim", normalize(schedulePartitionService.getValue(classData, endCol)));
            item.put("unidade_curricular", normalize(schedulePartitionService.getValue(classData, ucCol)));
            item.put("caracteristicas_pedidas_para_sala", normalize(schedulePartitionService.getValue(classData, roomFeaturesCol)));
            item.put("numero_estudantes", normalize(schedulePartitionService.getValue(classData, studentsCol)));
            item.put("turma", normalize(schedulePartitionService.getValue(classData, turmaCol)));
            item.put("tipo_aula", normalize(schedulePartitionService.getValue(classData, typeCol)));
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