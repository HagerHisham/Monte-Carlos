package edu.montecarlo.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;


public class ParallelPiEstimator implements PiEstimator {

    @Override
    public double estimatePi(SimulationConfig config) {
        ExecutorService executor = Executors.newFixedThreadPool(config.getNumThreads());

        long pointsPerTask = config.getTotalPoints() / config.getNumTasks();
        long remainder = config.getTotalPoints() % config.getNumTasks();

        List<Future<Long>> futures = new ArrayList<>();
        for (int i = 0; i < config.getNumTasks(); i++) {
            long points = (i == config.getNumTasks() - 1) ? pointsPerTask + remainder : pointsPerTask;
            futures.add(executor.submit(new MonteCarloTask(points)));
        }

        long totalPointsInsideCircle = 0;
        try {
            for (Future<Long> future : futures) {
                totalPointsInsideCircle += future.get(); 
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error during parallel execution", e);
        } finally {
            executor.shutdown(); 
        }

        return 4.0 * totalPointsInsideCircle / config.getTotalPoints();
    }


    private static class MonteCarloTask implements Callable<Long> {
        private final long numPoints;

        public MonteCarloTask(long numPoints) {
            this.numPoints = numPoints;
        }

        @Override
        public Long call() {
            long hitsInsideCircle = 0;

            ThreadLocalRandom random = ThreadLocalRandom.current();

            for (long i = 0; i < numPoints; i++) {
            double x = random.nextDouble() * 2 - 1;
            double y = random.nextDouble() * 2 - 1;

                if (x * x + y * y <= 1.0) {
                    hitsInsideCircle++;
                }
            }

            return hitsInsideCircle;
        }
    }
}
