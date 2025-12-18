package edu.montecarlo.gui;

import edu.montecarlo.experiment.ExperimentResult;
import edu.montecarlo.experiment.MultiTrialExperimentResult;
import edu.montecarlo.experiment.PiExperimentRunner;
import edu.montecarlo.model.MonteCarloEstimator;
import edu.montecarlo.model.ParallelIntegrationEstimator;
import edu.montecarlo.model.ParallelPiEstimator;
import edu.montecarlo.model.SequentialIntegrationEstimator;
import edu.montecarlo.model.SequentialPiEstimator;
import edu.montecarlo.model.SimulationConfig;
import edu.montecarlo.model.SimulationType;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ComboBox;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.paint.Color;

/**
 * Controller for the Monte Carlo π Estimation GUI.
 * Handles user interactions and visualization updates.
 */
public class MainController {

    @FXML
    private Canvas visualizationCanvas;
    @FXML
    private ComboBox<SimulationType> simulationTypeCombo;
    @FXML
    private Spinner<Integer> pointsSpinner;
    @FXML
    private Spinner<Integer> threadsSpinner;
    @FXML
    private Spinner<Integer> trialsSpinner;
    @FXML
    private RadioButton sequentialRadio;
    @FXML
    private RadioButton parallelRadio;
    @FXML
    private Button startButton;
    @FXML
    private Button stopButton;
    @FXML
    private Button clearButton;
    @FXML
    private Label piEstimateLabel;
    @FXML
    private Label errorLabel;
    @FXML
    private Label pointsLabel;
    @FXML
    private Label timeLabel;
    @FXML
    private Label actualValueLabel;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label statusLabel;
    @FXML
    private TextArea resultsTextArea;

    private VisualizationTask currentTask;
    private Thread simulationThread;
    private long totalPoints = 0;
    private long pointsInside = 0;
    private long startTime;

