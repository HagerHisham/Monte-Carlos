package edu.montecarlo.experiment;

import edu.montecarlo.model.SimulationConfig;

/**
 * Stores results from a single π estimation experiment.
 */
public class ExperimentResult {
    private final SimulationConfig config;
    private final double estimate;
    private final long runtimeMs;
    private final double absoluteError;
    private final String estimatorType;

    public ExperimentResult(SimulationConfig config, double estimate,
            long runtimeMs, String estimatorType, double expectedValue) {
        this.config = config;
        this.estimate = estimate;
        this.runtimeMs = runtimeMs;
        this.estimatorType = estimatorType;
        this.absoluteError = Math.abs(estimate - expectedValue);
    }

    public ExperimentResult(SimulationConfig config, double estimate,
            long runtimeMs, String estimatorType) {
        this(config, estimate, runtimeMs, estimatorType, Math.PI);
    }

    public SimulationConfig getConfig() {
        return config;
    }

    public double getEstimate() {
        return estimate;
    }

    public long getRuntimeMs() {
        return runtimeMs;
    }

    public double getAbsoluteError() {
        return absoluteError;
    }

    public String getEstimatorType() {
        return estimatorType;
    }

    @Override
    public String toString() {
        return String.format("%s | %s | Est ≈ %.6f | Error: %.6f | Time: %,d ms",
                estimatorType, config, estimate, absoluteError, runtimeMs);
    }
}
