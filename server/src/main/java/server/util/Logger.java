package server.util;

import java.io.BufferedWriter;
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

    // PrintWriter giu file mo lien tuc va dung BufferedWriter de dem dữ liệu ghi xuong dia
    private static PrintWriter fileWriter;

    static {
        try {
            // autoFlush = true để đẩy dữ liệu xuống file ngay khi println
            fileWriter = new PrintWriter(new BufferedWriter(new FileWriter(LOG_FILE, true)), true);
        } catch (IOException e) {
            System.err.println("Loi khi khoi tao file log: " + e.getMessage());
        }
    }

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

        // Ghi ra file (Dong bo hoa tranh xung dot giua cac thread client)
        if (fileWriter != null) {
            synchronized (Logger.class) {
                fileWriter.println(logMessage);
            }
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
