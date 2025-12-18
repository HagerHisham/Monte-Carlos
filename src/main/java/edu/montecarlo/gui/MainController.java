package edu.montecarlo.gui;

import java.util.List;

import edu.montecarlo.experiment.ExperimentResult;
import edu.montecarlo.experiment.PiExperimentRunner;
import edu.montecarlo.model.ParallelPiEstimator;
import edu.montecarlo.model.PiEstimator;
import edu.montecarlo.model.SimulationConfig;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
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

public class MainController {

    @FXML private Canvas visualizationCanvas;
    @FXML private Spinner<Integer> pointsSpinner;
    @FXML private Spinner<Integer> threadsSpinner;
    @FXML private RadioButton sequentialRadio;
    @FXML private RadioButton parallelRadio;
    @FXML private Button startButton;
    @FXML private Button stopButton;
    @FXML private Button clearButton;
    @FXML private Button runExperimentsButton;
    @FXML private Label piEstimateLabel;
    @FXML private Label errorLabel;
    @FXML private Label pointsLabel;
    @FXML private Label timeLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;
    @FXML private TextArea resultsTextArea;

    private VisualizationTask currentTask;
    private Thread simulationThread;
    private long totalPoints = 0;
    private long pointsInside = 0;
    private long startTime;

    @FXML
    public void initialize() {
        pointsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(100, 10_000_000, 10_000, 1_000));
        threadsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 16, 4, 1));

        ToggleGroup group = new ToggleGroup();
        sequentialRadio.setToggleGroup(group);
        parallelRadio.setToggleGroup(group);
        sequentialRadio.setSelected(true);
        threadsSpinner.setDisable(true);
        sequentialRadio.setOnAction(e -> threadsSpinner.setDisable(true));
        parallelRadio.setOnAction(e -> threadsSpinner.setDisable(false));

        drawInitialCanvas();
        stopButton.setDisable(true);

        resultsTextArea.setText(
                "Monte Carlo π Estimation\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "Green → inside circle\n" +
                "Red   → outside circle\n\n" +
                "Actual π value: " + Math.PI + "\n"
        );
    }

    private void drawInitialCanvas() {
        GraphicsContext gc = visualizationCanvas.getGraphicsContext2D();
        double w = visualizationCanvas.getWidth();
        double h = visualizationCanvas.getHeight();

        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, w, h);

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        gc.strokeRect(0, 0, w, h);

        double r = Math.min(w, h) / 2;
        gc.setStroke(Color.rgb(52, 152, 219, 0.7));
        gc.strokeOval(w / 2 - r, h / 2 - r, r * 2, r * 2);
    }

    @FXML
    private void handleStart() {
        totalPoints = 0;
        pointsInside = 0;
        startTime = System.currentTimeMillis();
        drawInitialCanvas();

        int numPoints = pointsSpinner.getValue();
        boolean parallel = parallelRadio.isSelected();
        int threads = threadsSpinner.getValue();

        currentTask = new VisualizationTask(numPoints, parallel, threads, this::addPoint);

        progressBar.progressProperty().bind(currentTask.progressProperty());
        statusLabel.textProperty().bind(currentTask.messageProperty());

        currentTask.setOnSucceeded(e -> {
            double pi = currentTask.getValue();
            long time = System.currentTimeMillis() - startTime;

            piEstimateLabel.setText(String.format("%.10f", pi));
            errorLabel.setText(String.format("%.10f", Math.abs(pi - Math.PI)));
            timeLabel.setText(time + " ms");

            String mode = parallel ? "Parallel (" + threads + " threads)" : "Sequential";

            resultsTextArea.appendText(
                    String.format("\n[%s] %,d points → π ≈ %.6f | Error: %.6f | Time: %,d ms",
                            mode, numPoints, pi, Math.abs(pi - Math.PI), time)
            );

            startButton.setDisable(false);
            stopButton.setDisable(true);
        });

        currentTask.setOnCancelled(e -> {
            statusLabel.setText("Cancelled");
            startButton.setDisable(false);
            stopButton.setDisable(true);
        });

        simulationThread = new Thread(currentTask);
        simulationThread.setDaemon(true);
        simulationThread.start();

        startButton.setDisable(true);
        stopButton.setDisable(false);
    }

    @FXML
    private void handleStop() {
        if (currentTask != null) {
            currentTask.cancel();
            simulationThread.interrupt();
        }
    }

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

