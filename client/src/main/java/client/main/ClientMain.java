package client.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientMain extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/ui/fxml/login.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 480, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Đăng nhập - Quản Lý Công Việc");
        primaryStage.setMinWidth(400);
        primaryStage.setMinHeight(500);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
