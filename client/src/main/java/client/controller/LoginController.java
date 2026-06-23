package client.controller;

import client.service.ServerConnector;
import client.model.Request;
import client.model.Response;
import client.model.UserDTO;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

public class LoginController {

    @FXML private VBox root;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;
    @FXML private Label loadingLabel;
    @FXML private Label serverStatusLabel;

    private ServerConnector connector;
    private Gson gson;

    public LoginController() {
        connector = new ServerConnector();
        gson = new Gson();
    }

    @FXML
    private void initialize() {
        root.getStylesheets().add(getClass().getResource("/client/view/css/login.css").toExternalForm());
        
        // Khoi chay kiem tra trang thai server qua UDP
        Thread statusThread = new Thread(() -> {
            while (true) {
                boolean isOnline = client.service.UDPClientUtil.checkServerOnline();
                javafx.application.Platform.runLater(() -> {
                    if (isOnline) {
                        serverStatusLabel.setText("● Server: Trực tuyến");
                        serverStatusLabel.getStyleClass().removeAll("server-status-offline");
                        if (!serverStatusLabel.getStyleClass().contains("server-status-online")) {
                            serverStatusLabel.getStyleClass().add("server-status-online");
                        }
                    } else {
                        serverStatusLabel.setText("○ Server: Ngoại tuyến");
                        serverStatusLabel.getStyleClass().removeAll("server-status-online");
                        if (!serverStatusLabel.getStyleClass().contains("server-status-offline")) {
                            serverStatusLabel.getStyleClass().add("server-status-offline");
                        }
                    }
                });
                try {
                    Thread.sleep(3000); // Kiem tra moi 3 giay
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "Server-UDP-Status-Checker");
        statusThread.setDaemon(true);
        statusThread.start();
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        hideError();
        setLoading(true);

        Task<Response> loginTask = new Task<>() {
            @Override
            protected Response call() {
                Map<String, String> loginData = new HashMap<>();
                loginData.put("username", username);
                loginData.put("password", password);
                Request request = new Request("LOGIN", loginData);
                return connector.sendRequest(request);
            }
        };

        loginTask.setOnSucceeded(event -> {
            setLoading(false);
            Response response = loginTask.getValue();
            if (response.isSuccess()) {
                java.lang.reflect.Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
                Map<String, Object> loginResult = gson.fromJson(gson.toJson(response.getData()), mapType);
                String token = (String) loginResult.get("token");
                java.lang.reflect.Type userType = new TypeToken<UserDTO>() {}.getType();
                UserDTO user = gson.fromJson(gson.toJson(loginResult.get("user")), userType);
                ServerConnector.setSessionToken(token);
                openDashboard(user);
            } else {
                showError(response.getMessage());
            }
        });

        loginTask.setOnFailed(event -> {
            setLoading(false);
            showError("Lỗi kết nối: " + loginTask.getException().getMessage());
        });

        new Thread(loginTask).start();
    }

    private void openDashboard(UserDTO user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/view/fxml/dashboard.fxml"));
            Parent root = loader.load();
            DashboardController controller = loader.getController();
            controller.initData(user);

            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(root, 1200, 800);
            stage.setScene(scene);
            stage.setTitle("Dashboard - " + user.getUsername() + " (" + user.getRole() + ")");
            stage.setMinWidth(900);
            stage.setMinHeight(600);
            stage.centerOnScreen();
        } catch (Exception e) {
            showError("Lỗi mở dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setLoading(boolean loading) {
        loginButton.setDisable(loading);
        loginButton.setText(loading ? "Đang xử lý..." : "Đăng nhập");
        usernameField.setDisable(loading);
        passwordField.setDisable(loading);
        loadingLabel.setVisible(loading);
        loadingLabel.setManaged(loading);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
