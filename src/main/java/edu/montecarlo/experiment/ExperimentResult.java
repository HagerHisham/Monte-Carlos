package edu.montecarlo.experiment;

import java.util.ArrayList;
import java.util.List;

import edu.montecarlo.model.SimulationConfig;


public class ExperimentResult {

    private final SimulationConfig config;
    private final double piEstimate;
    private final long runtimeMs;
    private final double absoluteError;
    private final String estimatorType;


    private double averageError = 0.0;
    private List<ExperimentResult> trialResults = new ArrayList<>();


    public ExperimentResult(SimulationConfig config, double piEstimate,
                            long runtimeMs, String estimatorType) {
        this.config = config;
        this.piEstimate = piEstimate;
        this.runtimeMs = runtimeMs;
        this.estimatorType = estimatorType;
        this.absoluteError = Math.abs(piEstimate - Math.PI);
    }


    public SimulationConfig getConfig() {
        return config;
    }

    public double getPiEstimate() {
        return piEstimate;
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

    public double getAverageError() {
        return averageError;
    }

    public List<ExperimentResult> getTrialResults() {
        return trialResults;
    }


    public void setAverageError(double averageError) {
        this.averageError = averageError;
    }

    public void setTrialResults(List<ExperimentResult> trialResults) {
        this.trialResults = trialResults;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s | %s | π ≈ %.6f | Error: %.6f | Time: %,d ms",
                estimatorType, config, piEstimate, absoluteError, runtimeMs));

        if (!trialResults.isEmpty()) {
            sb.append("\n  Trials:\n");
            int i = 1;
            for (ExperimentResult trial : trialResults) {
                sb.append(String.format("    Trial %d → π ≈ %.6f | Error: %.6f | Time: %,d ms\n",
                        i++, trial.getPiEstimate(), trial.getAbsoluteError(), trial.getRuntimeMs()));
            }
            sb.append(String.format("    AVG → π ≈ %.6f | Avg Error: %.6f | Avg Time: %,d ms",
                    piEstimate, averageError, runtimeMs));
        }

        return sb.toString();
    }
}