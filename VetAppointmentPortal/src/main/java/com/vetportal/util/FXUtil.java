package com.vetportal.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import java.io.IOException;


public class FXUtil {
    public static void setPage(Pane container, String fxmlPath){
        try {
            FXMLLoader loader = new FXMLLoader(FXUtil.class.getResource(fxmlPath));
            Parent page = loader.load();
            container.getChildren().setAll(page);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
