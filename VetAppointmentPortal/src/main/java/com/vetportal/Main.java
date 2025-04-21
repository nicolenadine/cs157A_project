package com.vetportal;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        URL fxmlUrl = getClass().getResource("/fxml/main.fxml");
        if (fxmlUrl == null) {
            throw new IllegalStateException("FXML file not found: /fxml/main.fxml");
        }
        Parent root = FXMLLoader.load(fxmlUrl);

        primaryStage.setTitle("Vet Appointment Portal");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
