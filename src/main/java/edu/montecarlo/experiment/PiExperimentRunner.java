package edu.montecarlo.experiment;

import java.util.ArrayList;
import java.util.List;

import edu.montecarlo.model.MonteCarloEstimator;
import edu.montecarlo.model.ParallelIntegrationEstimator;
import edu.montecarlo.model.ParallelPiEstimator;
import edu.montecarlo.model.PiEstimator;
import edu.montecarlo.model.SequentialPiEstimator;
import edu.montecarlo.model.SimulationConfig;

/**
 * Runs experiments to compare different Monte Carlo estimators.
 * Measures runtime, accuracy, and speedup for different configurations.
 */
public class PiExperimentRunner {

    /**
     * Runs a single experiment with the given configuration and estimator.
     * 
     * @param estimator     The estimation strategy to use
     * @param config        Simulation parameters
     * @param type          Descriptive name for this estimator
     * @param expectedValue The expected/actual value for error calculation
     * @return Experiment result with timing and accuracy data
     */
    public ExperimentResult runExperiment(MonteCarloEstimator estimator,
            SimulationConfig config,
            String type,
            double expectedValue) {
        // Warm up JVM (optional but recommended for accurate timing)
        if (config.getTotalPoints() > 100000) {
            estimator.estimate(new SimulationConfig(10000, config.getNumTasks(), config.getNumThreads()));
        }

        // Measure execution time
        long startTime = System.currentTimeMillis();
        double estimate = estimator.estimate(config);
        long endTime = System.currentTimeMillis();

        long runtime = endTime - startTime;

        return new ExperimentResult(config, estimate, runtime, type, expectedValue);
    }

    /**
     * Runs a single experiment for Pi estimation (defaults expected value to PI).
     */
    public ExperimentResult runExperiment(PiEstimator estimator,
            SimulationConfig config,
            String type) {
        return runExperiment(estimator, config, type, Math.PI);
    }

    /**
     * Runs multiple independent trials of the same experiment.
     * Useful for gathering statistical data (mean, std dev, confidence intervals).
     * 
     * @param estimator     The estimator to test
     * @param config        Configuration for each trial
     * @param type          Description of the estimator
     * @param numTrials     Number of independent trials to run
     * @param expectedValue Actual value for error calculation
     * @return Aggregated result of all trials
     */
    public MultiTrialExperimentResult runMultiTrialExperiment(MonteCarloEstimator estimator,
            SimulationConfig config, String type, int numTrials, double expectedValue) {
        List<ExperimentResult> results = new ArrayList<>();
        
        System.out.println("Running " + numTrials + " trials for " + type + "...");
        
        for (int i = 0; i < numTrials; i++) {
            results.add(runExperiment(estimator, config, type, expectedValue));
            // Optional: Print dot for progress
            if (numTrials > 20 && i % (numTrials / 20) == 0) {
                System.out.print(".");
            }
        }
        if (numTrials > 20) System.out.println();
        
        return new MultiTrialExperimentResult(results);
    }

    /**
     * Runs a comprehensive set of experiments comparing sequential and parallel
     * estimators.
     * 
     * @param pointsList    Different sample sizes to test
     * @param threadCounts  Different thread pool sizes to test
     * @param sequential    Sequential estimator instance
     * @param parallel      Parallel estimator instance
     * @param expectedValue Expected value for error calculation
     * @return List of all experiment results
     */
    public List<ExperimentResult> runComprehensiveExperiments(long[] pointsList, int[] threadCounts,
            MonteCarloEstimator sequential, MonteCarloEstimator parallel, double expectedValue) {
        List<ExperimentResult> results = new ArrayList<>();

        System.out.println(
                "=== Monte Carlo Experiments (" + sequential.getName() + " vs " + parallel.getName() + ") ===\n");

        // Test each sample size
        for (long points : pointsList) {
            System.out.println("Testing with " + String.format("%,d", points) + " points:");

            // Run sequential version
            SimulationConfig seqConfig = new SimulationConfig(points, 1, 1);
            ExperimentResult seqResult = runExperiment(sequential, seqConfig, "Sequential", expectedValue);
            results.add(seqResult);
            System.out.println("  " + seqResult);

            // Run parallel versions with different thread counts
            for (int threads : threadCounts) {
                int tasks = threads * 2; // Use 2x tasks as threads for better load balancing
                SimulationConfig parConfig = new SimulationConfig(points, tasks, threads);
                ExperimentResult parResult = runExperiment(parallel, parConfig,
                        "Parallel(" + threads + " threads)", expectedValue);
                results.add(parResult);

                // Calculate speedup
                double speedup = (double) seqResult.getRuntimeMs() / parResult.getRuntimeMs();
                System.out.println("  " + parResult +
                        String.format(" | Speedup: %.2fx", speedup));
            }

            System.out.println();
        }

        return results;
    }

