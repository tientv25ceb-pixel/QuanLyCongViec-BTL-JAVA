package server.util;

import common.model.UserDTO;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private static final Map<String, UserDTO> sessions = new ConcurrentHashMap<>();

    public static String createSession(UserDTO user) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, user);
        return token;
    }

    public static UserDTO getUser(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        return sessions.get(token);
    }

    public static void removeSession(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }
}