package edu.montecarlo.model;

/**
 * Enum defining available Monte Carlo simulation types.
 */
public enum SimulationType {
    PI_ESTIMATION("Pi Estimation (Circle)", Math.PI),
    INTEGRATION_X_SQUARED("Integration x^2 [0,1]", 1.0/3.0);

    private final String displayName;
    private final double actualValue;

    SimulationType(String displayName, double actualValue) {
        this.displayName = displayName;
        this.actualValue = actualValue;
    }

    public String getDisplayName() { return displayName; }
    public double getActualValue() { return actualValue; }
    
    @Override
    public String toString() { return displayName; }
}