@FXML
private void handleRunExperiments() {
    resultsTextArea.clear();
    resultsTextArea.appendText("=== Monte Carlo π Estimation ===\n");
    resultsTextArea.appendText("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
    resultsTextArea.appendText("Green → inside circle\nRed   → outside circle\n\n");

    Task<Void> task = new Task<>() {
        @Override
        protected Void call() {
            PiExperimentRunner runner = new PiExperimentRunner();

            long points = pointsSpinner.getValue();
            int threads = threadsSpinner.getValue();
            int numTasks = threads * 2;

            SimulationConfig config = new SimulationConfig(points, numTasks, threads);

            long[] pointsList = {100_000, 1_000_000, 10_000_000};
            int[] threadCounts = {2, 4, 8};

            List<ExperimentResult> batchResults = runner.runComprehensiveExperiments(pointsList, threadCounts);

            StringBuilder batchSb = new StringBuilder();
            batchSb.append("\n=== Experiment Summary ===\n");
            batchSb.append(String.format("%-25s | %-15s | %-12s | %-12s | %-10s\n",
                    "Estimator", "Points", "π Estimate", "Error", "Time (ms)"));
            batchSb.append("-".repeat(90)).append("\n");

            for (ExperimentResult result : batchResults) {
                batchSb.append(String.format("%-25s | %,15d | %.10f | %.10f | %,10d\n",
                        result.getEstimatorType(),
                        result.getConfig().getTotalPoints(),
                        result.getPiEstimate(),
                        result.getAbsoluteError(),
                        result.getRuntimeMs()));
            }

            Platform.runLater(() -> resultsTextArea.appendText(batchSb.toString()));

            int trials = 4;
            PiEstimator parallelEstimator = new ParallelPiEstimator();
            ExperimentResult trialResult = runner.runTrials(parallelEstimator, config,
                    "Parallel(" + threads + " threads)", trials);

            StringBuilder trialSb = new StringBuilder();
            trialSb.append(String.format("\n---- Trials (%d) for Parallel(%d threads) | N=%s | Threads=%d ----\n",
                    trials, threads, String.format("%,d", points), threads));

            int i = 1;
            for (ExperimentResult trial : trialResult.getTrialResults()) {
                trialSb.append(String.format("Trial %d | π = %.6f | Error = %.6f | Time = %d ms\n",
                        i++, trial.getPiEstimate(), trial.getAbsoluteError(), trial.getRuntimeMs()));
            }

            trialSb.append(String.format("AVG | π = %.6f | Avg Error = %.6f | Avg Time = %d ms\n",
                    trialResult.getPiEstimate(), trialResult.getAverageError(), trialResult.getRuntimeMs()));

            trialSb.append(String.format("\nActual π value: %.15f\n", Math.PI));

            Platform.runLater(() -> resultsTextArea.appendText(trialSb.toString()));
            return null;
        }
    };

    new Thread(task).start();
}

    private void addPoint(VisualizationTask.PointData p) {
        GraphicsContext gc = visualizationCanvas.getGraphicsContext2D();
        double w = visualizationCanvas.getWidth();
        double h = visualizationCanvas.getHeight();

        double r = Math.min(w, h) / 2;
        double cx = w / 2;
        double cy = h / 2;

        double x = cx + p.x * r;
        double y = cy + p.y * r;

        gc.setFill(p.insideCircle ? Color.rgb(0, 180, 0, 0.7) : Color.rgb(200, 0, 0, 0.7));
        gc.fillOval(x - 1.5, y - 1.5, 3, 3);

        totalPoints++;
        if (p.insideCircle) pointsInside++;
        pointsLabel.setText(String.format("%,d", totalPoints));

        double estimate = 4.0 * pointsInside / totalPoints;
        piEstimateLabel.setText(String.format("%.10f", estimate));
        errorLabel.setText(String.format("%.10f", Math.abs(estimate - Math.PI)));
    }
}