    /**
     * Runs default comprehensive experiments for Pi estimation.
     */
    public List<ExperimentResult> runComprehensiveExperiments(long[] pointsList, int[] threadCounts) {
        return runComprehensiveExperiments(pointsList, threadCounts,
                new SequentialPiEstimator(), new ParallelPiEstimator(), Math.PI);
    }

    /**
     * Prints a summary table of all experiment results.
     */
    public void printResultsSummary(List<ExperimentResult> results) {
        System.out.println("\n=== Experiment Summary ===");
        System.out.println(String.format("%-25s | %-15s | %-12s | %-12s | %-10s",
                "Estimator", "Points", "Estimate", "Error", "Time (ms)"));
        System.out.println("-".repeat(90));

        for (ExperimentResult result : results) {
            System.out.println(String.format("%-25s | %,15d | %.10f | %.10f | %,10d",
                    result.getEstimatorType(),
                    result.getConfig().getTotalPoints(),
                    result.getEstimate(),
                    result.getAbsoluteError(),
                    result.getRuntimeMs()));
        }
    }

    /**
     * Prints a summary table of multi-trial experiment results.
     */
    public void printMultiTrialResultsSummary(List<MultiTrialExperimentResult> results) {
        System.out.println("\n=== Multi-Trial Experiment Summary ===");
        System.out.println(String.format("%-25s | %-10s | %-12s | %-12s | %-12s",
                "Estimator", "Trials", "Mean Est", "Std Dev", "Mean Error"));
        System.out.println("-".repeat(90));

        for (MultiTrialExperimentResult result : results) {
            System.out.println(String.format("%-25s | %-10d | %.8f | %.8f | %.8f",
                    result.getEstimatorType(),
                    result.getTrialCount(),
                    result.getMeanEstimate(),
                    result.getStdDevEstimate(),
                    result.getMeanError()));
        }
    }

    /**
     * Main method to run default experiments.
     */
    public static void main(String[] args) {
        PiExperimentRunner runner = new PiExperimentRunner();

        // Define test configurations
        long[] pointsList = { 100_000, 1_000_000, 10_000_000 };
        int[] threadCounts = { 2, 4, 8 };

        // Run experiments
        List<ExperimentResult> results = runner.runComprehensiveExperiments(pointsList, threadCounts);

        // Print summary
        runner.printResultsSummary(results);

        // Run Multi-Trial Example
        System.out.println("\n=== Running Multi-Trial Analysis ===");
        PiEstimator parallelEstimator = new ParallelPiEstimator();
        SimulationConfig config = new SimulationConfig(1_000_000, 8, 4);
        MultiTrialExperimentResult multiResult = runner.runMultiTrialExperiment(
                parallelEstimator, config, "Parallel(4 threads)", 10, Math.PI);

        runner.printMultiTrialResultsSummary(List.of(multiResult));

        System.out.println("\n=== Running Integration Experiment (x^2) ===");
        MonteCarloEstimator integrationEstimator = new ParallelIntegrationEstimator();
        SimulationConfig intConfig = new SimulationConfig(1_000_000, 8, 4);
        double expectedInt = 1.0 / 3.0;
        
        // Single run
        ExperimentResult intResult = runner.runExperiment(integrationEstimator, intConfig, "Integration(Par)", expectedInt);
        System.out.println("Single Run: " + intResult);
        
        // Multi trial
        MultiTrialExperimentResult multiIntResult = runner.runMultiTrialExperiment(
                integrationEstimator, intConfig, "Integration(Par)", 5, expectedInt);
        runner.printMultiTrialResultsSummary(List.of(multiIntResult));
        
        System.out.println("\nActual π value: " + Math.PI);
        System.out.println("Actual Integral value: " + expectedInt);
    }
}
