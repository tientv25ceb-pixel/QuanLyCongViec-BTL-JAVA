package server.dao;

import common.model.UserDTO;
import server.util.DBUtil;
import server.util.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public UserDTO login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND status = 'ACTIVE'";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String hash = rs.getString("password_hash");
                if (PasswordUtil.checkPassword(password, hash)) {
                    UserDTO user = new UserDTO();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setRole(rs.getString("role"));
                    user.setFullName(rs.getString("full_name"));
                    user.setStatus(rs.getString("status"));
                    // Giai ma so dien thoai neu co
                    String encPhone = rs.getString("encrypted_phone");
                    if (encPhone != null && !encPhone.isEmpty()) {
                        user.setPhone(PasswordUtil.decryptAES(encPhone));
                    }
                    return user;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<UserDTO> getAllUsers() {
        List<UserDTO> list = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                UserDTO user = new UserDTO();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setRole(rs.getString("role"));
                user.setFullName(rs.getString("full_name"));
                user.setStatus(rs.getString("status"));
                String encPhone = rs.getString("encrypted_phone");
                if (encPhone != null && !encPhone.isEmpty()) {
                    user.setPhone(PasswordUtil.decryptAES(encPhone));
                }
                list.add(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean addUser(UserDTO user) {
        String sql = "INSERT INTO users (username, password_hash, role, full_name, encrypted_phone) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, PasswordUtil.hashPassword(user.getPassword()));
            stmt.setString(3, user.getRole());
            stmt.setString(4, user.getFullName());
            // Ma hoa so dien thoai bang AES truoc khi luu
            if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                stmt.setString(5, PasswordUtil.encryptAES(user.getPhone()));
            } else {
                stmt.setNull(5, Types.VARCHAR);
            }
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateUser(UserDTO user) {
        String sql = "UPDATE users SET username = ?, role = ?, full_name = ?, encrypted_phone = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getRole());
            stmt.setString(3, user.getFullName());
            if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                stmt.setString(4, PasswordUtil.encryptAES(user.getPhone()));
            } else {
                stmt.setNull(4, Types.VARCHAR);
            }
            stmt.setInt(5, user.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateUserPassword(int userId, String newPassword) {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, PasswordUtil.hashPassword(newPassword));
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean toggleUserStatus(int userId) {
        String sql = "UPDATE users SET status = CASE WHEN status = 'ACTIVE' THEN 'DISABLED' ELSE 'ACTIVE' END WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
