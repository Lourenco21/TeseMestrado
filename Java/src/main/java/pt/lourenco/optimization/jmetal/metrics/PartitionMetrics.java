package pt.lourenco.optimization.jmetal.metrics;

import lombok.Getter;

@Getter
public class PartitionMetrics {
    private int evaluationCount;
    private long totalEvaluateTimeNs;
    private long totalBuildAssignmentsTimeNs;
    private long totalConstraintEvaluationTimeNs;

    public void incrementEvaluationCount() {
        evaluationCount++;
    }

    public void addEvaluateTime(long ns) {
        totalEvaluateTimeNs += ns;
    }

    public void addBuildAssignmentsTime(long ns) {
        totalBuildAssignmentsTimeNs += ns;
    }

    public void addConstraintEvaluationTime(long ns) {
        totalConstraintEvaluationTimeNs += ns;
    }

    public long getTotalEvaluateTimeMs() {
        return totalEvaluateTimeNs / 1_000_000L;
    }

    public long getTotalBuildAssignmentsTimeMs() {
        return totalBuildAssignmentsTimeNs / 1_000_000L;
    }

    public long getTotalConstraintEvaluationTimeMs() {
        return totalConstraintEvaluationTimeNs / 1_000_000L;
    }

    public double getAverageEvaluateTimeMs() {
        return evaluationCount == 0 ? 0.0 : (totalEvaluateTimeNs / 1_000_000.0) / evaluationCount;
    }
}