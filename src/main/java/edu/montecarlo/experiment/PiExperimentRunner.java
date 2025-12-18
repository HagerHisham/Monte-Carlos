package edu.montecarlo.experiment;

import java.util.ArrayList;
import java.util.List;

import edu.montecarlo.model.ParallelPiEstimator;
import edu.montecarlo.model.PiEstimator;
import edu.montecarlo.model.SequentialPiEstimator;
import edu.montecarlo.model.SimulationConfig;

public class PiExperimentRunner {

    public ExperimentResult runExperiment(
            PiEstimator estimator,
            SimulationConfig config,
            String type
    ) {
        long startTime = System.currentTimeMillis();
        double piEstimate = estimator.estimatePi(config);
        long endTime = System.currentTimeMillis();

        long runtime = endTime - startTime;
        return new ExperimentResult(config, piEstimate, runtime, type);
    }

    public ExperimentResult runTrials(
            PiEstimator estimator,
            SimulationConfig config,
            String type,
            int numTrials
    ) {
        List<ExperimentResult> trials = new ArrayList<>();

        System.out.println("---- Trials (" + numTrials + ") for "
                + type + " | N=" + String.format("%,d", config.getTotalPoints())
                + " | Threads=" + config.getNumThreads() + " ----");

        for (int i = 1; i <= numTrials; i++) {
            ExperimentResult result =
                    runExperiment(estimator, config, type + " [Trial " + i + "]");
            trials.add(result);

            System.out.println(String.format(
                    "Trial %d | π = %.6f | Error = %.6f | Time = %d ms",
                    i,
                    result.getPiEstimate(),
                    result.getAbsoluteError(),
                    result.getRuntimeMs()
            ));
        }

        double avgPi = trials.stream()
                .mapToDouble(ExperimentResult::getPiEstimate)
                .average()
                .orElse(0.0);

        double avgError = trials.stream()
                .mapToDouble(ExperimentResult::getAbsoluteError)
                .average()
                .orElse(0.0);

        long avgTime = (long) trials.stream()
                .mapToLong(ExperimentResult::getRuntimeMs)
                .average()
                .orElse(0);

        System.out.println(String.format(
                "AVG | π = %.6f | Avg Error = %.6f | Avg Time = %d ms\n",
                avgPi, avgError, avgTime
        ));

        ExperimentResult avgResult = new ExperimentResult(
                config,
                avgPi,
                avgTime,
                type + " (avg)"
        );
        avgResult.setAverageError(avgError);
        avgResult.setTrialResults(trials);

        return avgResult;
    }

    public List<ExperimentResult> runComprehensiveExperiments(
            long[] pointsList,
            int[] threadCounts
    ) {
        List<ExperimentResult> results = new ArrayList<>();

        PiEstimator sequentialEstimator = new SequentialPiEstimator();
        PiEstimator parallelEstimator = new ParallelPiEstimator();

        System.out.println("=== Monte Carlo π Estimation Experiments ===\n");

        for (long points : pointsList) {
            System.out.println("Testing with " + String.format("%,d", points) + " points:");

            // Sequential
            SimulationConfig seqConfig = new SimulationConfig(points, 1, 1);
            ExperimentResult seqResult =
                    runExperiment(sequentialEstimator, seqConfig, "Sequential");
            results.add(seqResult);
            System.out.println("  " + seqResult);

            // Parallel
            for (int threads : threadCounts) {
                int tasks = threads * 2;
                SimulationConfig parConfig =
                        new SimulationConfig(points, tasks, threads);

                ExperimentResult parResult =
                        runExperiment(
                                parallelEstimator,
                                parConfig,
                                "Parallel(" + threads + " threads)"
                        );

                results.add(parResult);

                double speedup =
                        (double) seqResult.getRuntimeMs() / parResult.getRuntimeMs();

                System.out.println("  " + parResult +
                        String.format(" | Speedup: %.2fx", speedup));
            }

            System.out.println();
        }

        return results;
    }

    public void printResultsSummary(List<ExperimentResult> results) {
        System.out.println("\n=== Experiment Summary ===");
        System.out.println(String.format(
                "%-25s | %-15s | %-12s | %-12s | %-10s",
                "Estimator", "Points", "π Estimate", "Error", "Time (ms)"
        ));
        System.out.println("-".repeat(90));

        for (ExperimentResult result : results) {
            System.out.println(String.format(
                    "%-25s | %,15d | %.10f | %.10f | %,10d",
                    result.getEstimatorType(),
                    result.getConfig().getTotalPoints(),
                    result.getPiEstimate(),
                    result.getAbsoluteError(),
                    result.getRuntimeMs()
            ));
        }
    }


    public static void main(String[] args) {
        PiExperimentRunner runner = new PiExperimentRunner();

        long[] pointsList = {100_000, 1_000_000, 10_000_000};
        int[] threadCounts = {2, 4, 8};

        List<ExperimentResult> batchResults =
                runner.runComprehensiveExperiments(pointsList, threadCounts);

        runner.printResultsSummary(batchResults);

        System.out.println("\n=== BONUS: Trials Example ===\n");

        int trials = 4;
        long N = 1_000_000;
        int threads = 4;

        PiEstimator estimator = new ParallelPiEstimator();
        SimulationConfig config =
                new SimulationConfig(N, threads * 2, threads);

        runner.runTrials(estimator, config,
                "Parallel(" + threads + " threads)", trials);

        System.out.println("\nActual π value: " + Math.PI);
    }
}