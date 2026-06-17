package server.dao;

import common.model.SystemLogDTO;
import server.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SystemLogDAO {

    public List<SystemLogDTO> getAllLogs() {
        List<SystemLogDTO> list = new ArrayList<>();
        String sql = "SELECT * FROM system_logs ORDER BY timestamp DESC LIMIT 200";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                SystemLogDTO log = new SystemLogDTO();
                log.setId(rs.getInt("id"));
                log.setTimestamp(rs.getString("timestamp"));
                log.setUsername(rs.getString("username"));
                log.setAction(rs.getString("action"));
                log.setStatus(rs.getString("status"));
                list.add(log);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean addLog(String username, String action, String status) {
        String sql = "INSERT INTO system_logs (username, action, status) VALUES (?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, action);
            stmt.setString(3, status);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
