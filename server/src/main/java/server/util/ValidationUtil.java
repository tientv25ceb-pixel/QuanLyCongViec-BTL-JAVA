package server.util;

import common.model.TaskDTO;
import common.model.UserDTO;

import java.util.Set;

public class ValidationUtil {

    public static String validateUser(UserDTO user, boolean isNew) {
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            return "Ten dang nhap khong duoc de trong!";
        }
        if (user.getUsername().length() < 3) {
            return "Ten dang nhap phai co it nhat 3 ky tu!";
        }
        if (isNew && (user.getPassword() == null || user.getPassword().isEmpty())) {
            return "Mat khau khong duoc de trong!";
        }
        if (user.getPassword() != null && !user.getPassword().isEmpty() && user.getPassword().length() < 6) {
            return "Mat khau phai co it nhat 6 ky tu!";
        }
        if (user.getPhone() != null && !user.getPhone().isEmpty() && !user.getPhone().matches("^0[0-9]{9,10}$")) {
            return "So dien thoai khong hop le! Dinh dang: 0xxxxxxxxx";
        }
        if (user.getRole() == null || (!user.getRole().equals("Admin") && !user.getRole().equals("User"))) {
            return "Vai tro khong hop le!";
        }
        return null;
    }

    public static String validateTask(TaskDTO task) {
        if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
            return "Tieu de cong viec khong duoc de trong!";
        }
        if (task.getDueDate() == null || task.getDueDate().trim().isEmpty()) {
            return "Han chot khong duoc de trong!";
        }
        if (!task.getDueDate().matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            return "Han chot phai co dinh dang yyyy-MM-dd!";
        }
        if (task.getStatus() == null || !Set.of("TODO", "IN_PROGRESS", "DONE").contains(task.getStatus())) {
            return "Trang thai cong viec khong hop le!";
        }
        if (task.getPriority() == null || !Set.of("LOW", "MEDIUM", "HIGH").contains(task.getPriority())) {
            return "Do uu tien khong hop le!";
        }
        return null;
    }
}