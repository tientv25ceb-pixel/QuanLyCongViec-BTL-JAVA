package server.dao;

import common.model.GroupDTO;
import common.model.GroupMemberDTO;
import server.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GroupDAO {

    public List<GroupDTO> getAllGroups() {
        List<GroupDTO> list = new ArrayList<>();
        String sql = "SELECT g.*, u.username as createdByName FROM `groups` g LEFT JOIN users u ON g.created_by = u.id";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                GroupDTO group = new GroupDTO();
                group.setId(rs.getInt("id"));
                group.setGroupName(rs.getString("group_name"));
                group.setDescription(rs.getString("description"));
                group.setCreatedBy(rs.getInt("created_by"));
                group.setCreatedByName(rs.getString("createdByName"));
                list.add(group);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean addGroup(GroupDTO group) {
        String sql = "INSERT INTO `groups` (group_name, description, created_by) VALUES (?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, group.getGroupName());
            stmt.setString(2, group.getDescription());
            stmt.setInt(3, group.getCreatedBy());
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        group.setId(keys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateGroup(GroupDTO group) {
        String sql = "UPDATE `groups` SET group_name = ?, description = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, group.getGroupName());
            stmt.setString(2, group.getDescription());
            stmt.setInt(3, group.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteGroup(int groupId) {
        String sql = "DELETE FROM `groups` WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, groupId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ==================== Group Members ====================

    public List<GroupMemberDTO> getGroupMembers(int groupId) {
        List<GroupMemberDTO> list = new ArrayList<>();
        String sql = "SELECT gm.*, u.username, u.full_name FROM group_members gm " +
                     "JOIN users u ON gm.user_id = u.id WHERE gm.group_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, groupId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                GroupMemberDTO member = new GroupMemberDTO();
                member.setGroupId(rs.getInt("group_id"));
                member.setUserId(rs.getInt("user_id"));
                member.setRoleInGroup(rs.getString("role_in_group"));
                member.setUsername(rs.getString("username"));
                member.setFullName(rs.getString("full_name"));
                list.add(member);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean addGroupMember(int groupId, int userId, String roleInGroup) {
        String sql = "INSERT INTO group_members (group_id, user_id, role_in_group) VALUES (?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, groupId);
            stmt.setInt(2, userId);
            stmt.setString(3, roleInGroup != null ? roleInGroup : "MEMBER");
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeGroupMember(int groupId, int userId) {
        String sql = "DELETE FROM group_members WHERE group_id = ? AND user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, groupId);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
