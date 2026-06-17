package client.ui.controller;

import client.service.CSVUtil;
import client.service.ServerConnector;
import common.model.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.chart.PieChart;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.*;
import java.util.List;

public class DashboardController {

    @FXML private BorderPane root;
    @FXML private Button deleteUserBtn;
    @FXML private Button deleteGroupBtn;
    @FXML private Label userLabel;
    @FXML private Label roleBadge;
    @FXML private TabPane tabPane;
    @FXML private GridPane statsGrid;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterCombo;
    @FXML private ComboBox<String> sortCombo;
    @FXML private TableView<Map<String, Object>> taskTable;
    @FXML private TableView<Map<String, Object>> userTable;
    @FXML private TableView<Map<String, Object>> groupTable;
    @FXML private Tab userTab;
    @FXML private Tab groupTab;
    @FXML private Button addTaskBtn;
    @FXML private Button deleteTaskBtn;
    @FXML private Button exportCsvBtn;
    @FXML private Button importCsvBtn;
    @FXML private PieChart taskStatusChart;

    // Task form
    @FXML private Label taskFormTitle, taskFormError;
    @FXML private TextField taskTitleField;
    @FXML private TextArea taskDescArea;
    @FXML private DatePicker taskDueDatePicker;
    @FXML private ComboBox<String> taskStatusCombo, taskPriorityCombo, taskGroupCombo, taskUserCombo;
    @FXML private Button taskSaveBtn, taskCancelBtn;

    // User form
    @FXML private Label userFormTitle, userFormError;
    @FXML private TextField userUsernameField, userFullNameField, userPhoneField;
    @FXML private PasswordField userPasswordField;
    @FXML private ComboBox<String> userRoleCombo;
    @FXML private Button userSaveBtn, userCancelBtn;

    // Group form
    @FXML private Label groupFormTitle, groupFormError;
    @FXML private TextField groupNameField;
    @FXML private TextArea groupDescArea;
    @FXML private Button groupSaveBtn, groupCancelBtn;
    @FXML private TableView<Map<String, Object>> groupMemberTable;
    @FXML private ComboBox<String> memberUserCombo;

    private UserDTO currentUser;
    private ServerConnector connector;
    private Gson gson;
    private ObservableList<Map<String, Object>> taskData;
    private ObservableList<Map<String, Object>> userData;
    private ObservableList<Map<String, Object>> groupData;
    private ObservableList<Map<String, Object>> groupMemberData;

    // State tracking for inline forms
    private TaskDTO selectedTask;
    private UserDTO selectedUser;
    private GroupDTO selectedGroup;
    private List<GroupDTO> groupList;
    private List<UserDTO> userList;

    public DashboardController() {
        connector = new ServerConnector();
        gson = new Gson();
    }

    @FXML
    private void initialize() {
        root.getStylesheets().add(getClass().getResource("/client/ui/css/dashboard.css").toExternalForm());
    }

    public void initData(UserDTO user) {
        this.currentUser = user;
        userLabel.setText("👤 " + user.getUsername());
        roleBadge.setText(user.getRole());

        if ("Admin".equals(user.getRole())) {
            deleteTaskBtn.setVisible(true);
            deleteTaskBtn.setManaged(true);
            exportCsvBtn.setVisible(true);
            exportCsvBtn.setManaged(true);
            importCsvBtn.setVisible(true);
            importCsvBtn.setManaged(true);
        } else {
            deleteTaskBtn.setVisible(false);
            deleteTaskBtn.setManaged(false);
            exportCsvBtn.setVisible(false);
            exportCsvBtn.setManaged(false);
            importCsvBtn.setVisible(false);
            importCsvBtn.setManaged(false);
            tabPane.getTabs().remove(userTab);
            tabPane.getTabs().remove(groupTab);
        }

        setupTaskTable();
        setupUserTable();
        setupGroupTable();
        setupGroupMemberTable();
        setupFilterCombo();
        setupSortCombo();
        setupTaskForm();
        setupUserForm();
        setupGroupForm();

        loadDashboardStats();
        loadAllTasks();
        loadFormComboData();
        if ("Admin".equals(user.getRole())) {
            loadUsers();
            loadGroups();
        }
    }

    private void setupFilterCombo() {
        filterCombo.getItems().addAll("Tất cả", "TODO", "IN_PROGRESS", "DONE");
        filterCombo.setValue("Tất cả");
        filterCombo.setOnAction(e -> handleFilterTasks());
    }

    private void setupSortCombo() {
        sortCombo.getItems().addAll(
                "Mặc định (ID)",
                "Tiêu đề A-Z",
                "Tiêu đề Z-A",
                "Hạn chót sớm nhất",
                "Hạn chót muộn nhất",
                "Trạng thái A-Z"
        );
        sortCombo.setValue("Mặc định (ID)");
        sortCombo.setOnAction(e -> loadAllTasks());
    }

    private Map<String, Object> buildSortParams() {
        Map<String, Object> params = new HashMap<>();
        String sortVal = sortCombo.getValue() != null ? sortCombo.getValue() : "Mặc định (ID)";
        switch (sortVal) {
            case "Tiêu đề A-Z" -> { params.put("sortBy", "title"); params.put("sortOrder", "ASC"); }
            case "Tiêu đề Z-A" -> { params.put("sortBy", "title"); params.put("sortOrder", "DESC"); }
            case "Hạn chót sớm nhất" -> { params.put("sortBy", "due_date"); params.put("sortOrder", "ASC"); }
            case "Hạn chót muộn nhất" -> { params.put("sortBy", "due_date"); params.put("sortOrder", "DESC"); }
            case "Trạng thái A-Z" -> { params.put("sortBy", "status"); params.put("sortOrder", "ASC"); }
            default -> { params.put("sortBy", "id"); params.put("sortOrder", "ASC"); }
        }
        return params;
    }

    private Map<String, Object> buildTaskQueryParams() {
        Map<String, Object> params = buildSortParams();
        if (!"Admin".equals(currentUser.getRole())) {
            params.put("userId", currentUser.getId());
        }
        return params;
    }

