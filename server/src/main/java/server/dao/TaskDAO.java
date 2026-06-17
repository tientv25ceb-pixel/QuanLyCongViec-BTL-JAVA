package server.dao;

import common.model.TaskDTO;
import server.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TaskDAO {

    private static final String BASE_SELECT =
            "SELECT t.*, g.group_name as groupName, " +
            "u1.username as creatorName, u2.username as assigneeName " +
            "FROM tasks t " +
            "LEFT JOIN `groups` g ON t.group_id = g.id " +
            "LEFT JOIN users u1 ON t.creator_id = u1.id " +
            "LEFT JOIN users u2 ON t.assignee_id = u2.id ";

    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "title", "t.title",
            "due_date", "t.due_date",
            "status", "t.status",
            "priority", "t.priority",
            "id", "t.id"
    );

    public List<TaskDTO> getAllTasks(String sortBy, String sortOrder) {
        return queryTasks(BASE_SELECT, null, null, sortBy, sortOrder);
    }

    public List<TaskDTO> getTasksByUserId(int userId, String sortBy, String sortOrder) {
        return queryTasks(BASE_SELECT + "WHERE t.creator_id = ? OR t.assignee_id = ? ",
                new Object[]{userId, userId}, null, sortBy, sortOrder);
    }

    public List<TaskDTO> searchTasksByTitle(String keyword, Integer userId, String sortBy, String sortOrder) {
        String sql = BASE_SELECT + "WHERE t.title LIKE ? ";
        if (userId != null) {
            sql += "AND (t.creator_id = ? OR t.assignee_id = ?) ";
            return queryTasks(sql, new Object[]{"%" + keyword + "%", userId, userId}, null, sortBy, sortOrder);
        }
        return queryTasks(sql, new Object[]{"%" + keyword + "%"}, null, sortBy, sortOrder);
    }

    public List<TaskDTO> filterTasksByStatus(String status, Integer userId, String sortBy, String sortOrder) {
        String sql = BASE_SELECT + "WHERE t.status = ? ";
        if (userId != null) {
            sql += "AND (t.creator_id = ? OR t.assignee_id = ?) ";
            return queryTasks(sql, new Object[]{status, userId, userId}, null, sortBy, sortOrder);
        }
        return queryTasks(sql, new Object[]{status}, null, sortBy, sortOrder);
    }

    public List<TaskDTO> getOverdueTasks(Integer userId) {
        String sql = BASE_SELECT + "WHERE t.due_date < CURDATE() AND t.status != 'DONE' ";
        if (userId != null) {
            sql += "AND (t.creator_id = ? OR t.assignee_id = ?) ";
            return queryTasks(sql, new Object[]{userId, userId}, "due_date", "due_date", "ASC");
        }
        return queryTasks(sql, null, null, "due_date", "ASC");
    }

    public boolean canUserModifyTask(int taskId, int userId) {
        String sql = "SELECT COUNT(*) FROM tasks WHERE id = ? AND (creator_id = ? OR assignee_id = ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, taskId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int importTasks(List<TaskDTO> tasks, int defaultCreatorId) throws SQLException {
        String sql = "INSERT INTO tasks (title, description, status, priority, due_date, creator_id, group_id, assignee_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);
            int imported = 0;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (TaskDTO task : tasks) {
                    stmt.setString(1, task.getTitle());
                    stmt.setString(2, task.getDescription());
                    stmt.setString(3, task.getStatus() != null ? task.getStatus() : "TODO");
                    stmt.setString(4, task.getPriority() != null ? task.getPriority() : "MEDIUM");
                    stmt.setString(5, task.getDueDate());
                    stmt.setInt(6, task.getCreatorId() > 0 ? task.getCreatorId() : defaultCreatorId);
                    if (task.getGroupId() > 0) {
                        stmt.setInt(7, task.getGroupId());
                    } else {
                        stmt.setNull(7, Types.INTEGER);
                    }
                    if (task.getAssigneeId() > 0) {
                        stmt.setInt(8, task.getAssigneeId());
                    } else {
                        stmt.setNull(8, Types.INTEGER);
                    }
                    stmt.addBatch();
                    imported++;
                }
                stmt.executeBatch();
            }
            conn.commit();
            return imported;
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    public boolean addTask(TaskDTO task) {
        String sql = "INSERT INTO tasks (title, description, status, priority, due_date, creator_id, group_id, assignee_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bindTaskParams(stmt, task);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        task.setId(generatedKeys.getInt(1));
                        return true;
                    }
                }
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateTask(TaskDTO task) {
        String sql = "UPDATE tasks SET title = ?, description = ?, status = ?, priority = ?, due_date = ?, group_id = ?, assignee_id = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, task.getTitle());
            stmt.setString(2, task.getDescription());
            stmt.setString(3, task.getStatus());
            stmt.setString(4, task.getPriority());
            stmt.setString(5, task.getDueDate());
            setNullableInt(stmt, 6, task.getGroupId());
            setNullableInt(stmt, 7, task.getAssigneeId());
            stmt.setInt(8, task.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateTaskStatus(int taskId, String status) {
        String sql = "UPDATE tasks SET status = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            stmt.setInt(2, taskId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteTask(int taskId) {
        String sql = "DELETE FROM tasks WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, taskId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private List<TaskDTO> queryTasks(String sql, Object[] params, String defaultSortBy, String sortBy, String sortOrder) {
        List<TaskDTO> list = new ArrayList<>();
        String orderClause = buildOrderClause(
                sortBy != null ? sortBy : defaultSortBy,
                sortOrder != null ? sortOrder : "ASC"
        );
        String finalSql = sql + orderClause;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(finalSql)) {

            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    Object param = params[i];
                    if (param instanceof Integer) {
                        stmt.setInt(i + 1, (Integer) param);
                    } else {
                        stmt.setString(i + 1, param.toString());
                    }
                }
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToTask(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private String buildOrderClause(String sortBy, String sortOrder) {
        String column = SORT_COLUMNS.getOrDefault(
                sortBy != null ? sortBy.toLowerCase() : "id", "t.id");
        String order = "DESC".equalsIgnoreCase(sortOrder) ? "DESC" : "ASC";
        return "ORDER BY " + column + " " + order;
    }

    private void bindTaskParams(PreparedStatement stmt, TaskDTO task) throws SQLException {
        stmt.setString(1, task.getTitle());
        stmt.setString(2, task.getDescription());
        stmt.setString(3, task.getStatus());
        stmt.setString(4, task.getPriority());
        stmt.setString(5, task.getDueDate());
        stmt.setInt(6, task.getCreatorId());
        setNullableInt(stmt, 7, task.getGroupId());
        setNullableInt(stmt, 8, task.getAssigneeId());
    }

    private void setNullableInt(PreparedStatement stmt, int index, int value) throws SQLException {
        if (value > 0) {
            stmt.setInt(index, value);
        } else {
            stmt.setNull(index, Types.INTEGER);
        }
    }

    private TaskDTO mapResultSetToTask(ResultSet rs) throws SQLException {
        TaskDTO task = new TaskDTO();
        task.setId(rs.getInt("id"));
        task.setTitle(rs.getString("title"));
        task.setDescription(rs.getString("description"));
        task.setStatus(rs.getString("status"));
        task.setPriority(rs.getString("priority"));
        task.setDueDate(rs.getString("due_date"));
        task.setCreatorId(rs.getInt("creator_id"));
        task.setCreatorName(rs.getString("creatorName"));
        task.setGroupId(rs.getInt("group_id"));
        task.setGroupName(rs.getString("groupName"));
        task.setAssigneeId(rs.getInt("assignee_id"));
        task.setAssigneeName(rs.getString("assigneeName"));
        return task;
    }
}