package server.main;

import server.handler.ClientHandler;
import server.service.TaskService;
import server.util.PasswordUtil;

import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {
    private static final String DEFAULT_PASSWORD = "123456";
    private static ExecutorService threadPool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        System.out.println("=== TASK MANAGEMENT SERVER ===");
        System.out.println("Dang khoi dong server...");

        // Doc port tu config.properties
        int port = 5555;
        try (InputStream input = ServerMain.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                Properties prop = new Properties();
                prop.load(input);
                String portStr = prop.getProperty("server.port");
                if (portStr != null && !portStr.isEmpty()) {
                    port = Integer.parseInt(portStr);
                }
            }
        } catch (Exception e) {
            System.err.println("[CONFIG] Loi doc config.properties, su dung port mac dinh: " + port);
        }

        // Kiem tra ket noi DB va reset mat khau mac dinh
        try (Connection conn = server.util.DBUtil.getConnection()) {
            System.out.println("[DB] Ket noi den database thanh cong!");
            upgradeDatabaseSchema(conn);
            resetDefaultPasswords(conn);
            printUserList(conn);
        } catch (Exception e) {
            System.err.println("[DB] LOI KET NOI DATABASE: " + e.getMessage());
            System.err.println("[DB] Hay kiem tra MySQL dang chay va thong tin ket noi trong config.properties");
        }

        // Khoi dong UDP Server trong thread rieng
        UDPServer udpServer = new UDPServer();
        Thread udpThread = new Thread(udpServer, "UDPServer-Thread");
        udpThread.setDaemon(true);
        udpThread.start();

        TaskService taskService = new TaskService();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server dang lang nghe tren port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client ket noi: " + clientSocket.getInetAddress());
                threadPool.execute(new ClientHandler(clientSocket, taskService));
            }
        } catch (Exception e) {
            System.err.println("Loi server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void upgradeDatabaseSchema(Connection conn) {
        System.out.println("[DB] Kiem tra va nang cap cau truc database...");
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getColumns(null, null, "system_logs", "request_id")) {
                if (!rs.next()) {
                    System.out.println("[DB] Cong them cot 'request_id' vao bang 'system_logs'...");
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate("ALTER TABLE system_logs ADD COLUMN request_id VARCHAR(50) DEFAULT NULL");
                        System.out.println("[DB]   -> Nang cap thanh cong!");
                    }
                } else {
                    System.out.println("[DB] Cot 'request_id' da ton tai.");
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB] Loi khi nang cap database: " + e.getMessage());
        }
    }

    private static void resetDefaultPasswords(Connection conn) {
        String[] usernames = {"admin", "user1", "user2"};
        System.out.println("[DB] Dang reset mat khau ve '" + DEFAULT_PASSWORD + "' cho tat ca tai khoan bang PBKDF2...");

        String sql = "UPDATE users SET password_hash = ?, status = 'ACTIVE' WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String username : usernames) {
                String newHash = PasswordUtil.hashPassword(DEFAULT_PASSWORD);
                ps.setString(1, newHash);
                ps.setString(2, username);
                int updated = ps.executeUpdate();
                System.out.println("[DB]   -> " + username + " (hash: " + newHash + "): " + (updated > 0 ? "OK" : "KHONG TIM THAY"));
            }
        } catch (SQLException e) {
            System.err.println("[DB] Loi khi reset mat khau: " + e.getMessage());
            return;
        }

        // Verify lai mat khau vua reset
        String verifySql = "SELECT username, password_hash FROM users WHERE username IN ('admin', 'user1', 'user2')";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(verifySql)) {
            System.out.println("[DB] Xac minh mat khau sau khi reset:");
            while (rs.next()) {
                String username = rs.getString("username");
                String hash = rs.getString("password_hash");
                boolean valid = PasswordUtil.checkPassword(DEFAULT_PASSWORD, hash);
                System.out.println("[DB]   -> " + username + ": verify('" + DEFAULT_PASSWORD + "') = " + valid);
            }
        } catch (SQLException e) {
            System.err.println("[DB] Loi khi xac minh: " + e.getMessage());
        }
    }

    private static void printUserList(Connection conn) {
        String sql = "SELECT id, username, role, status FROM users";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("[DB] Danh sach tai khoan:");
            while (rs.next()) {
                System.out.println("[DB]   - ID: " + rs.getInt("id")
                        + ", Username: '" + rs.getString("username") + "'"
                        + ", Role: " + rs.getString("role")
                        + ", Status: " + rs.getString("status"));
            }
        } catch (SQLException e) {
            System.err.println("[DB] Loi doc bang users: " + e.getMessage());
        }
    }
}
