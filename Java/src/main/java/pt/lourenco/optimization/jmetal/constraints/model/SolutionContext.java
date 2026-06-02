package pt.lourenco.optimization.jmetal.constraints.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SolutionContext {

    private Integer problemId;
    private String problemName;
    private String problemType;
    private String problemSubtype;

    private String selectedAlgorithm;
    private String resolutionScope;
    private String repeatedInstanceStrategy;

    private Map<String, Object> scheduleData;
    private Map<String, Object> roomsData;
    private Map<String, Object> mappingData;
    private Map<String, Object> roomsMappingData;
    private Map<String, Object> constraintsSummary;
    private Map<String, Object> instanceCharacteristics;

    private List<Object> objectives;
    private List<Object> constraints;
    private List<Object> selectedConstraints;
}