    private void setupTaskForm() {
        taskStatusCombo.getItems().addAll("TODO", "IN_PROGRESS", "DONE");
        taskStatusCombo.setValue("TODO");

        taskPriorityCombo.getItems().addAll("LOW", "MEDIUM", "HIGH");
        taskPriorityCombo.setValue("MEDIUM");

        taskTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) populateTaskForm(sel);
        });
    }

    private void populateTaskForm(Map<String, Object> task) {
        selectedTask = new TaskDTO();
        selectedTask.setId(((Number) task.get("id")).intValue());
        selectedTask.setGroupId(task.get("groupId") != null ? ((Number) task.get("groupId")).intValue() : 0);
        selectedTask.setAssigneeId(task.get("assigneeId") != null ? ((Number) task.get("assigneeId")).intValue() : 0);
        selectedTask.setCreatorId(task.get("creatorId") != null ? ((Number) task.get("creatorId")).intValue() : 0);

        taskFormTitle.setText("Sửa công việc");
        taskTitleField.setText(getStr(task.get("title")));
        taskDescArea.setText(getStr(task.get("description")));
        if (task.get("dueDate") != null && task.get("dueDate").toString().length() >= 10) {
            taskDueDatePicker.setValue(java.time.LocalDate.parse(task.get("dueDate").toString().substring(0, 10)));
        } else {
            taskDueDatePicker.setValue(null);
        }
        taskStatusCombo.setValue(getStr(task.get("status")));
        taskPriorityCombo.setValue(getStr(task.get("priority")));

        // Group combo
        if (task.get("groupId") != null && ((Number) task.get("groupId")).intValue() > 0) {
            String groupVal = task.get("groupId") + " - " + getStr(task.get("groupName"));
            if (taskGroupCombo.getItems().contains(groupVal)) {
                taskGroupCombo.setValue(groupVal);
            }
        } else {
            taskGroupCombo.setValue("Không (Cá nhân)");
        }
        // User combo
        if (task.get("assigneeId") != null && ((Number) task.get("assigneeId")).intValue() > 0) {
            String userVal = task.get("assigneeId") + " - " + getStr(task.get("assigneeName"));
            if (taskUserCombo.getItems().contains(userVal)) {
                taskUserCombo.setValue(userVal);
            }
        }

        taskFormError.setVisible(false);
        taskFormError.setManaged(false);
    }

    private void clearTaskForm() {
        selectedTask = null;
        taskFormTitle.setText("+ Thêm công việc");
        taskTitleField.clear();
        taskDescArea.clear();
        taskDueDatePicker.setValue(null);
        taskStatusCombo.setValue("TODO");
        taskPriorityCombo.setValue("MEDIUM");
        taskGroupCombo.setValue("Không (Cá nhân)");
        if (!taskUserCombo.getItems().isEmpty()) taskUserCombo.setValue(taskUserCombo.getItems().get(0));
        taskFormError.setVisible(false);
        taskFormError.setManaged(false);
    }

    private void setupUserForm() {
        userRoleCombo.getItems().addAll("User", "Admin");
        userRoleCombo.setValue("User");

        userTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) populateUserForm(sel);
        });
    }

    private void populateUserForm(Map<String, Object> user) {
        selectedUser = new UserDTO();
        selectedUser.setId(((Number) user.get("id")).intValue());
        selectedUser.setUsername(getStr(user.get("username")));
        selectedUser.setRole(getStr(user.get("role")));
        selectedUser.setFullName(getStr(user.get("fullName")));
        selectedUser.setPhone(getStr(user.get("phone")));

        userFormTitle.setText("Sửa user");
        userUsernameField.setText(selectedUser.getUsername());
        userPasswordField.clear();
        userPasswordField.setPromptText("Để trống nếu không đổi mật khẩu");
        userRoleCombo.setValue(selectedUser.getRole());
        userFullNameField.setText(selectedUser.getFullName());
        userPhoneField.setText(selectedUser.getPhone());

        userFormError.setVisible(false);
        userFormError.setManaged(false);
    }

    private void clearUserForm() {
        selectedUser = null;
        userFormTitle.setText("+ Thêm user");
        userUsernameField.clear();
        userPasswordField.clear();
        userPasswordField.setPromptText("Nhập mật khẩu");
        userRoleCombo.setValue("User");
        userFullNameField.clear();
        userPhoneField.clear();
        userFormError.setVisible(false);
        userFormError.setManaged(false);
    }

    private void setupGroupForm() {
        groupTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                populateGroupForm(sel);
                loadGroupMembers(((Number) sel.get("id")).intValue());
            } else {
                clearGroupMembers();
            }
        });
    }

    private void setupGroupMemberTable() {
        TableColumn<Map<String, Object>, String> usernameCol = new TableColumn<>("Username");
        usernameCol.setCellValueFactory(cellData -> {
            Object val = cellData.getValue().get("username");
            return new javafx.beans.property.SimpleStringProperty(val != null ? val.toString() : "");
        });
        usernameCol.setPrefWidth(120);

        TableColumn<Map<String, Object>, String> fullNameCol = new TableColumn<>("Họ tên");
        fullNameCol.setCellValueFactory(cellData -> {
            Object val = cellData.getValue().get("fullName");
            return new javafx.beans.property.SimpleStringProperty(val != null ? val.toString() : "");
        });
        fullNameCol.setPrefWidth(150);

        TableColumn<Map<String, Object>, String> roleCol = new TableColumn<>("Vai trò");
        roleCol.setCellValueFactory(cellData -> {
            Object val = cellData.getValue().get("roleInGroup");
            return new javafx.beans.property.SimpleStringProperty(val != null ? val.toString() : "");
        });
        roleCol.setPrefWidth(100);

        groupMemberTable.getColumns().addAll(usernameCol, fullNameCol, roleCol);
        groupMemberData = FXCollections.observableArrayList();
        groupMemberTable.setItems(groupMemberData);
        groupMemberTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
    }

    private void clearGroupMembers() {
        if (groupMemberData != null) {
            groupMemberData.clear();
        }
    }

    private void loadGroupMembers(int groupId) {
        Task<List<Map<String, Object>>> loadTask = new Task<>() {
            @Override
            protected List<Map<String, Object>> call() {
                Request request = new Request("GET_GROUP_MEMBERS", groupId);
                Response response = connector.sendRequest(request);
                if (response.isSuccess()) {
                    java.lang.reflect.Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
                    return gson.fromJson(gson.toJson(response.getData()), listType);
                }
                return new ArrayList<>();
            }
        };

        loadTask.setOnSucceeded(e -> {
            groupMemberData.clear();
            for (Map<String, Object> member : loadTask.getValue()) {
                groupMemberData.add(new HashMap<>(member));
            }
        });
        loadTask.setOnFailed(e -> showAlert("Lỗi", "Không thể tải thành viên nhóm: " + loadTask.getException().getMessage()));
        new Thread(loadTask).start();
    }

    private void populateGroupForm(Map<String, Object> group) {
        selectedGroup = new GroupDTO();
        selectedGroup.setId(((Number) group.get("id")).intValue());
        selectedGroup.setGroupName(getStr(group.get("groupName")));
        selectedGroup.setDescription(getStr(group.get("description")));

        groupFormTitle.setText("Sửa nhóm");
        groupNameField.setText(selectedGroup.getGroupName());
        groupDescArea.setText(selectedGroup.getDescription());

        groupFormError.setVisible(false);
        groupFormError.setManaged(false);
    }

    private void clearGroupForm() {
        selectedGroup = null;
        groupFormTitle.setText("+ Thêm nhóm");
        groupNameField.clear();
        groupDescArea.clear();
        groupFormError.setVisible(false);
        groupFormError.setManaged(false);
        clearGroupMembers();
    }

    private void loadFormComboData() {
        if (!"Admin".equals(currentUser.getRole())) {
            Platform.runLater(() -> {
                taskGroupCombo.getItems().clear();
                taskGroupCombo.getItems().add("Không (Cá nhân)");
                taskGroupCombo.setValue("Không (Cá nhân)");
                taskUserCombo.getItems().clear();
                String self = currentUser.getId() + " - " + currentUser.getUsername();
                taskUserCombo.getItems().add(self);
                taskUserCombo.setValue(self);
            });
            return;
        }

        Task<List<GroupDTO>> loadGroups = new Task<>() {
            @Override
            protected List<GroupDTO> call() {
                Request request = new Request("GET_ALL_GROUPS", null);
                Response response = connector.sendRequest(request);
                if (response.isSuccess()) {
                    java.lang.reflect.Type listType = new TypeToken<List<GroupDTO>>() {}.getType();
                    return gson.fromJson(gson.toJson(response.getData()), listType);
                }
                return new ArrayList<>();
            }
        };

        Task<List<UserDTO>> loadUsers = new Task<>() {
            @Override
            protected List<UserDTO> call() {
                Request request = new Request("GET_ALL_USERS", null);
                Response response = connector.sendRequest(request);
                if (response.isSuccess()) {
                    java.lang.reflect.Type listType = new TypeToken<List<UserDTO>>() {}.getType();
                    return gson.fromJson(gson.toJson(response.getData()), listType);
                }
                return new ArrayList<>();
            }
        };

        loadGroups.setOnSucceeded(e -> {
            groupList = loadGroups.getValue();
            Platform.runLater(() -> {
                taskGroupCombo.getItems().clear();
                taskGroupCombo.getItems().add("Không (Cá nhân)");
                for (GroupDTO g : groupList) {
                    taskGroupCombo.getItems().add(g.getId() + " - " + g.getGroupName());
                }
                taskGroupCombo.setValue("Không (Cá nhân)");
            });
        });

        loadUsers.setOnSucceeded(e -> {
            userList = loadUsers.getValue();
            Platform.runLater(() -> {
                taskUserCombo.getItems().clear();
                memberUserCombo.getItems().clear();
                for (UserDTO u : userList) {
                    String item = u.getId() + " - " + u.getUsername();
                    taskUserCombo.getItems().add(item);
                    memberUserCombo.getItems().add(item);
                }
                if (!userList.isEmpty()) {
                    taskUserCombo.setValue(taskUserCombo.getItems().get(0));
                    memberUserCombo.setValue(memberUserCombo.getItems().get(0));
                }
            });
        });

        new Thread(loadGroups).start();
        new Thread(loadUsers).start();
    }

    // ==================== STATS ====================

    private void loadDashboardStats() {
        Task<Map<String, Object>> statsTask = new Task<>() {
            @Override
            protected Map<String, Object> call() {
                Request request = new Request("GET_DASHBOARD_STATS", null);
                Response response = connector.sendRequest(request);
                if (response.isSuccess()) {
                    java.lang.reflect.Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
                    return gson.fromJson(gson.toJson(response.getData()), mapType);
                }
                return null;
            }
        };

        statsTask.setOnSucceeded(e -> {
            Map<String, Object> stats = statsTask.getValue();
            if (stats != null) {
                Platform.runLater(() -> buildStatsCards(stats));
            }
        });
        statsTask.setOnFailed(e -> showAlert("Lỗi", "Không thể tải thống kê: " + statsTask.getException().getMessage()));

        new Thread(statsTask).start();
    }

    private void buildStatsCards(Map<String, Object> stats) {
        statsGrid.getChildren().clear();

        long total = getLong(stats.get("totalTasks"));
        long done = getLong(stats.get("doneTasks"));
        long overdue = getLong(stats.get("overdueTasks"));
        long groups = getLong(stats.get("totalGroups"));

        statsGrid.add(createStatCard("📋", "Tổng công việc", String.valueOf(total), "stat-card-total", "#0D9488"), 0, 0);
        statsGrid.add(createStatCard("✅", "Hoàn thành", String.valueOf(done), "stat-card-done", "#10B981"), 1, 0);
        statsGrid.add(createStatCard("⚠️", "Quá hạn", String.valueOf(overdue), "stat-card-overdue", "#EF4444"), 0, 1);
        statsGrid.add(createStatCard("👥", "Nhóm", String.valueOf(groups), "stat-card-project", "#6366F1"), 1, 1);
    }

    private void updateStatusChart(List<Map<String, Object>> tasks) {
        if (tasks == null) return;

        long todo = 0;
        long progress = 0;
        long done = 0;

        for (Map<String, Object> task : tasks) {
            Object statusObj = task.get("status");
            if (statusObj != null) {
                String status = statusObj.toString();
                switch (status) {
                    case "TODO" -> todo++;
                    case "IN_PROGRESS" -> progress++;
                    case "DONE" -> done++;
                }
            }
        }

        long finalTodo = todo;
        long finalProgress = progress;
        long finalDone = done;

        Platform.runLater(() -> {
            taskStatusChart.getData().clear();

            if (finalTodo == 0 && finalProgress == 0 && finalDone == 0) {
                return;
            }

            PieChart.Data todoData = new PieChart.Data("TODO (" + finalTodo + ")", finalTodo);
            PieChart.Data progressData = new PieChart.Data("IN_PROGRESS (" + finalProgress + ")", finalProgress);
            PieChart.Data doneData = new PieChart.Data("DONE (" + finalDone + ")", finalDone);

            taskStatusChart.getData().addAll(todoData, progressData, doneData);

            stylePieSlice(todoData, "#94A3B8");
            stylePieSlice(progressData, "#6366F1");
            stylePieSlice(doneData, "#10B981");
        });
    }

    private void stylePieSlice(PieChart.Data data, String color) {
        if (data.getNode() != null) {
            data.getNode().setStyle("-fx-pie-color: " + color + ";");
        } else {
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    newNode.setStyle("-fx-pie-color: " + color + ";");
                }
            });
        }
    }

    private long getLong(Object val) {
        if (val instanceof Number) return ((Number) val).longValue();
        if (val instanceof String) {
            try { return Long.parseLong((String) val); }
            catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

    private VBox createStatCard(String icon, String label, String value, String styleClass, String accentColor) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("stat-card", styleClass);
        card.setPadding(new Insets(20, 24, 20, 24));

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 28px;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: 700; -fx-text-fill: #0F172A;");

        Label descLabel = new Label(label);
        descLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: #64748B;");

        card.getChildren().addAll(iconLabel, valueLabel, descLabel);
        return card;
    }

    // ==================== TASK TABLE ====================

    private void setupTaskTable() {
        TableColumn<Map<String, Object>, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(cellData -> {
            Object val = cellData.getValue().get("id");
            return new javafx.beans.property.SimpleObjectProperty<>(val instanceof Number ? ((Number) val).intValue() : 0);
        });
        idCol.setPrefWidth(50);

        TableColumn<Map<String, Object>, String> titleCol = new TableColumn<>("Tiêu đề");
        titleCol.setCellValueFactory(cellData -> {
            Object val = cellData.getValue().get("title");
            return new javafx.beans.property.SimpleStringProperty(val != null ? val.toString() : "");
        });
        titleCol.setPrefWidth(180);

        TableColumn<Map<String, Object>, String> groupCol = new TableColumn<>("Nhóm");
        groupCol.setCellValueFactory(cellData -> {
            Object val = cellData.getValue().get("groupName");
            return new javafx.beans.property.SimpleStringProperty(val != null ? val.toString() : "Cá nhân");
        });
        groupCol.setPrefWidth(120);

        TableColumn<Map<String, Object>, String> statusCol = new TableColumn<>("Trạng thái");
        statusCol.setCellValueFactory(cellData -> {
            Object val = cellData.getValue().get("status");
            return new javafx.beans.property.SimpleStringProperty(val != null ? val.toString() : "");
        });
        statusCol.setPrefWidth(110);
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("status-badge");
                    switch (item) {
                        case "TODO" -> badge.getStyleClass().add("status-todo");
                        case "IN_PROGRESS" -> badge.getStyleClass().add("status-progress");
                        case "DONE" -> badge.getStyleClass().add("status-done");
                    }
                    setGraphic(badge);
                }
            }
        });

        TableColumn<Map<String, Object>, String> priorityCol = new TableColumn<>("Ưu tiên");
        priorityCol.setCellValueFactory(cellData -> {
            Object val = cellData.getValue().get("priority");
            return new javafx.beans.property.SimpleStringProperty(val != null ? val.toString() : "");
        });
        priorityCol.setPrefWidth(80);
        priorityCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("status-badge");
                    switch (item) {
                        case "HIGH" -> badge.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-padding: 2 8; -fx-background-radius: 10;");
                        case "MEDIUM" -> badge.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #D97706; -fx-padding: 2 8; -fx-background-radius: 10;");
                        case "LOW" -> badge.setStyle("-fx-background-color: #DBEAFE; -fx-text-fill: #2563EB; -fx-padding: 2 8; -fx-background-radius: 10;");
                    }
                    setGraphic(badge);
                }
            }
        });

        TableColumn<Map<String, Object>, String> dueDateCol = new TableColumn<>("Hạn chót");
        dueDateCol.setCellValueFactory(cellData -> {
            Object val = cellData.getValue().get("dueDate");
            return new javafx.beans.property.SimpleStringProperty(val != null ? val.toString() : "");
        });
        dueDateCol.setPrefWidth(110);

        TableColumn<Map<String, Object>, String> assigneeCol = new TableColumn<>("Người thực hiện");
        assigneeCol.setCellValueFactory(cellData -> {
            Object val = cellData.getValue().get("assigneeName");
            return new javafx.beans.property.SimpleStringProperty(val != null ? val.toString() : "");
        });
        assigneeCol.setPrefWidth(120);

        taskTable.getColumns().addAll(idCol, titleCol, groupCol, statusCol, priorityCol, dueDateCol, assigneeCol);
        taskData = FXCollections.observableArrayList();
        taskTable.setItems(taskData);
        taskTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
    }

    private void loadAllTasks() {
        Task<List<Map<String, Object>>> loadTask = new Task<>() {
            @Override
            protected List<Map<String, Object>> call() {
                Request request;
                Map<String, Object> params = buildTaskQueryParams();
                if ("Admin".equals(currentUser.getRole())) {
                    request = new Request("GET_ALL_TASKS", params);
                } else {
                    request = new Request("GET_TASKS_BY_USER", params);
                }
                Response response = connector.sendRequest(request);
                if (response.isSuccess()) {
                    java.lang.reflect.Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
                    return gson.fromJson(gson.toJson(response.getData()), listType);
                }
                return new ArrayList<>();
            }
        };

        loadTask.setOnSucceeded(e -> {
            taskData.clear();
            List<Map<String, Object>> tasks = loadTask.getValue();
            for (Map<String, Object> task : tasks) {
                taskData.add(new HashMap<>(task));
            }
            updateStatusChart(tasks);
        });
        loadTask.setOnFailed(e -> showAlert("Lỗi", "Không thể tải danh sách công việc: " + loadTask.getException().getMessage()));

        new Thread(loadTask).start();
    }

    @FXML
    private void handleSearchTask() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            loadAllTasks();
            return;
        }

        Task<List<Map<String, Object>>> searchTask = new Task<>() {
            @Override
            protected List<Map<String, Object>> call() {
                Map<String, Object> params = buildSortParams();
                params.put("keyword", keyword);
                Request request = new Request("SEARCH_TASKS", params);
                Response response = connector.sendRequest(request);
                if (response.isSuccess()) {
                    java.lang.reflect.Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
                    return gson.fromJson(gson.toJson(response.getData()), listType);
                }
                return new ArrayList<>();
            }
        };

        searchTask.setOnSucceeded(e -> {
            taskData.clear();
            List<Map<String, Object>> tasks = searchTask.getValue();
            for (Map<String, Object> task : tasks) {
                taskData.add(new HashMap<>(task));
            }
        });
        searchTask.setOnFailed(e -> showAlert("Lỗi", "Không thể tìm kiếm: " + searchTask.getException().getMessage()));

        new Thread(searchTask).start();
    }

    @FXML
    private void handleLoadAllTasks() {
        searchField.clear();
        filterCombo.setValue("Tất cả");
        loadAllTasks();
    }

    private void handleFilterTasks() {
        String selected = filterCombo.getValue();
        if (selected == null || "Tất cả".equals(selected)) {
            loadAllTasks();
            return;
        }

        Task<List<Map<String, Object>>> filterTask = new Task<>() {
            @Override
            protected List<Map<String, Object>> call() {
                Map<String, Object> params = buildSortParams();
                params.put("status", selected);
                Request request = new Request("FILTER_TASKS_BY_STATUS", params);
                Response response = connector.sendRequest(request);
                if (response.isSuccess()) {
                    java.lang.reflect.Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
                    return gson.fromJson(gson.toJson(response.getData()), listType);
                }
                return new ArrayList<>();
            }
        };

        filterTask.setOnSucceeded(e -> {
            taskData.clear();
            List<Map<String, Object>> tasks = filterTask.getValue();
            for (Map<String, Object> task : tasks) {
                taskData.add(new HashMap<>(task));
            }
        });
        filterTask.setOnFailed(e -> showAlert("Lỗi", "Không thể lọc: " + filterTask.getException().getMessage()));

        new Thread(filterTask).start();
    }

    @FXML
    private void handleAddTask() {
        clearTaskForm();
    }

    @FXML
    private void handleSaveTask() {
        String title = taskTitleField.getText().trim();
        if (title.isEmpty()) {
            taskFormError.setText("Vui lòng nhập tiêu đề công việc!");
            taskFormError.setVisible(true);
            taskFormError.setManaged(true);
            return;
        }
        if (taskDueDatePicker.getValue() == null) {
            taskFormError.setText("Vui lòng chọn hạn chót!");
            taskFormError.setVisible(true);
            taskFormError.setManaged(true);
            return;
        }

        taskFormError.setVisible(false);
        taskFormError.setManaged(false);
        taskSaveBtn.setDisable(true);
        taskSaveBtn.setText("Đang lưu...");

        TaskDTO task = new TaskDTO();
        task.setTitle(title);
        task.setDescription(taskDescArea.getText());
        task.setDueDate(taskDueDatePicker.getValue().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        task.setStatus(taskStatusCombo.getValue());
        task.setPriority(taskPriorityCombo.getValue());

        // Group combo
        String groupVal = taskGroupCombo.getValue();
        if (groupVal != null && groupVal.contains(" - ")) {
            task.setGroupId(Integer.parseInt(groupVal.split(" - ")[0]));
        }
        // User combo
        String userVal = taskUserCombo.getValue();
        if (userVal != null && userVal.contains(" - ")) {
            task.setAssigneeId(Integer.parseInt(userVal.split(" - ")[0]));
        }

        String action;
        if (selectedTask != null) {
            task.setId(selectedTask.getId());
            task.setCreatorId(selectedTask.getCreatorId());
            action = "UPDATE_TASK";
        } else {
            task.setCreatorId(currentUser.getId());
            action = "ADD_TASK";
        }

        String finalAction = action;
        Task<Response> saveTask = new Task<>() {
            @Override
            protected Response call() {
                Request request = new Request(finalAction, task);
                return connector.sendRequest(request);
            }
        };

        saveTask.setOnSucceeded(e -> {
            Response response = saveTask.getValue();
            if (response.isSuccess()) {
                loadAllTasks();
                loadDashboardStats();
                clearTaskForm();
            } else {
                taskFormError.setText(response.getMessage());
                taskFormError.setVisible(true);
                taskFormError.setManaged(true);
            }
            taskSaveBtn.setDisable(false);
            taskSaveBtn.setText("Lưu");
        });

        saveTask.setOnFailed(e -> {
            taskFormError.setText("Lỗi: " + saveTask.getException().getMessage());
            taskFormError.setVisible(true);
            taskFormError.setManaged(true);
            taskSaveBtn.setDisable(false);
            taskSaveBtn.setText("Lưu");
        });

        new Thread(saveTask).start();
    }

    @FXML
    private void handleCancelTask() {
        clearTaskForm();
    }

    @FXML
    private void handleDeleteTask() {
        Map<String, Object> selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Thông báo", "Vui lòng chọn một công việc!");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Bạn có chắc muốn xóa công việc này?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText(null);
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.YES) {
            Object idVal = selected.get("id");
            if (idVal == null) {
                showAlert("Lỗi", "Không tìm thấy ID công việc!");
                return;
            }
            int taskId = ((Number) idVal).intValue();

            Task<Response> deleteTask = new Task<>() {
                @Override
                protected Response call() {
                    Request request = new Request("DELETE_TASK", taskId);
                    return connector.sendRequest(request);
                }
            };

            deleteTask.setOnSucceeded(e -> {
                deleteTaskBtn.setDisable(false);
                Response response = deleteTask.getValue();
                showAlert("Kết quả", response.getMessage());
                if (response.isSuccess()) {
                    loadAllTasks();
                    loadDashboardStats();
                }
            });
            deleteTask.setOnFailed(e -> {
                deleteTaskBtn.setDisable(false);
                showAlert("Lỗi", "Không thể xóa công việc: " + deleteTask.getException().getMessage());
            });

            deleteTaskBtn.setDisable(true);
            new Thread(deleteTask).start();
        }
    }

    @FXML
    private void handleExportCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Xuất file CSV");
        fileChooser.setInitialFileName("tasks_export.csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));

        File file = fileChooser.showSaveDialog(taskTable.getScene().getWindow());
        if (file != null) {
            List<TaskDTO> tasks = new ArrayList<>();
            for (Map<String, Object> row : taskData) {
                TaskDTO t = new TaskDTO();
                t.setId(((Number) row.get("id")).intValue());
                t.setTitle(getStr(row.get("title")));
                t.setGroupName(getStr(row.get("groupName")));
                t.setStatus(getStr(row.get("status")));
                t.setPriority(getStr(row.get("priority")));
                t.setDueDate(getStr(row.get("dueDate")));
                t.setAssigneeName(getStr(row.get("assigneeName")));
                t.setDescription(getStr(row.get("description")));
                tasks.add(t);
            }
            try {
                CSVUtil.exportTasksToCSV(tasks, file.getAbsolutePath());
                showAlert("Thành công", "Xuất file thành công: " + file.getName());
            } catch (Exception e) {
                showAlert("Lỗi", "Lỗi xuất file: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleImportCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Nhập file CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));

        File file = fileChooser.showOpenDialog(taskTable.getScene().getWindow());
        if (file != null) {
            try {
                List<TaskDTO> tasks = CSVUtil.importTasksFromCSV(file.getAbsolutePath());

                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Import " + tasks.size() + " công việc vào database?\nNếu có lỗi, toàn bộ sẽ không được lưu.",
                        ButtonType.YES, ButtonType.NO);
                confirm.setTitle("Xác nhận import");
                confirm.setHeaderText(null);
                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isEmpty() || result.get() != ButtonType.YES) {
                    return;
                }

                Task<Response> importTask = new Task<>() {
                    @Override
                    protected Response call() {
                        Request request = new Request("IMPORT_TASKS", tasks);
                        return connector.sendRequest(request);
                    }
                };

                importTask.setOnSucceeded(e -> {
                    Response response = importTask.getValue();
                    if (response.isSuccess()) {
                        showAlert("Thành công", response.getMessage());
                        loadAllTasks();
                        loadDashboardStats();
                    } else {
                        showAlert("Lỗi", response.getMessage());
                    }
                });
                importTask.setOnFailed(e -> showAlert("Lỗi", "Import thất bại: " + importTask.getException().getMessage()));
                new Thread(importTask).start();
            } catch (Exception e) {
                showAlert("Lỗi", "Lỗi nhập file: " + e.getMessage());
            }
        }
    }

    private String getStr(Object val) {
        return val != null ? val.toString() : "";
    }

    // ==================== USER TABLE ====================

    private void setupUserTable() {
        TableColumn<Map<String, Object>, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(cellData -> {
            Object val = cellData.getValue().get("id");
            return new javafx.beans.property.SimpleObjectProperty<>(val instanceof Number ? ((Number) val).intValue() : 0);
        });
        idCol.setPrefWidth(60);

        TableColumn<Map<String, Object>, String> usernameCol = new TableColumn<>("Username");
        usernameCol.setCellValueFactory(cellData -> {
            Object val = cellData.getValue().get("username");
            return new javafx.beans.property.SimpleStringProperty(val != null ? val.toString() : "");
        });
        usernameCol.setPrefWidth(120);

        TableColumn<Map<String, Object>, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(cellData -> {
            Object val = cellData.getValue().get("role");
            return new javafx.beans.property.SimpleStringProperty(val != null ? val.toString() : "");
        });
        roleCol.setPrefWidth(80);

        TableColumn<Map<String, Object>, String> fullNameCol = new TableColumn<>("Họ tên");
        fullNameCol.setCellValueFactory(cellData -> {
            Object val = cellData.getValue().get("fullName");
            return new javafx.beans.property.SimpleStringProperty(val != null ? val.toString() : "");
        });
        fullNameCol.setPrefWidth(150);

        TableColumn<Map<String, Object>, String> statusCol = new TableColumn<>("Trạng thái");
        statusCol.setCellValueFactory(cellData -> {
            Object val = cellData.getValue().get("status");
            return new javafx.beans.property.SimpleStringProperty(val != null ? val.toString() : "ACTIVE");
        });
        statusCol.setPrefWidth(100);

        userTable.getColumns().addAll(idCol, usernameCol, roleCol, fullNameCol, statusCol);
        userData = FXCollections.observableArrayList();
        userTable.setItems(userData);
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
    }

    private void loadUsers() {
        Task<List<Map<String, Object>>> loadTask = new Task<>() {
            @Override
            protected List<Map<String, Object>> call() {
                Request request = new Request("GET_ALL_USERS", null);
                Response response = connector.sendRequest(request);
                if (response.isSuccess()) {
                    java.lang.reflect.Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
                    return gson.fromJson(gson.toJson(response.getData()), listType);
                }
                return new ArrayList<>();
            }
        };

        loadTask.setOnSucceeded(e -> {
            userData.clear();
            List<Map<String, Object>> users = loadTask.getValue();
            for (Map<String, Object> user : users) {
                userData.add(new HashMap<>(user));
            }
        });
        loadTask.setOnFailed(e -> showAlert("Lỗi", "Không thể tải danh sách người dùng: " + loadTask.getException().getMessage()));

        new Thread(loadTask).start();
    }

    @FXML
    private void handleAddUser() {
        clearUserForm();
    }

    @FXML
    private void handleSaveUser() {
        String username = userUsernameField.getText().trim();
        if (username.isEmpty()) {
            userFormError.setText("Vui lòng nhập tên đăng nhập!");
            userFormError.setVisible(true);
            userFormError.setManaged(true);
            return;
        }
        if (selectedUser == null && userPasswordField.getText().isEmpty()) {
            userFormError.setText("Vui lòng nhập mật khẩu!");
            userFormError.setVisible(true);
            userFormError.setManaged(true);
            return;
        }
        String phone = userPhoneField.getText().trim();
        if (!phone.isEmpty() && !phone.matches("^0[0-9]{9,10}$")) {
            userFormError.setText("Số điện thoại không hợp lệ! Định dạng: 0xxxxxxxxx");
            userFormError.setVisible(true);
            userFormError.setManaged(true);
            return;
        }
        String password = userPasswordField.getText();
        if (!password.isEmpty() && password.length() < 6) {
            userFormError.setText("Mật khẩu phải có ít nhất 6 ký tự!");
            userFormError.setVisible(true);
            userFormError.setManaged(true);
            return;
        }

        userFormError.setVisible(false);
        userFormError.setManaged(false);
        userSaveBtn.setDisable(true);
        userSaveBtn.setText("Đang lưu...");

        UserDTO user = new UserDTO();
        user.setUsername(username);
        user.setRole(userRoleCombo.getValue());
        user.setFullName(userFullNameField.getText().trim());
        user.setPhone(phone);

        if (!password.isEmpty()) user.setPassword(password);

        String action;
        if (selectedUser != null) {
            user.setId(selectedUser.getId());
            action = "UPDATE_USER";
        } else {
            action = "ADD_USER";
        }

        String finalAction = action;
        Task<Response> saveTask = new Task<>() {
            @Override
            protected Response call() {
                Request request = new Request(finalAction, user);
                return connector.sendRequest(request);
            }
        };

        saveTask.setOnSucceeded(e -> {
            Response response = saveTask.getValue();
            if (response.isSuccess()) {
                loadUsers();
                clearUserForm();
            } else {
                userFormError.setText(response.getMessage());
                userFormError.setVisible(true);
                userFormError.setManaged(true);
            }
            userSaveBtn.setDisable(false);
            userSaveBtn.setText("Lưu");
        });

        saveTask.setOnFailed(e -> {
            userFormError.setText("Lỗi: " + saveTask.getException().getMessage());
            userFormError.setVisible(true);
            userFormError.setManaged(true);
            userSaveBtn.setDisable(false);
            userSaveBtn.setText("Lưu");
        });

        new Thread(saveTask).start();
    }

    @FXML
    private void handleCancelUser() {
        clearUserForm();
    }

    @FXML
    private void handleToggleUserStatus() {
        Map<String, Object> selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Thông báo", "Vui lòng chọn một user!");
            return;
        }

        Object idVal = selected.get("id");
        if (idVal == null) {
            showAlert("Lỗi", "Không tìm thấy ID user!");
            return;
        }
        int userId = ((Number) idVal).intValue();
        Task<Response> toggleTask = new Task<>() {
            @Override
            protected Response call() {
                Request request = new Request("TOGGLE_USER_STATUS", userId);
                return connector.sendRequest(request);
            }
        };

        toggleTask.setOnSucceeded(e -> {
            Response response = toggleTask.getValue();
            showAlert("Kết quả", response.getMessage());
            if (response.isSuccess()) loadUsers();
        });
        toggleTask.setOnFailed(e -> showAlert("Lỗi", "Không thể thay đổi trạng thái: " + toggleTask.getException().getMessage()));

        new Thread(toggleTask).start();
    }

    @FXML
    private void handleDeleteUser() {
        Map<String, Object> selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Thông báo", "Vui lòng chọn một user!");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Bạn có chắc muốn xóa user này?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText(null);
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.YES) {
            Object idVal = selected.get("id");
            if (idVal == null) {
                showAlert("Lỗi", "Không tìm thấy ID user!");
                return;
            }
            int userId = ((Number) idVal).intValue();
            Task<Response> deleteTask = new Task<>() {
                @Override
                protected Response call() {
                    Request request = new Request("DELETE_USER", userId);
                    return connector.sendRequest(request);
                }
            };

            deleteTask.setOnSucceeded(e -> {
                deleteUserBtn.setDisable(false);
                Response response = deleteTask.getValue();
                showAlert("Kết quả", response.getMessage());
                if (response.isSuccess()) loadUsers();
            });
            deleteTask.setOnFailed(e -> {
                deleteUserBtn.setDisable(false);
                showAlert("Lỗi", "Không thể xóa người dùng: " + deleteTask.getException().getMessage());
            });

            deleteUserBtn.setDisable(true);
            new Thread(deleteTask).start();
        }
    }

    @FXML
    private void handleRefreshUsers() {
        loadUsers();
    }

    // ==================== GROUP TABLE ====================

    private void setupGroupTable() {
        TableColumn<Map<String, Object>, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(cellData -> {
            Object val = cellData.getValue().get("id");
            return new javafx.beans.property.SimpleObjectProperty<>(val instanceof Number ? ((Number) val).intValue() : 0);
        });
        idCol.setPrefWidth(60);

        TableColumn<Map<String, Object>, String> nameCol = new TableColumn<>("Tên nhóm");
        nameCol.setCellValueFactory(cellData -> {
            Object val = cellData.getValue().get("groupName");
            return new javafx.beans.property.SimpleStringProperty(val != null ? val.toString() : "");
        });
        nameCol.setPrefWidth(200);

        TableColumn<Map<String, Object>, String> descCol = new TableColumn<>("Mô tả");
        descCol.setCellValueFactory(cellData -> {
            Object val = cellData.getValue().get("description");
            return new javafx.beans.property.SimpleStringProperty(val != null ? val.toString() : "");
        });
        descCol.setPrefWidth(250);

        TableColumn<Map<String, Object>, String> creatorCol = new TableColumn<>("Người tạo");
        creatorCol.setCellValueFactory(cellData -> {
            Object val = cellData.getValue().get("createdByName");
            return new javafx.beans.property.SimpleStringProperty(val != null ? val.toString() : "");
        });
        creatorCol.setPrefWidth(150);

        groupTable.getColumns().addAll(idCol, nameCol, descCol, creatorCol);
        groupData = FXCollections.observableArrayList();
        groupTable.setItems(groupData);
        groupTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
    }

    private void loadGroups() {
        Task<List<Map<String, Object>>> loadTask = new Task<>() {
            @Override
            protected List<Map<String, Object>> call() {
                Request request = new Request("GET_ALL_GROUPS", null);
                Response response = connector.sendRequest(request);
                if (response.isSuccess()) {
                    java.lang.reflect.Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
                    return gson.fromJson(gson.toJson(response.getData()), listType);
                }
                return new ArrayList<>();
            }
        };

        loadTask.setOnSucceeded(e -> {
            groupData.clear();
            List<Map<String, Object>> groups = loadTask.getValue();
            for (Map<String, Object> group : groups) {
                groupData.add(new HashMap<>(group));
            }
        });
        loadTask.setOnFailed(e -> showAlert("Lỗi", "Không thể tải danh sách nhóm: " + loadTask.getException().getMessage()));

        new Thread(loadTask).start();
    }

    @FXML
    private void handleAddGroup() {
        clearGroupForm();
    }

    @FXML
    private void handleSaveGroup() {
        String name = groupNameField.getText().trim();
        if (name.isEmpty()) {
            groupFormError.setText("Vui lòng nhập tên nhóm!");
            groupFormError.setVisible(true);
            groupFormError.setManaged(true);
            return;
        }

        groupFormError.setVisible(false);
        groupFormError.setManaged(false);
        groupSaveBtn.setDisable(true);
        groupSaveBtn.setText("Đang lưu...");

        GroupDTO group = new GroupDTO();
        group.setGroupName(name);
        group.setDescription(groupDescArea.getText());

        String action;
        if (selectedGroup != null) {
            group.setId(selectedGroup.getId());
            action = "UPDATE_GROUP";
        } else {
            group.setCreatedBy(currentUser.getId());
            action = "ADD_GROUP";
        }

        String finalAction = action;
        Task<Response> saveTask = new Task<>() {
            @Override
            protected Response call() {
                Request request = new Request(finalAction, group);
                return connector.sendRequest(request);
            }
        };

        saveTask.setOnSucceeded(e -> {
            Response response = saveTask.getValue();
            if (response.isSuccess()) {
                loadGroups();
                loadDashboardStats();
                clearGroupForm();
            } else {
                groupFormError.setText(response.getMessage());
                groupFormError.setVisible(true);
                groupFormError.setManaged(true);
            }
            groupSaveBtn.setDisable(false);
            groupSaveBtn.setText("Lưu");
        });

        saveTask.setOnFailed(e -> {
            groupFormError.setText("Lỗi: " + saveTask.getException().getMessage());
            groupFormError.setVisible(true);
            groupFormError.setManaged(true);
            groupSaveBtn.setDisable(false);
            groupSaveBtn.setText("Lưu");
        });

        new Thread(saveTask).start();
    }

    @FXML
    private void handleCancelGroup() {
        clearGroupForm();
    }

    @FXML
    private void handleDeleteGroup() {
        Map<String, Object> selected = groupTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Thông báo", "Vui lòng chọn một nhóm!");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Bạn có chắc muốn xóa nhóm này?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText(null);
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.YES) {
            Object idVal = selected.get("id");
            if (idVal == null) {
                showAlert("Lỗi", "Không tìm thấy ID nhóm!");
                return;
            }
            int groupId = ((Number) idVal).intValue();
            Task<Response> deleteTask = new Task<>() {
                @Override
                protected Response call() {
                    Request request = new Request("DELETE_GROUP", groupId);
                    return connector.sendRequest(request);
                }
            };

            deleteTask.setOnSucceeded(e -> {
                deleteGroupBtn.setDisable(false);
                Response response = deleteTask.getValue();
                showAlert("Kết quả", response.getMessage());
                if (response.isSuccess()) {
                    loadGroups();
                    loadDashboardStats();
                }
            });
            deleteTask.setOnFailed(e -> {
                deleteGroupBtn.setDisable(false);
                showAlert("Lỗi", "Không thể xóa nhóm: " + deleteTask.getException().getMessage());
            });

            deleteGroupBtn.setDisable(true);
            new Thread(deleteTask).start();
        }
    }

    @FXML
    private void handleRefreshGroups() {
        loadGroups();
    }

    @FXML
    private void handleAddGroupMember() {
        if (selectedGroup == null) {
            showAlert("Thông báo", "Vui lòng chọn một nhóm!");
            return;
        }
        String userVal = memberUserCombo.getValue();
        if (userVal == null || !userVal.contains(" - ")) {
            showAlert("Thông báo", "Vui lòng chọn user để thêm!");
            return;
        }
        int userId = Integer.parseInt(userVal.split(" - ")[0]);
        Map<String, Object> data = new HashMap<>();
        data.put("groupId", selectedGroup.getId());
        data.put("userId", userId);
        data.put("roleInGroup", "MEMBER");

        Task<Response> addTask = new Task<>() {
            @Override
            protected Response call() {
                return connector.sendRequest(new Request("ADD_GROUP_MEMBER", data));
            }
        };
        addTask.setOnSucceeded(e -> {
            Response response = addTask.getValue();
            showAlert("Kết quả", response.getMessage());
            if (response.isSuccess()) {
                loadGroupMembers(selectedGroup.getId());
            }
        });
        addTask.setOnFailed(e -> showAlert("Lỗi", "Không thể thêm thành viên: " + addTask.getException().getMessage()));
        new Thread(addTask).start();
    }

    @FXML
    private void handleRemoveGroupMember() {
        if (selectedGroup == null) {
            showAlert("Thông báo", "Vui lòng chọn một nhóm!");
            return;
        }
        Map<String, Object> selected = groupMemberTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Thông báo", "Vui lòng chọn thành viên cần xóa!");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Xóa thành viên khỏi nhóm?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Xác nhận");
        confirm.setHeaderText(null);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.YES) {
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("groupId", selectedGroup.getId());
        data.put("userId", ((Number) selected.get("userId")).intValue());

        Task<Response> removeTask = new Task<>() {
            @Override
            protected Response call() {
                return connector.sendRequest(new Request("REMOVE_GROUP_MEMBER", data));
            }
        };
        removeTask.setOnSucceeded(e -> {
            Response response = removeTask.getValue();
            showAlert("Kết quả", response.getMessage());
            if (response.isSuccess()) {
                loadGroupMembers(selectedGroup.getId());
            }
        });
        removeTask.setOnFailed(e -> showAlert("Lỗi", "Không thể xóa thành viên: " + removeTask.getException().getMessage()));
        new Thread(removeTask).start();
    }

    @FXML
    private void handleLogout() {
        if (ServerConnector.hasSession()) {
            connector.sendRequest(new Request("LOGOUT", null));
            ServerConnector.clearSessionToken();
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/ui/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) userLabel.getScene().getWindow();
            Scene scene = new Scene(root, 480, 600);
            stage.setScene(scene);
            stage.setTitle("Đăng nhập - Quản Lý Công Việc");
            stage.setMinWidth(400);
            stage.setMinHeight(500);
            stage.centerOnScreen();
        } catch (Exception e) {
            showAlert("Lỗi", "Không thể đăng xuất: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== HELPERS ====================

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
