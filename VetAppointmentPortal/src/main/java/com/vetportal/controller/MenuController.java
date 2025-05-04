package com.vetportal.controller;

import com.vetportal.service.ServiceManager;
import com.vetportal.util.FXUtil;
import com.vetportal.util.CommonUtil;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MenuController {

    @FXML private HBox mainBox;
    @FXML private Label dateTime;
    @FXML private Circle profileImage;

    @FXML
    public void initialize() {
        CommonUtil commonObjs = CommonUtil.getInstance();
        commonObjs.setMainBox(mainBox);

        // Initialize the date and time
        updateDateTime();

        // Create a timeline to update the date and time every second
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> updateDateTime())
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

        setProfileImage();

        FXUtil.setPage("/fxml/Home.fxml");
    }

    private void updateDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d HH:mm");
        dateTime.setText(now.format(formatter));
    }

    private void setProfileImage() {
        String imagePath = "/images/profile.png";

        // Create an image from the resource path
        Image image = new Image(getClass().getResourceAsStream("/media/icon.png"));

        profileImage.setFill(new ImagePattern(image));
    }

    @FXML
    public void showHome() {
        FXUtil.setPage("/fxml/Home.fxml");
    }

    @FXML
    public void showCreateAppointment() {
        CreateAppointmentController controller = FXUtil.setCustomPage("/fxml/CreateAppointment.fxml");

        // Now you have the controller, set the connection
        if (controller != null) {
            // Get connection from ServiceManager or another source
            Connection conn = ServiceManager.getInstance().getConnection();
            controller.setConnection(conn);
        }
    }

    @FXML
    public void showCustomerSearch() {
        FXUtil.setPage("/fxml/Customer.fxml");
    }

    public void showEmployeeSearch() {
        FXUtil.setPage("/fxml/Employee.fxml");
    }

    @FXML
    public void showEditAppointment() {
        FXUtil.setPage("/fxml/AppointmentSearch.fxml");
    }
}
