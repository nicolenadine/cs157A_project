package com.vetportal.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;
import java.io.IOException;
import java.net.URL;
import com.vetportal.util.CommonUtil;

//util class for dynamically displaying views
public class FXUtil {
    static CommonUtil common = CommonUtil.getInstance(); //singleton

    //set page based on path (just UI)
    public static void setPage(String path) {
        URL url = FXUtil.class.getResource(path);
        try {
            HBox mainBox = common.getMainBox();

            if (mainBox.getChildren().size() > 1) //remove existing page
                mainBox.getChildren().remove(1);

            Parent panel = FXMLLoader.load(url);
            mainBox.getChildren().add(panel);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //set page and db data based on path
    public static <T> T setCustomPage(String path) {
        URL url = FXUtil.class.getResource(path);
        try {
            HBox mainBox = common.getMainBox();
            if (mainBox.getChildren().size() > 1)
                mainBox.getChildren().remove(1);

            // return the loader to pass account later
            FXMLLoader loader = new FXMLLoader(url);
            Parent panel = loader.load();
            mainBox.getChildren().add(panel);
            return loader.getController();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
