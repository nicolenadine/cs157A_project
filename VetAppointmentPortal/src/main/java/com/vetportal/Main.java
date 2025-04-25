package com.vetportal;

import com.vetportal.service.ServiceManager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class Main extends Application {

    private ServiceManager serviceManager;

    @Override
    public void start(Stage primaryStage) throws Exception {

        serviceManager = new ServiceManager(); //singleton

        URL fxmlUrl = getClass().getResource("/fxml/main.fxml");
        if (fxmlUrl == null) {
            throw new IllegalStateException("FXML file not found: /fxml/main.fxml");
        }

        Parent root = FXMLLoader.load(fxmlUrl);

        primaryStage.setTitle("Vet Appointment Portal");
        primaryStage.setScene(new Scene(root));
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
