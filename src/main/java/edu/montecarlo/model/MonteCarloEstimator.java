package edu.montecarlo.model;

/**
 * Interface for Monte Carlo estimation strategies.
 * Allows generic implementation for different problems (Pi, Integration, Volume, etc.).
 */
public interface MonteCarloEstimator {
    /**
     * Estimates the value using Monte Carlo simulation.
     * 
     * @param config Simulation configuration parameters
     * @return Estimated value
     */
    double estimate(SimulationConfig config);
    
    /**
     * Returns the name of the estimator.
     * @return Name of the estimator
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
}
