package pt.lourenco.optimization.jmetal.partitioning;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import pt.lourenco.optimization.jmetal.problems.model.ProblemInputData;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class PartitionedProblemInputData {
    private String partitionKey;
    private PartitionType partitionType;
    private int partitionOrder;
    private ProblemInputData inputData;
    private List<Map<String, Object>> classesInPartition;
}