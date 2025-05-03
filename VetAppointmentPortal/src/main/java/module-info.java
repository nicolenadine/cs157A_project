module VetAppointmentPortal {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;

    opens com.vetportal to javafx.fxml;
    opens com.vetportal.controller to javafx.fxml;

    exports com.vetportal;
}