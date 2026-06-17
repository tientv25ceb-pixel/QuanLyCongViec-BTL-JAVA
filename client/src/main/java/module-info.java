module client {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;

    opens client.main to javafx.graphics;
    opens client.ui.controller to javafx.fxml;
    opens common.model to com.google.gson;

    exports client.main;
    exports client.ui.controller;
    exports common.model;
}
