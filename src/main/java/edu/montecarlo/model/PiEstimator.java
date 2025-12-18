package edu.montecarlo.model;

/**
 * Interface for π estimation strategies.
 * Allows both sequential and parallel implementations.
 * Extends generic MonteCarloEstimator.
 */
public interface PiEstimator extends MonteCarloEstimator {
    /**
     * Estimates the value of π using Monte Carlo simulation.
     * Delegates to generic estimate method.
     * 
     * @param config Simulation configuration parameters
     * @return Estimated value of π
     */
    default double estimatePi(SimulationConfig config) {
        return estimate(config);
    }
}
