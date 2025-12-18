package edu.montecarlo.gui;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import edu.montecarlo.model.SimulationType;
import javafx.application.Platform;
import javafx.concurrent.Task;

/**
 * Background task for Monte Carlo simulation with real-time updates.
 * Generates random points and reports progress for visualization.
 */
public class VisualizationTask extends Task<Double> {
    private final long totalPoints;
    private final boolean isParallel;
    private final SimulationType type;
    private final Consumer<PointData> pointCallback; // Called for each point generated

    private long pointsInside = 0;

    /**
     * Data class for a single random point.
     */
    public static class PointData {
        public final double x;
        public final double y;
        public final boolean inside;

        public PointData(double x, double y, boolean inside) {
            this.x = x;
            this.y = y;
            this.inside = inside;
        }
    }

    public VisualizationTask(long totalPoints, boolean isParallel, int numThreads, SimulationType type,
            Consumer<PointData> pointCallback) {
        this.totalPoints = totalPoints;
        this.isParallel = isParallel;
        this.type = type;
        this.pointCallback = pointCallback;
    }

    @Override
    protected Double call() throws Exception {
        long startTime = System.currentTimeMillis();

        if (isParallel) {
            runParallelSimulation();
        } else {
            runSequentialSimulation();
        }

        long endTime = System.currentTimeMillis();
        updateMessage("Completed in " + (endTime - startTime) + " ms");

        double ratio = (double) pointsInside / totalPoints;
        if (type == SimulationType.PI_ESTIMATION) {
            return ratio * 4.0;
        } else {
            return ratio;
        }
    }

    /**
     * Sequential simulation with visualization updates.
     */
    private void runSequentialSimulation() throws InterruptedException {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (long i = 0; i < totalPoints && !isCancelled(); i++) {
            double x, y;
            boolean inside;

            if (type == SimulationType.INTEGRATION_X_SQUARED) {
                x = random.nextDouble(); // [0, 1]
                y = random.nextDouble(); // [0, 1]
                inside = y < x * x;
            } else {
                x = random.nextDouble() * 2 - 1; // [-1, 1]
                y = random.nextDouble() * 2 - 1; // [-1, 1]
                inside = x * x + y * y <= 1.0;
            }

            if (inside) {
                pointsInside++;
            }

            // Update visualization for every point (for smaller datasets)
            if (totalPoints < 10000 || i % (totalPoints / 10000) == 0) {
                PointData point = new PointData(x, y, inside);
                Platform.runLater(() -> pointCallback.accept(point));

                // Add delay for animation effect
                if (totalPoints <= 5000) {
                    Thread.sleep(1);
                }
            }

            // Update progress
            if (i % 1000 == 0) {
                updateProgress(i, totalPoints);
                double currentEstimate = (double) pointsInside / (i + 1);
                if (type == SimulationType.PI_ESTIMATION) currentEstimate *= 4.0;
                
                updateMessage(String.format("Est ≈ %.6f (%,d / %,d points)",
                        currentEstimate, i + 1, totalPoints));
            }
        }
    }

    /**
     * Parallel simulation with visualization updates.
     * Shows sampled points to maintain performance while providing visual feedback.
     */
    private void runParallelSimulation() throws InterruptedException {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (long i = 0; i < totalPoints && !isCancelled(); i++) {
            double x, y;
            boolean inside;

            if (type == SimulationType.INTEGRATION_X_SQUARED) {
                x = random.nextDouble();
                y = random.nextDouble();
                inside = y < x * x;
            } else {
                x = random.nextDouble() * 2 - 1;
                y = random.nextDouble() * 2 - 1;
                inside = x * x + y * y <= 1.0;
            }

            if (inside) {
                pointsInside++;
            }

            // Visualize sampled points
            long samplingRate = Math.max(1, totalPoints / 5000);
            if (i % samplingRate == 0) {
                PointData point = new PointData(x, y, inside);
                Platform.runLater(() -> pointCallback.accept(point));

                // Small delay for animation
                if (totalPoints <= 20000) {
                    Thread.sleep(1);
                }
            }

            // Update progress periodically
            if (i % 1000 == 0) {
                updateProgress(i, totalPoints);
                double currentEstimate = (double) pointsInside / (i + 1);
                if (type == SimulationType.PI_ESTIMATION) currentEstimate *= 4.0;

                updateMessage(String.format("Est ≈ %.6f (%,d / %,d points)",
                        currentEstimate, i + 1, totalPoints));
            }
        }
    }
}
