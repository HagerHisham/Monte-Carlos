package edu.montecarlo.model;

import java.util.Random;

/**
 * Sequential implementation of Integration estimation.
 * Estimates integral of x^2 from 0 to 1.
 * True value = 1/3 ≈ 0.333333
 */
public class SequentialIntegrationEstimator implements MonteCarloEstimator {

    @Override
    public double estimate(SimulationConfig config) {
        Random random = new Random();
        long pointsUnderCurve = 0;

        for (long i = 0; i < config.getTotalPoints(); i++) {
            double x = random.nextDouble(); // [0, 1)
            double y = random.nextDouble(); // [0, 1)

            // Check if point is under y = x^2
            if (y < x * x) {
                pointsUnderCurve++;
            }
        }

        // Area = 1 * 1 * (pointsUnderCurve / totalPoints)
        return (double) pointsUnderCurve / config.getTotalPoints();
    }
    
    @Override
    public String getName() {
        return "Sequential Integration (x^2)";
    }
}
