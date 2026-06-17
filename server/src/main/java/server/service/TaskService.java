package server.service;

import common.model.*;
import server.dao.*;
import server.util.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskService {
    private final UserDAO userDAO = new UserDAO();
    private final GroupDAO groupDAO = new GroupDAO();
    private final TaskDAO taskDAO = new TaskDAO();
    private final SystemLogDAO systemLogDAO = new SystemLogDAO();
    private final Gson gson = new Gson();

    public Response handleRequest(Request request) {
        String action = request.getAction();
        Object data = request.getData();

        if (!"LOGIN".equals(action)) {
            Response authError = AuthorizationHelper.check(request);
            if (authError != null) {
                return authError;
            }
        }

        switch (action) {
            case "LOGIN":
                return handleLogin(data);
            case "LOGOUT":
                return handleLogout(request.getToken());
            case "GET_ALL_USERS":
                return handleGetAllUsers();
            case "ADD_USER":
                return handleAddUser(data, request);
            case "UPDATE_USER":
                return handleUpdateUser(data, request);
            case "TOGGLE_USER_STATUS":
                return handleToggleUserStatus(data, request);
            case "DELETE_USER":
                return handleDeleteUser(data, request);
            case "GET_ALL_GROUPS":
                return handleGetAllGroups();
            case "ADD_GROUP":
                return handleAddGroup(data, request);
            case "UPDATE_GROUP":
                return handleUpdateGroup(data, request);
            case "DELETE_GROUP":
                return handleDeleteGroup(data, request);
            case "GET_GROUP_MEMBERS":
                return handleGetGroupMembers(data);
            case "ADD_GROUP_MEMBER":
                return handleAddGroupMember(data, request);
            case "REMOVE_GROUP_MEMBER":
                return handleRemoveGroupMember(data, request);
            case "GET_ALL_TASKS":
                return handleGetAllTasks(data);
            case "GET_TASKS_BY_USER":
                return handleGetTasksByUser(data, request);
            case "ADD_TASK":
                return handleAddTask(data, request);
            case "UPDATE_TASK":
                return handleUpdateTask(data, request);
            case "UPDATE_TASK_STATUS":
                return handleUpdateTaskStatus(data, request);
            case "DELETE_TASK":
                return handleDeleteTask(data, request);
            case "SEARCH_TASKS":
                return handleSearchTasks(data, request);
            case "FILTER_TASKS_BY_STATUS":
                return handleFilterTasksByStatus(data, request);
            case "GET_OVERDUE_TASKS":
                return handleGetOverdueTasks(request);
            case "GET_DASHBOARD_STATS":
                return handleGetDashboardStats(request);
            case "GET_SYSTEM_LOGS":
                return handleGetSystemLogs();
            case "IMPORT_TASKS":
                return handleImportTasks(data, request);
            default:
                return new Response(false, "Hanh dong khong hop le!", null);
        }
    }

    // ==================== AUTH ====================

    private Response handleLogin(Object data) {
        @SuppressWarnings("unchecked")
        Map<String, String> loginData = (Map<String, String>) data;
        String username = loginData.get("username");
        String password = loginData.get("password");

        String lockMessage = LoginLockoutManager.checkLocked(username);
        if (lockMessage != null) {
            Logger.log(username, "LOGIN", "LOCKED");
            return new Response(false, lockMessage, null);
        }

        UserDTO user = userDAO.login(username, password);
        if (user != null) {
            LoginLockoutManager.recordSuccess(username);
            String token = SessionManager.createSession(user);
            Map<String, Object> result = new HashMap<>();
            result.put("user", user);
            result.put("token", token);
            Logger.log(username, "LOGIN", "SUCCESS");
            return new Response(true, "Dang nhap thanh cong!", result);
        }

        String failMessage = LoginLockoutManager.recordFailure(username);
        Logger.log(username, "LOGIN", "FAILED");
        return new Response(false, failMessage, null);
    }

    private Response handleLogout(String token) {
        UserDTO user = SessionManager.getUser(token);
        if (user != null) {
            Logger.log(user.getUsername(), "LOGOUT", "SUCCESS");
        }
        SessionManager.removeSession(token);
        return new Response(true, "Dang xuat thanh cong!", null);
    }

    private UserDTO currentUser(Request request) {
        return SessionManager.getUser(request.getToken());
    }

    // ==================== USERS ====================

    private Response handleGetAllUsers() {
        return new Response(true, "Lay danh sach nguoi dung thanh cong!", userDAO.getAllUsers());
    }

    private Response handleAddUser(Object data, Request request) {
        UserDTO user = gson.fromJson(gson.toJson(data), UserDTO.class);
        String validationError = ValidationUtil.validateUser(user, true);
        if (validationError != null) {
            return new Response(false, validationError, null);
        }
        if (userDAO.addUser(user)) {
            Logger.log(currentUser(request).getUsername(), "ADD_USER", "SUCCESS");
            return new Response(true, "Them nguoi dung thanh cong!", null);
        }
        return new Response(false, "Them nguoi dung that bai! Username co the da ton tai.", null);
    }

    private Response handleUpdateUser(Object data, Request request) {
        UserDTO user = gson.fromJson(gson.toJson(data), UserDTO.class);
        String validationError = ValidationUtil.validateUser(user, false);
        if (validationError != null) {
            return new Response(false, validationError, null);
        }
        if (userDAO.updateUser(user)) {
            String password = user.getPassword();
            if (password != null && !password.isEmpty()) {
                userDAO.updateUserPassword(user.getId(), password);
            }
            Logger.log(currentUser(request).getUsername(), "UPDATE_USER", "SUCCESS");
            return new Response(true, "Cap nhat nguoi dung thanh cong!", null);
        }
        return new Response(false, "Cap nhat nguoi dung that bai!", null);
    }

    private Response handleToggleUserStatus(Object data, Request request) {
        int userId = ((Number) data).intValue();
        if (userDAO.toggleUserStatus(userId)) {
            Logger.log(currentUser(request).getUsername(), "TOGGLE_USER_STATUS", "SUCCESS");
            return new Response(true, "Cap nhat trang thai thanh cong!", null);
        }
        return new Response(false, "Cap nhat trang thai that bai!", null);
    }

    private Response handleDeleteUser(Object data, Request request) {
        int userId = ((Number) data).intValue();
        if (userDAO.deleteUser(userId)) {
            Logger.log(currentUser(request).getUsername(), "DELETE_USER", "SUCCESS");
            return new Response(true, "Xoa nguoi dung thanh cong!", null);
        }
        return new Response(false, "Xoa nguoi dung that bai!", null);
    }

    // ==================== GROUPS ====================

    private Response handleGetAllGroups() {
        return new Response(true, "Lay danh sach nhom thanh cong!", groupDAO.getAllGroups());
    }

    private Response handleAddGroup(Object data, Request request) {
        GroupDTO group = gson.fromJson(gson.toJson(data), GroupDTO.class);
        if (group.getGroupName() == null || group.getGroupName().trim().isEmpty()) {
            return new Response(false, "Ten nhom khong duoc de trong!", null);
        }
        if (groupDAO.addGroup(group)) {
            Logger.log(currentUser(request).getUsername(), "ADD_GROUP", "SUCCESS");
            return new Response(true, "Them nhom thanh cong!", null);
        }
        return new Response(false, "Them nhom that bai!", null);
    }

    private Response handleUpdateGroup(Object data, Request request) {
        GroupDTO group = gson.fromJson(gson.toJson(data), GroupDTO.class);
        if (group.getGroupName() == null || group.getGroupName().trim().isEmpty()) {
            return new Response(false, "Ten nhom khong duoc de trong!", null);
        }
        if (groupDAO.updateGroup(group)) {
            Logger.log(currentUser(request).getUsername(), "UPDATE_GROUP", "SUCCESS");
            return new Response(true, "Cap nhat nhom thanh cong!", null);
        }
        return new Response(false, "Cap nhat nhom that bai!", null);
    }

    private Response handleDeleteGroup(Object data, Request request) {
        int groupId = ((Number) data).intValue();
        if (groupDAO.deleteGroup(groupId)) {
            Logger.log(currentUser(request).getUsername(), "DELETE_GROUP", "SUCCESS");
            return new Response(true, "Xoa nhom thanh cong!", null);
        }
        return new Response(false, "Xoa nhom that bai!", null);
    }

    private Response handleGetGroupMembers(Object data) {
        int groupId = ((Number) data).intValue();
        return new Response(true, "Lay danh sach thanh vien thanh cong!", groupDAO.getGroupMembers(groupId));
    }

    @SuppressWarnings("unchecked")
    private Response handleAddGroupMember(Object data, Request request) {
        Map<String, Object> memberData = (Map<String, Object>) data;
        int groupId = ((Number) memberData.get("groupId")).intValue();
        int userId = ((Number) memberData.get("userId")).intValue();
        String role = memberData.get("roleInGroup") != null ? (String) memberData.get("roleInGroup") : "MEMBER";
        if (groupDAO.addGroupMember(groupId, userId, role)) {
            Logger.log(currentUser(request).getUsername(), "ADD_GROUP_MEMBER", "SUCCESS");
            return new Response(true, "Them thanh vien thanh cong!", null);
        }
        return new Response(false, "Them thanh vien that bai! Co the thanh vien da ton tai.", null);
    }

    @SuppressWarnings("unchecked")
    private Response handleRemoveGroupMember(Object data, Request request) {
        Map<String, Object> memberData = (Map<String, Object>) data;
        int groupId = ((Number) memberData.get("groupId")).intValue();
        int userId = ((Number) memberData.get("userId")).intValue();
        if (groupDAO.removeGroupMember(groupId, userId)) {
            Logger.log(currentUser(request).getUsername(), "REMOVE_GROUP_MEMBER", "SUCCESS");
            return new Response(true, "Xoa thanh vien thanh cong!", null);
        }
        return new Response(false, "Xoa thanh vien that bai!", null);
    }

    // ==================== TASKS ====================

    @SuppressWarnings("unchecked")
    private Map<String, String> parseSortParams(Object data) {
        Map<String, String> sort = new HashMap<>();
        sort.put("sortBy", "id");
        sort.put("sortOrder", "ASC");
        if (data instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) data;
            if (map.get("sortBy") != null) {
                sort.put("sortBy", map.get("sortBy").toString());
            }
            if (map.get("sortOrder") != null) {
                sort.put("sortOrder", map.get("sortOrder").toString());
            }
        }
        return sort;
    }

    private Response handleGetAllTasks(Object data) {
        Map<String, String> sort = parseSortParams(data);
        return new Response(true, "Lay danh sach cong viec thanh cong!",
                taskDAO.getAllTasks(sort.get("sortBy"), sort.get("sortOrder")));
    }

    private Response handleGetTasksByUser(Object data, Request request) {
        UserDTO user = currentUser(request);
        int userId;
        if (data instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) data;
            userId = ((Number) map.get("userId")).intValue();
            if (!AuthorizationHelper.isAdmin(user) && userId != user.getId()) {
                return new Response(false, "Ban chi duoc xem cong viec cua minh!", null);
            }
            Map<String, String> sort = parseSortParams(data);
            return new Response(true, "Lay danh sach cong viec thanh cong!",
                    taskDAO.getTasksByUserId(userId, sort.get("sortBy"), sort.get("sortOrder")));
        }
        userId = ((Number) data).intValue();
        if (!AuthorizationHelper.isAdmin(user) && userId != user.getId()) {
            return new Response(false, "Ban chi duoc xem cong viec cua minh!", null);
        }
        return new Response(true, "Lay danh sach cong viec thanh cong!",
                taskDAO.getTasksByUserId(userId, "id", "ASC"));
    }

    private Response handleAddTask(Object data, Request request) {
        UserDTO user = currentUser(request);
        TaskDTO task = gson.fromJson(gson.toJson(data), TaskDTO.class);
        String validationError = ValidationUtil.validateTask(task);
        if (validationError != null) {
            return new Response(false, validationError, null);
        }
        if (!AuthorizationHelper.isAdmin(user)) {
            task.setCreatorId(user.getId());
        }
        if (taskDAO.addTask(task)) {
            Logger.log(user.getUsername(), "ADD_TASK", "SUCCESS");
            return new Response(true, "Them cong viec thanh cong!", null);
        }
        return new Response(false, "Them cong viec that bai!", null);
    }

    private Response handleUpdateTask(Object data, Request request) {
        UserDTO user = currentUser(request);
        TaskDTO task = gson.fromJson(gson.toJson(data), TaskDTO.class);
        String validationError = ValidationUtil.validateTask(task);
        if (validationError != null) {
            return new Response(false, validationError, null);
        }
        if (!AuthorizationHelper.isAdmin(user) && !taskDAO.canUserModifyTask(task.getId(), user.getId())) {
            return new Response(false, "Ban khong co quyen sua cong viec nay!", null);
        }
        if (taskDAO.updateTask(task)) {
            Logger.log(user.getUsername(), "UPDATE_TASK", "SUCCESS");
            return new Response(true, "Cap nhat cong viec thanh cong!", null);
        }
        return new Response(false, "Cap nhat cong viec that bai!", null);
    }

    @SuppressWarnings("unchecked")
    private Response handleUpdateTaskStatus(Object data, Request request) {
        UserDTO user = currentUser(request);
        Map<String, Object> statusData = (Map<String, Object>) data;
        int taskId = ((Number) statusData.get("taskId")).intValue();
        String status = (String) statusData.get("status");
        if (!AuthorizationHelper.isAdmin(user) && !taskDAO.canUserModifyTask(taskId, user.getId())) {
            return new Response(false, "Ban khong co quyen cap nhat trang thai cong viec nay!", null);
        }
        if (taskDAO.updateTaskStatus(taskId, status)) {
            Logger.log(user.getUsername(), "UPDATE_TASK_STATUS", "SUCCESS");
            return new Response(true, "Cap nhat trang thai thanh cong!", null);
        }
        return new Response(false, "Cap nhat trang thai that bai!", null);
    }

    private Response handleDeleteTask(Object data, Request request) {
        int taskId = ((Number) data).intValue();
        if (taskDAO.deleteTask(taskId)) {
            Logger.log(currentUser(request).getUsername(), "DELETE_TASK", "SUCCESS");
            return new Response(true, "Xoa cong viec thanh cong!", null);
        }
        return new Response(false, "Xoa cong viec that bai!", null);
    }

    @SuppressWarnings("unchecked")
    private Response handleSearchTasks(Object data, Request request) {
        UserDTO user = currentUser(request);
        String keyword;
        String sortBy = "title";
        String sortOrder = "ASC";
        Integer userScope = AuthorizationHelper.isAdmin(user) ? null : user.getId();

        if (data instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) data;
            keyword = (String) map.get("keyword");
            if (map.get("sortBy") != null) sortBy = map.get("sortBy").toString();
            if (map.get("sortOrder") != null) sortOrder = map.get("sortOrder").toString();
        } else {
            keyword = (String) data;
        }

        return new Response(true, "Tim kiem thanh cong!",
                taskDAO.searchTasksByTitle(keyword, userScope, sortBy, sortOrder));
    }

    @SuppressWarnings("unchecked")
    private Response handleFilterTasksByStatus(Object data, Request request) {
        UserDTO user = currentUser(request);
        String status;
        String sortBy = "due_date";
        String sortOrder = "ASC";
        Integer userScope = AuthorizationHelper.isAdmin(user) ? null : user.getId();

        if (data instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) data;
            status = (String) map.get("status");
            if (map.get("sortBy") != null) sortBy = map.get("sortBy").toString();
            if (map.get("sortOrder") != null) sortOrder = map.get("sortOrder").toString();
        } else {
            status = (String) data;
        }

        return new Response(true, "Loc thanh cong!",
                taskDAO.filterTasksByStatus(status, userScope, sortBy, sortOrder));
    }

    private Response handleGetOverdueTasks(Request request) {
        UserDTO user = currentUser(request);
        Integer userScope = AuthorizationHelper.isAdmin(user) ? null : user.getId();
        return new Response(true, "Lay danh sach cong viec tre han!", taskDAO.getOverdueTasks(userScope));
    }

    private Response handleGetDashboardStats(Request request) {
        UserDTO user = currentUser(request);
        Map<String, Object> stats = new HashMap<>();
        List<TaskDTO> tasks;
        if (AuthorizationHelper.isAdmin(user)) {
            tasks = taskDAO.getAllTasks("id", "ASC");
            stats.put("totalGroups", groupDAO.getAllGroups().size());
        } else {
            tasks = taskDAO.getTasksByUserId(user.getId(), "id", "ASC");
            stats.put("totalGroups", 0);
        }

        long totalTasks = tasks.size();
        long doneTasks = tasks.stream().filter(t -> "DONE".equals(t.getStatus())).count();
        long overdueTasks = taskDAO.getOverdueTasks(
                AuthorizationHelper.isAdmin(user) ? null : user.getId()).size();

        stats.put("totalTasks", totalTasks);
        stats.put("doneTasks", doneTasks);
        stats.put("overdueTasks", overdueTasks);

        return new Response(true, "Lay thong ke thanh cong!", stats);
    }

    private Response handleGetSystemLogs() {
        return new Response(true, "Lay logs thanh cong!", systemLogDAO.getAllLogs());
    }

    private Response handleImportTasks(Object data, Request request) {
        UserDTO user = currentUser(request);
        Type listType = new TypeToken<List<TaskDTO>>() {}.getType();
        List<TaskDTO> tasks = gson.fromJson(gson.toJson(data), listType);

        if (tasks == null || tasks.isEmpty()) {
            return new Response(false, "File import khong co du lieu hop le!", null);
        }

        for (TaskDTO task : tasks) {
            String validationError = ValidationUtil.validateTask(task);
            if (validationError != null) {
                return new Response(false, "Loi du lieu import: " + validationError + " - Khong co du lieu nao duoc luu.", null);
            }
            if (task.getCreatorId() <= 0) {
                task.setCreatorId(user.getId());
            }
        }

        try {
            int imported = taskDAO.importTasks(tasks, user.getId());
            Logger.log(user.getUsername(), "IMPORT_TASKS", "SUCCESS");
            Map<String, Object> result = new HashMap<>();
            result.put("imported", imported);
            return new Response(true, "Import thanh cong " + imported + " cong viec vao database!", result);
        } catch (SQLException e) {
            Logger.log(user.getUsername(), "IMPORT_TASKS", "FAILED");
            return new Response(false, "Import that bai! Du lieu cu khong bi thay doi. Loi: " + e.getMessage(), null);
        }
    }
}