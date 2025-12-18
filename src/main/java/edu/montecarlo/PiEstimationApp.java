package edu.montecarlo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class PiEstimationApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load FXML
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/edu/montecarlo/gui/main.fxml"));
        Scene scene = new Scene(loader.load());

        // Configure stage
        primaryStage.setTitle("Monte Carlo π Estimation");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);

        // Show window
        primaryStage.show();
    }

    /**
     * Application entry point.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
