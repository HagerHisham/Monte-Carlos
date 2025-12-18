package edu.montecarlo.model;

import java.util.Random;

public class SequentialPiEstimator implements PiEstimator {

    @Override
    public double estimatePi(SimulationConfig config) {
        Random random = new Random();
        long pointsInsideCircle = 0;

        for (long i = 0; i < config.getTotalPoints(); i++) {
            double x = random.nextDouble() * 2 - 1; 
            double y = random.nextDouble() * 2 - 1; 

            if (x * x + y * y <= 1.0) {
                pointsInsideCircle++;
            }
        }

        return 4.0 * pointsInsideCircle / config.getTotalPoints();
    }
}
