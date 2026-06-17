package server.util;

import common.model.Request;
import common.model.Response;
import common.model.UserDTO;

import java.util.Set;

public class AuthorizationHelper {
    private static final Set<String> PUBLIC_ACTIONS = Set.of("LOGIN");

    private static final Set<String> ADMIN_ACTIONS = Set.of(
            "GET_ALL_USERS", "ADD_USER", "UPDATE_USER", "TOGGLE_USER_STATUS", "DELETE_USER",
            "GET_ALL_GROUPS", "ADD_GROUP", "UPDATE_GROUP", "DELETE_GROUP",
            "GET_GROUP_MEMBERS", "ADD_GROUP_MEMBER", "REMOVE_GROUP_MEMBER",
            "GET_ALL_TASKS", "DELETE_TASK", "GET_OVERDUE_TASKS",
            "GET_SYSTEM_LOGS", "IMPORT_TASKS"
    );

    public static Response check(Request request) {
        String action = request.getAction();
        if (PUBLIC_ACTIONS.contains(action)) {
            return null;
        }

        UserDTO user = SessionManager.getUser(request.getToken());
        if (user == null) {
            return new Response(false, "Chua dang nhap hoac phien lam viec het han!", null);
        }

        if (ADMIN_ACTIONS.contains(action) && !"Admin".equals(user.getRole())) {
            return new Response(false, "Ban khong co quyen thuc hien thao tac nay!", null);
        }

        return null;
    }

    public static boolean isAdmin(UserDTO user) {
        return user != null && "Admin".equals(user.getRole());
    }
}