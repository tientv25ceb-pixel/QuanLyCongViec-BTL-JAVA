package server.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static final String LOG_FILE = "server.log";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // ThreadLocal de luu tru requestId cua luong hien tai
    private static final ThreadLocal<String> threadRequestId = new ThreadLocal<>();

    public static void setRequestId(String requestId) {
        threadRequestId.set(requestId);
    }

    public static String getRequestId() {
        return threadRequestId.get();
    }

    public static void clearRequestId() {
        threadRequestId.remove();
    }

    public static void log(String username, String action, String status) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String reqId = getRequestId();
        String reqIdLabel = (reqId != null && !reqId.isEmpty()) ? " [ReqID: " + reqId + "]" : "";
        
        String logMessage = String.format("%s%s | %s | %s | %s", timestamp, reqIdLabel, username, action, status);

        // Ghi ra console
        System.out.println(logMessage);

        // Ghi ra file
        try (PrintWriter out = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            out.println(logMessage);
        } catch (IOException e) {
            System.err.println("Loi khi ghi log file: " + e.getMessage());
        }

        // Ghi vao database (bang system_logs)
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO system_logs (username, action, status, request_id) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, username);
            ps.setString(2, action);
            ps.setString(3, status);
            ps.setString(4, reqId);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("Loi khi ghi log vao database: " + e.getMessage());
        }
    }
}
