package pt.lourenco.optimization.jmetal.problems.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pt.lourenco.optimization.jmetal.constraints.dto.UserConstraintSelection;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProblemInputData {

    private Integer problemId;
    private String problemName;
    private String problemType;
    private String problemSubtype;

    private String selectedAlgorithm;
    private String resolutionScope;
    private String repeatedInstanceStrategy;

    private List<UserConstraintSelection> selectedConstraints;

    private Map<String, Object> scheduleData;
    private Map<String, Object> roomsData;
    private Map<String, Object> metadata;

    private Map<String, Object> mappingData;
    private Map<String, Object> roomsMappingData;
    private Map<String, Object> roomFeatureResolution;
    private List<Object> resolvedRequestedRoomFeatures;
    private Map<String, Object> constraintsSummary;
    private Map<String, Object> instanceCharacteristics;
}