    /**
     * Initialize the controller and set up UI components.
     */
    @FXML
    public void initialize() {
        // Set up spinners
        pointsSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(100, 10000000, 10000, 1000));
        threadsSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 32, 4, 1));
        trialsSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 1, 1));

        // Set up Simulation Type Combo
        simulationTypeCombo.getItems().addAll(SimulationType.values());
        simulationTypeCombo.setValue(SimulationType.PI_ESTIMATION);
        simulationTypeCombo.setOnAction(e -> {
            drawInitialCanvas();
            updateActualValueLabel();
        });

        // Group radio buttons
        ToggleGroup group = new ToggleGroup();
        sequentialRadio.setToggleGroup(group);
        parallelRadio.setToggleGroup(group);
        sequentialRadio.setSelected(true);

        // Disable threads spinner when sequential is selected
        threadsSpinner.setDisable(true);
        sequentialRadio.setOnAction(e -> threadsSpinner.setDisable(true));
        parallelRadio.setOnAction(e -> threadsSpinner.setDisable(false));

        // Update Actual Value Label
        updateActualValueLabel();

        // Draw initial canvas (circle and square)
        drawInitialCanvas();

        // Disable stop button initially
        stopButton.setDisable(true);

        // Add welcome message
        resultsTextArea.setText("Monte Carlo Simulation\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "Configure parameters and click Start to begin simulation.\n");
    }

    private void updateActualValueLabel() {
        if (actualValueLabel != null && simulationTypeCombo.getValue() != null) {
            actualValueLabel.setText(String.format("%.10f", simulationTypeCombo.getValue().getActualValue()));
        }
    }

    /**
     * Draws the initial canvas with boundaries.
     */
    private void drawInitialCanvas() {
        GraphicsContext gc = visualizationCanvas.getGraphicsContext2D();
        double width = visualizationCanvas.getWidth();
        double height = visualizationCanvas.getHeight();

        // Clear canvas
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, width, height);

        // Draw border
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        gc.strokeRect(0, 0, width, height);

        SimulationType type = simulationTypeCombo.getValue();
        if (type == SimulationType.INTEGRATION_X_SQUARED) {
            // Draw y = x^2 curve
            // Coordinate system: x [0, width], y [height, 0] (inverted y)
            gc.setStroke(Color.rgb(52, 152, 219, 0.7)); // Blue
            gc.setLineWidth(2);
            gc.beginPath();
            gc.moveTo(0, height);
            
            for (int px = 0; px <= width; px++) {
                double x = (double) px / width; // [0, 1]
                double y = x * x; // [0, 1]
                double py = height - (y * height);
                gc.lineTo(px, py);
            }
            gc.stroke();
            
            // Fill area under curve lightly
            // gc.setFill(Color.rgb(52, 152, 219, 0.1));
            // gc.lineTo(width, height);
            // gc.lineTo(0, height);
            // gc.fill();
            
        } else {
            // Draw full circle (inscribed in the square)
            gc.setStroke(Color.rgb(52, 152, 219, 0.7)); // Blue circle
            gc.setLineWidth(2);
            double radius = Math.min(width, height) / 2;
            double centerX = width / 2;
            double centerY = height / 2;
            gc.strokeOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
        }
    }

    /**
     * Starts the Monte Carlo simulation.
     */
    @FXML
    private void handleStart() {
        // Reset counters
        totalPoints = 0;
        pointsInside = 0;
        startTime = System.currentTimeMillis();

        // Clear canvas
        drawInitialCanvas();

        // Get parameters
        int numPoints = pointsSpinner.getValue();
        boolean isParallel = parallelRadio.isSelected();
        int numThreads = threadsSpinner.getValue();

        // Create visualization task
        SimulationType type = simulationTypeCombo.getValue();
        currentTask = new VisualizationTask(numPoints, isParallel, numThreads, type, this::addPoint);

        // Bind UI to task
        progressBar.progressProperty().bind(currentTask.progressProperty());
        statusLabel.textProperty().bind(currentTask.messageProperty());

        // Handle task completion
        currentTask.setOnSucceeded(e -> {
            double piEstimate = currentTask.getValue();
            long elapsed = System.currentTimeMillis() - startTime;

            piEstimateLabel.setText(String.format("%.10f", piEstimate));
            errorLabel.setText(String.format("%.10f", Math.abs(piEstimate - Math.PI)));
            timeLabel.setText(elapsed + " ms");

            // Log result
            String mode = isParallel ? "Parallel (" + numThreads + " threads)" : "Sequential";
            resultsTextArea.appendText(String.format("\n[%s] %,d points → π ≈ %.6f, Error: %.6f, Time: %,d ms",
                    mode, numPoints, piEstimate,
                    Math.abs(piEstimate - Math.PI), elapsed));

            startButton.setDisable(false);
            stopButton.setDisable(true);
        });

        currentTask.setOnCancelled(e -> {
            statusLabel.setText("Simulation cancelled");
            startButton.setDisable(false);
            stopButton.setDisable(true);
        });

        // Run task in background thread
        simulationThread = new Thread(currentTask);
        simulationThread.setDaemon(true);
        simulationThread.start();

        // Update button states
        startButton.setDisable(true);
        stopButton.setDisable(false);
    }

    /**
     * Stops the current simulation.
     */
    @FXML
    private void handleStop() {
        if (currentTask != null && simulationThread != null) {
            currentTask.cancel();
            simulationThread.interrupt();
        }
    }

    /**
     * Clears the visualization and results.
     */
    @FXML
    private void handleClear() {
        drawInitialCanvas();
        piEstimateLabel.setText("---");
        errorLabel.setText("---");
        pointsLabel.setText("0");
        timeLabel.setText("---");
        progressBar.setProgress(0);
        statusLabel.setText("Ready");
        resultsTextArea.clear();
        totalPoints = 0;
        pointsInside = 0;
    }

    /**
     * Runs batch experiments comparing different configurations.
     */
    @FXML
    private void handleRunExperiments() {
        resultsTextArea.appendText("\n\n=== Running Batch Experiments ===\n");

        // Run experiments in background
        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                PiExperimentRunner runner = new PiExperimentRunner();
                long[] pointsList = { 100_000, 500_000, 1_000_000 };
                int[] threadCounts = { 2, 4, 8 };

                var results = runner.runComprehensiveExperiments(pointsList, threadCounts);

                StringBuilder sb = new StringBuilder();
                for (ExperimentResult result : results) {
                    sb.append(result.toString()).append("\n");
                }
                return sb.toString();
            }
        };

        task.setOnSucceeded(e -> {
            resultsTextArea.appendText(task.getValue());
            resultsTextArea.appendText("\nExperiments completed!\n");
        });

        new Thread(task).start();
    }

    /**
     * Adds a point to the visualization.
     * Called from the background task for each generated point.
     */
    private void addPoint(VisualizationTask.PointData point) {
        GraphicsContext gc = visualizationCanvas.getGraphicsContext2D();
        double width = visualizationCanvas.getWidth();
        double height = visualizationCanvas.getHeight();
        SimulationType type = simulationTypeCombo.getValue();
        
        double canvasX, canvasY;
        
        if (type == SimulationType.INTEGRATION_X_SQUARED) {
             // point.x [0,1], point.y [0,1]
             canvasX = point.x * width;
             canvasY = height - (point.y * height);
        } else {
             // point.x [-1,1], point.y [-1,1]
             double radius = Math.min(width, height) / 2;
             double centerX = width / 2;
             double centerY = height / 2;
             canvasX = centerX + point.x * radius;
             canvasY = centerY + point.y * radius;
        }

        // Draw point
        gc.setFill(point.inside ? Color.rgb(0, 200, 0, 0.7) : Color.rgb(200, 0, 0, 0.7));
        gc.fillOval(canvasX - 1.5, canvasY - 1.5, 3, 3);

        // Update counters
        totalPoints++;
        if (point.inside) {
            pointsInside++;
        }

        // Update labels
        pointsLabel.setText(String.format("%,d", totalPoints));

        if (totalPoints > 0) {
            double currentEstimate = (double) pointsInside / totalPoints;
            if (type == SimulationType.PI_ESTIMATION) currentEstimate *= 4.0;
            
            double actualValue = type.getActualValue();
            piEstimateLabel.setText(String.format("%.10f", currentEstimate));
            errorLabel.setText(String.format("%.10f", Math.abs(currentEstimate - actualValue)));
        }
    }
}
