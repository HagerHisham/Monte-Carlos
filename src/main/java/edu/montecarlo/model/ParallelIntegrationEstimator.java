package edu.montecarlo.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Parallel implementation of Integration estimation.
 */
public class ParallelIntegrationEstimator implements MonteCarloEstimator {

    @Override
    public double estimate(SimulationConfig config) {
        ExecutorService executor = Executors.newFixedThreadPool(config.getNumThreads());
        
        long pointsPerTask = config.getTotalPoints() / config.getNumTasks();
        long remainder = config.getTotalPoints() % config.getNumTasks();

        List<Future<Long>> futures = new ArrayList<>();
        for (int i = 0; i < config.getNumTasks(); i++) {
            long points = (i == config.getNumTasks() - 1) ? pointsPerTask + remainder : pointsPerTask;
            futures.add(executor.submit(new IntegrationTask(points)));
        }

        long totalPointsUnderCurve = 0;
        try {
            for (Future<Long> future : futures) {
                totalPointsUnderCurve += future.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error during parallel execution", e);
        } finally {
            executor.shutdown();
        }

        return (double) totalPointsUnderCurve / config.getTotalPoints();
    }

    @Override
    public String getName() {
        return "Parallel Integration (x^2)";
    }

    private static class IntegrationTask implements Callable<Long> {
        private final long numPoints;

        public IntegrationTask(long numPoints) {
            this.numPoints = numPoints;
        }

        @Override
        public Long call() {
            long hits = 0;
            ThreadLocalRandom random = ThreadLocalRandom.current();

            for (long i = 0; i < numPoints; i++) {
                double x = random.nextDouble();
                double y = random.nextDouble();

                if (y < x * x) {
                    hits++;
                }
            }
            return hits;
        }
    }
}
