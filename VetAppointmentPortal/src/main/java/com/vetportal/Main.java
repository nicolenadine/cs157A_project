package com.vetportal;

import com.vetportal.service.ServiceManager;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Main extends Application {

    private ServiceManager serviceManager;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Initialize ServiceManager
        serviceManager = new ServiceManager();

        // Create a simple UI without FXML
        Label label = new Label("Welcome to the Vet Appointment Portal!");
        StackPane root = new StackPane(label);

        primaryStage.setTitle("Vet Appointment Portal");
        primaryStage.setScene(new Scene(root, 600, 400));
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        // shut down ServiceManager which closes DB connection
        if (serviceManager != null) {
            serviceManager.close();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}