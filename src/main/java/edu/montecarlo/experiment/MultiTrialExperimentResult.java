package edu.montecarlo.experiment;

import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.LongSummaryStatistics;

/**
 * Stores results from multiple independent trials of a Monte Carlo experiment.
 * Provides statistical analysis including mean, standard deviation, and confidence intervals.
 */
public class MultiTrialExperimentResult {
    private final List<ExperimentResult> trialResults;
    private final double meanEstimate;
    private final double stdDevEstimate;
    private final double meanRuntime;
    private final double meanError;
    private final String estimatorType;

    public MultiTrialExperimentResult(List<ExperimentResult> trialResults) {
        if (trialResults == null || trialResults.isEmpty()) {
            throw new IllegalArgumentException("Trial results cannot be empty");
        }
        this.trialResults = trialResults;
        this.estimatorType = trialResults.get(0).getEstimatorType();

        // Calculate statistics
        DoubleSummaryStatistics estimateStats = trialResults.stream()
                .mapToDouble(ExperimentResult::getEstimate)
                .summaryStatistics();
        
        DoubleSummaryStatistics errorStats = trialResults.stream()
                .mapToDouble(ExperimentResult::getAbsoluteError)
                .summaryStatistics();
        
        LongSummaryStatistics runtimeStats = trialResults.stream()
                .mapToLong(ExperimentResult::getRuntimeMs)
                .summaryStatistics();

        this.meanEstimate = estimateStats.getAverage();
        this.meanError = errorStats.getAverage();
        this.meanRuntime = runtimeStats.getAverage();

        // Calculate Standard Deviation
        double variance = trialResults.stream()
                .mapToDouble(r -> Math.pow(r.getEstimate() - meanEstimate, 2))
                .sum() / trialResults.size();
        this.stdDevEstimate = Math.sqrt(variance);
    }

    public int getTrialCount() {
        return trialResults.size();
    }

    public double getMeanEstimate() {
        return meanEstimate;
    }

    public double getStdDevEstimate() {
        return stdDevEstimate;
    }

    public double getMeanRuntime() {
        return meanRuntime;
    }

    public double getMeanError() {
        return meanError;
    }

    public String getEstimatorType() {
        return estimatorType;
    }

    @Override
    public String toString() {
        return String.format("%s (%d trials) | Mean Est: %.6f | StdDev: %.6f | Mean Error: %.6f | Mean Time: %.1f ms",
                estimatorType, getTrialCount(), meanEstimate, stdDevEstimate, meanError, meanRuntime);
    }
}
