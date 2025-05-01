package com.vetportal.controller;

import com.vetportal.util.FXUtil;
import com.vetportal.util.CommonUtil;
import javafx.fxml.FXML;
import javafx.scene.layout.HBox;

public class MenuController {

    @FXML
    private HBox mainBox;

    @FXML
    public void initialize() {
        CommonUtil commonObjs = CommonUtil.getInstance();
        commonObjs.setMainBox(mainBox);

        FXUtil.setPage("/fxml/Home.fxml");
    }

    @FXML
    public void showHome() {
        FXUtil.setPage("/fxml/Home.fxml");
    }

    @FXML
    public void showAddAppointment() {
        FXUtil.setPage("/fxml/AddAppointment.fxml");
    }

    @FXML
    public void showEditAppointment() {
        FXUtil.setPage("/fxml/EditAppointment.fxml");
    }

    @FXML
    public void showDeleteAppointment() {
        FXUtil.setPage("/fxml/DeleteAppointment.fxml");
    }

    @FXML
    public void showProfile() {
        FXUtil.setPage("/fxml/Profile.fxml");
    }
}
