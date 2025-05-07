package com.vetportal.util;

import javafx.scene.layout.HBox;

public class CommonUtil {

    private static CommonUtil commonUtil = new CommonUtil();

    private HBox mainBox;

    private CommonUtil() {
    }

    public static CommonUtil getInstance() {
        return commonUtil;
    }

    public HBox getMainBox() {
        return mainBox;
    }

    public void setMainBox(HBox mainBox) {
        this.mainBox = mainBox;
    }
}
