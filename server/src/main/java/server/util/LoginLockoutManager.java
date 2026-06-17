package server.util;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LoginLockoutManager {
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 5;

    private static final Map<String, AttemptInfo> attempts = new ConcurrentHashMap<>();

    public static String checkLocked(String username) {
        AttemptInfo info = attempts.get(username);
        if (info == null || info.lockUntil == null) {
            return null;
        }
        if (LocalDateTime.now().isBefore(info.lockUntil)) {
            long minutesLeft = java.time.Duration.between(LocalDateTime.now(), info.lockUntil).toMinutes() + 1;
            return "Tai khoan tam khoa do dang nhap sai qua nhieu lan. Thu lai sau " + minutesLeft + " phut.";
        }
        info.failedCount = 0;
        info.lockUntil = null;
        return null;
    }

    public static void recordSuccess(String username) {
        attempts.remove(username);
    }

    public static String recordFailure(String username) {
        AttemptInfo info = attempts.computeIfAbsent(username, k -> new AttemptInfo());
        info.failedCount++;
        if (info.failedCount >= MAX_ATTEMPTS) {
            info.lockUntil = LocalDateTime.now().plusMinutes(LOCK_MINUTES);
            return "Dang nhap sai " + MAX_ATTEMPTS + " lan. Tai khoan bi khoa tam thoi trong " + LOCK_MINUTES + " phut.";
        }
        int remaining = MAX_ATTEMPTS - info.failedCount;
        return "Sai ten dang nhap hoac mat khau! Con lai " + remaining + " lan thu.";
    }

    private static class AttemptInfo {
        int failedCount;
        LocalDateTime lockUntil;
    }
}