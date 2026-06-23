module client {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;

    opens client.main to javafx.graphics;
    opens client.controller to javafx.fxml;
    opens client.model to com.google.gson;

    exports client.main;
    exports client.controller;
    exports client.model;
}
